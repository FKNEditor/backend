package au.com.expressionless.nish.endpoints;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import au.com.expressionless.nish.models.entity.edition.Edition;
import au.com.expressionless.nish.models.entity.edition.Keyword;
import au.com.expressionless.nish.models.entity.edition.story.Story;
import au.com.expressionless.nish.service.MinIO;
import au.com.expressionless.nish.utils.GeneralUtils;
import au.com.expressionless.nish.utils.pdf.PDFWrapper;
import au.com.expressionless.nish.utils.python.PyScriptResult;
import au.com.expressionless.nish.utils.python.PyScriptRunner;
import io.minio.ObjectWriteResponse;

@Path("v2/edition")
@ApplicationScoped
public class EditionResource extends SecureResource {

    @Inject
    MinIO minio;

    static final Logger log = Logger.getLogger(EditionResource.class);

    /**
     * Retrieves a list of editions.
     * @param searchText Text search editions on.
     * @param published Whether or not the edition should be published or not
     * @return JsonArray of editions. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEditions(
        @QueryParam("search_text") String searchText, 
        @QueryParam("published") Boolean published) {

            // Set defaults for query params
            searchText = StringUtils.isNotBlank(searchText) ? searchText : null;

            // find all editions
            // TODO(Vesta): Adjust for multiple search terms. This is doable 
            Set<Edition> editions = Keyword.listEditionsByKeywordSearch(searchText, published, 10);

            JsonArrayBuilder editionsArrayBuilder = Json.createArrayBuilder();
            for (Edition edition : editions) {

                editionsArrayBuilder.add(
                    Json.createObjectBuilder()
                    .add("id",          edition.id)
                    .add("fileName",    edition.getFileName())
                    .add("author",      edition.getAuthor())
                    .add("minioId",     edition.getMinioId())
                    .add("published",   edition.isPublished())
                    .build()
                );
            }

            // send list of editions
            return Response.ok(
                Json.createObjectBuilder()
                .add(
                    "editions", editionsArrayBuilder.build()
                )
                .build()
            ).build();
    }

    /**
     * Deletes a particular edition
     * @param editionId Id of the edition to delete.
     * @return Whether or not the edition was deleted successfully. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @DELETE
    @Path("/{edition-id}")
    @Transactional
    public Response deleteEdition(@PathParam("edition-id") Long editionId) {
        mustBeAuthenticated();

        // find edition in db
        Edition edition = Edition.findById(editionId);
        if (edition == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Edition with id = "  + editionId + " does not exist"
                )
                .build()
            ).build();
        }
        deleteEdition(edition);
        
        return Response.status(Status.NO_CONTENT).build();
    }
 
    /**
     * Converts a published edition back to draft.
     * @param editionId Id of the edition to redraft.
     * @return Whether or not the edition was drafted successfully. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information
     * * */
    @PUT
    @Path("draft/{edition-id}")
    @Transactional
    public Response draftEdition(@PathParam("edition-id") Long editionId) {
        mustBeAuthenticated();

        // find edition in db
        Edition edition = Edition.findById(editionId);
        if (edition == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Edition with id = "  + editionId + " does not exist"
                )
                .build()
            ).build();
        }

        // Draft editions can't be re-drafted
        if (!edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Edition `" + edition.getFileName() + "` is already a draft"
                )
                .build()
            ).build();
        }

        // draft edition
        edition.setToDraft();

        // generate new keywords
        try {
            genKeywords(edition);
        } catch(IOException | InterruptedException e) {
            log.error("Keywords failed to generate!");
            log.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }  

        // persist the edition once keywords are generated
        edition.persist();
        return Response.ok().build();
    }

    /**
     * Converts a drafted edition back to published.
     * @param editionId Id of the edition to published.
     * @return Whether or not the edition was drafted successfully. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information
     * * */
    @PUT
    @Path("publish/{edition-id}")
    @Transactional
    public Response publishEdition(@PathParam("edition-id") Long editionId) {
        mustBeAuthenticated();

        // find edition in db
        Edition edition = Edition.findById(editionId);
        if (edition == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Edition with id = "  + editionId + " does not exist"
                )
                .build()
            ).build();
        }

        // Published editions can't be re-published
        if (edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Edition `" + edition.getFileName() + "` is already published"
                )
                .build()
            ).build();
        }

        // publish the edition
        edition.setToPublished();

        // generate new keywords   
        try {
            genKeywords(edition);
        } catch(IOException | InterruptedException e) {
            log.error("Keywords failed to generate!");
            log.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } 

        // persist the edition once keywords are generated
        edition.persist();
        return Response.ok().build(); 
    }

    /**
     * Uploads a new edition to the server.
     * @param input Multipart form data containing uploaded pdf file (in the ideal case)
     * @return Information about the new edition added. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information
     * * */
    @POST
    @Path("/upload")
    @Consumes("multipart/form-data")
    @Transactional
    public Response uploadEdition(MultipartFormDataInput input) {
        mustBeAuthenticated();

        // download file from request
        log.info("Attempting upload");
        List<InputPart> inputParts = extractInputParts(input);

        if (inputParts.size() < 1) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "No file was sent"
                )
                .build()
            ).build();
        }

        // try to upload PDF
        String minioId;
        String fileName;
        String author;
        try { 
        
            // Currently only uploading one file at a time
            InputPart inputPart = inputParts.get(0);

            // extract name from header
            MultivaluedMap<String, String> header = inputPart.getHeaders();
            minioId = UUID.randomUUID().toString();
            fileName = getFileName(header);
            author = "NO_AUTHOR"; // Placeholder

            // convert the uploaded file to inputstream
            InputStream inputStream = inputPart.getBody(InputStream.class, null);
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // verify that the byte stream is a PDF file
            PDFWrapper pdfWrapper = new PDFWrapper(bytes);

            // constructs upload file path using the minioId
            BufferedImage bufferedImage = pdfWrapper.generateThumbnail();
            File tbnFile = new File(minioId + "-thumbnail");
            ImageIO.write(bufferedImage, "jpeg", tbnFile);
            File pdfFile = GeneralUtils.writeFile(bytes, minioId + "-pdf");

            // upload both files to minIO
            log.info("Uploading files for `" + fileName + "`...");
            log.info("Uploading: " + pdfFile.getName());
            ObjectWriteResponse resp1 = minio.uploadPdf(pdfFile, minioId);
            log.info("Uploading: " + tbnFile.getName());
            ObjectWriteResponse resp2 = minio.uploadThumbnail(tbnFile, minioId);
            log.info(
                "Response: " + resp1.etag() + " : " + resp1.bucket() + " : " + resp1.object()
            );
            log.info(
                "Response: " + resp2.etag() + " : " + resp2.bucket() + " : " + resp2.object()
            );
            pdfFile.delete();
            tbnFile.delete(); 
        } 
        catch (IOException e) {
            log.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Something happened while processing the PDF"
                ).build()
            ).build();
        }
 
        // create and persist new edition
        Edition edition = new Edition();
        edition.setFileName(fileName);
        edition.setMinioId(minioId);
        edition.setAuthor(author);
        edition.setToDraft();
        edition.persist();

        // generate keywords for new edition
        try {
            genKeywords(edition);
        } catch(IOException | InterruptedException e) {
            log.error(e.getMessage());
            log.error("Keywords failed to generate!");
        }

        // ======== BUILD SUCCESS RESPONSE ========

        return Response.status(Status.CREATED).entity(
            Json.createObjectBuilder()
            .add("id",          edition.id)
            .add("fileName",    edition.getFileName())
            .add("minioId",     edition.getMinioId())
            .add("author",      edition.getAuthor())
            .add("published",   edition.isPublished())
            .build()
        ).build(); 
    }

    private List<InputPart> extractInputParts(MultipartFormDataInput input) {

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");
        log.info("Num Parts: " + inputParts.size());
 
        return inputParts;
    }

    // Code duplication but idk where to put it tbh
    // only because MinioResource still contains some old endpoints or smth :rolling_eyes:
    private String getFileName(MultivaluedMap<String, String> header) throws BadRequestException {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                return FilenameUtils.removeExtension(finalFileName);
            }
        }
        
        throw new BadRequestException("File did not have a file name");
    }

    /**
     * Procedure for deleting an edition including all relevant linked data
     * */
    private void deleteEdition(Edition edition) {

        // delete the minio files for each story
        List<Story> stories = Story.findByEdition(edition.id);
        for (Story story : stories) {
            log.info(
                "Deleting Files for story `" + story.getTitle() + "`..."
            );
            log.info("Deleting Delta " + story.getMinioId());
            minio.deleteHtml(story.getMinioId());
            minio.deleteDelta(story.getMinioId()); 
        }
        log.info(
            "Deleting Files for edition `" + edition.getFileName() + "`..."
        );

        // Delete any keywords from db
        Keyword.deleteByEdition(edition.id);

        // delete minio files for edition
        log.info("Deleting PDF " + edition.getMinioId());
        minio.deletePdf(edition.getMinioId()); 
        log.info("Deleting Thumbnail " + edition.getMinioId());
        minio.deleteThumbnail(edition.getMinioId());
        edition.delete();
    }

    /**
     * Generates Keywords for an edition, changing if the edition is published or in draft
     * */
    private void genKeywords(Edition edition) 
        throws IOException, InterruptedException, InternalServerErrorException {

            log.info("Generating Keywords...");
            if (edition == null)
                return;
 
            // construct stories array
            JsonArrayBuilder storyArrayBuilder = Json.createArrayBuilder();

            // Will only generate keywords for stories if the edition is published
            if (edition.isPublished()) {
                List<Story> editionStories = edition.getStories();
                for (Story story : editionStories) {
                    storyArrayBuilder.add(
                        Json.createObjectBuilder()
                        .add("title",   story.getTitle())
                        .add("author",  story.getAuthor())
                        .add("text",    story.getText())
                        .build()
                    );
                }
            }

            // build json object
            JsonArray stories = storyArrayBuilder.build();
            JsonObject input = Json.createObjectBuilder()
            .add("id",      edition.id)
            .add("title",   edition.getFileName())
            .add("author",  edition.getAuthor())
            .add("stories", stories)
            .build();

            PyScriptResult result = new PyScriptRunner("txt_tag/text_cleaner.py")
            .setTimeout(10)
            .setStdinData(GeneralUtils.JsonSerialise(input))
            .run();

            // script failed to run
            if (result == null)
                throw new InternalServerErrorException("Text tagging script timed out");

            // script ran but encountered an error
            if (result.getExitValue() != 0) {
                log.error(new String(result.getStderrData()));
                throw new InternalServerErrorException("Error occured during text tagging");
            }

            log.info("Keywords generated for edition: " + edition.getFileName());
            log.info(result.toString());

            // Since the keywords are generated we should delete the old keywords
            Keyword.deleteByEdition(edition.id);

            // Loop over CSV rows here
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(result.getStdout())
            );
            while(reader.ready()) {
                String line = reader.readLine();
                String[] rowElems = line.split(",");
                String word     = rowElems[0];
                Double ratio    = Double.valueOf(rowElems[2]);

                // NOTE: THIS IS TEMPORARY, GONNA MAKE NISH GO AND EDIT THE SCRIPT
                if (ratio <= 0.3d)
                    continue; 

                log.info("Word: " + word);

                word = word.toLowerCase();
                Keyword keyword = Keyword.findByEditionAndWord(word, edition.id);
                if (keyword == null) {
                    keyword = new Keyword(word, edition, ratio);
                } else {
                    keyword.ratio += ratio;
                }
                keyword.persist();
            }
    }
}

