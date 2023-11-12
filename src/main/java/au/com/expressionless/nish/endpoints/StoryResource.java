package au.com.expressionless.nish.endpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.Json;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;

import au.com.expressionless.nish.models.entity.edition.Edition;
import au.com.expressionless.nish.models.entity.edition.story.Bounds;
import au.com.expressionless.nish.models.entity.edition.story.Story;
import au.com.expressionless.nish.models.entity.edition.story.StorySelection;
import au.com.expressionless.nish.service.MinIO;
import au.com.expressionless.nish.utils.ANSIColour;
import au.com.expressionless.nish.utils.GeneralUtils;
import au.com.expressionless.nish.utils.pdf.PDFWrapper;
import io.minio.GetObjectResponse;
import io.minio.errors.ErrorResponseException;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

@Path("v2/story")
@ApplicationScoped
public class StoryResource extends SecureResource {

    static Jsonb jsonb = JsonbBuilder.create();

    @Inject
    MinIO minio;

    static final Logger log = Logger.getLogger(StoryResource.class);

    // default json file for delta data
    static final JsonObject DEFAULT_DELTA_JSON = 
    Json.createObjectBuilder()
    .add(
        "delta", Json.createObjectBuilder().add(
            "ops", Json.createArrayBuilder().build()
        ).build()
    ).build();

    // default html file for html data
    static final JsonObject DEFAULT_HTML_JSON = 
    Json.createObjectBuilder()
    .add(
        "html", ""
    )
    .build();

    /**
     * Provides a list of stories that are associated with a particular edition.
     * @param editionId Id of the edition to fetch the stories from.
     * @return JsonArray consisting of a list of stories. See 
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @GET
    @Path("/list/{edition-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEditionStories(@PathParam("edition-id") long editionId) { 

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

        // parse stories and their selections into json format
        JsonArrayBuilder responseArrayBuilder = Json.createArrayBuilder();
        List<Story> editionStories = edition.getStories();
        for(Story story : editionStories) {

            // parse selections into a json array
            JsonArrayBuilder selectionsArrayBuilder = Json.createArrayBuilder();
            List<StorySelection> storySelections = story.getSelections();
            for (StorySelection selection : storySelections) {
                Bounds bounds = selection.getBounds();
                selectionsArrayBuilder.add(
                    Json.createObjectBuilder()
                    .add("id",              selection.id)
                    .add("x",               bounds.getX())
                    .add("y",               bounds.getY())
                    .add("width",           bounds.getWidth())
                    .add("height",          bounds.getHeight())
                    .add("pageNumber",      selection.getPageNum())
                    .add("sequenceNumber",  bounds.getSequenceNum())
                    .build()
                );
            }
            JsonArray selections = selectionsArrayBuilder.build();

            // add story into the response
            responseArrayBuilder.add(
                Json.createObjectBuilder()
                .add("id",         story.id)
                .add("title",      story.getTitle())
                .add("author",     story.getAuthor())
                .add("selections", selections)
                .build()
            );
        }

        // ======== BUILD SUCCESS RESPONSE ========

        JsonArray stories = responseArrayBuilder.build();
        return Response.ok(
            Json.createObjectBuilder()
            .add("stories", stories)
            .build()
        ).build();
    }

    /**
     * Adds a story to the edition, generating any text detected by the provided story selections .
     * @param editionId Id of the edition to add the story to.
     * @param body Json body consisting of story data and story selection data.
     * @return Json Body consisting of the new story's id, the id's of the new selections.
     * and the newly generated paragraphs. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @POST
    @Path("/add/{edition-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addStory(JsonObject body, @PathParam("edition-id") long editionId) {
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

        // should not be able to add a story if the edition is published
        if (edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Cannot add story as `" + edition.getFileName() + "` is published"
                )
                .build()
            ).build();
        }

        // json fields in request body
        String title;
        String author;
        JsonArray selections; 
        Map<Integer, List<Bounds> > pageBoundsMap;

        // fetch fields from request body
        try {
            title         = body.getString("title"); 
            author        = body.getString("author");
            selections    = body.getJsonArray("selections");
            pageBoundsMap = parseSelections(selections);
        } catch (NullPointerException | ClassCastException e) {
            log.error(e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Fields in request body not instantiated correctly"
                )
                .build()
            ).build();
        }

        // wrap edition pdf for utilities
        PDFWrapper pdfWrapper;
        try {

            // fetch pdf file from minIO 
            GetObjectResponse resp = minio.downloadPdf(edition.getMinioId());

            // pdf could not be fetched for some reason
            if (resp == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    Json.createObjectBuilder()
                    .add(
                        "error", "PDF for article `" + title + "` could not be fetched"
                    )
                    .build()
                ).build();
            }
            pdfWrapper = new PDFWrapper(resp.readAllBytes());

        // pdf for story does not exist   
        // TODO: Go over scenarios where ErrorResponseException is thrown
        } catch (ErrorResponseException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "PDF for article `" + title + "` does not exist"
                )
                .build()
            ).build();

        // io issues with loading pdf into memory
        } catch (IOException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "PDF for article `" + title + "` could not be loaded on server"
                )
                .build()
            ).build();
        } 

        // create placeholder delta json file for this story
        String minioId;
        try {
            minioId = UUID.randomUUID().toString();

            // generate empty json for delta
            File deltaFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(DEFAULT_DELTA_JSON),
                minioId + "-delta"
            );

            // generate empty json for html
            File htmlFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(DEFAULT_HTML_JSON),
                minioId + "-html"
            );

            minio.uploadDelta(deltaFile, minioId);
            minio.uploadHtml(htmlFile, minioId);

            deltaFile.delete();
            htmlFile.delete();
        } catch (IOException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Could not generate default files for " + title
                )
                .build()
            ).build();
        }

        // ======= PERSIST STORY ========

        // set default fields
        Story story = new Story();
        story.setTitle(title);
        story.setAuthor(author);
        story.setText("");
        story.setMinioId(minioId);
        List<StorySelection> newSelections = story.addSelections(pageBoundsMap);
        story.persist();

        // add story to edition
        edition.addStory(story);
        edition.persist();

        // ======== BUILD SUCCESS RESPONSE ========

        // generate list of selection ids in json format
        JsonArrayBuilder selectionIdsBuilder = Json.createArrayBuilder();
        for (StorySelection selection : newSelections){
            selectionIdsBuilder.add(selection.id);
        }

        // build response
        JsonArray selectionIds = selectionIdsBuilder.build();
        JsonArray paragraphs = generateTextBySequence(pdfWrapper, pageBoundsMap);
        return Response.ok(
            Json.createObjectBuilder()
            .add("id",         story.id)
            .add("selectionIds",    selectionIds)
            .add("paragraphs",      paragraphs)
            .build()
        ).build();
    }

    /**
     * Updates the selections of a particular story, regenerating any text in the new 
     * selections.
     * @param body JsonArray consisting of selections
     * @param storyId Id of the story to update the selections for 
     * @return The new id's of the updated selections, as well as any newly generated text. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @PUT
    @Path("/selections/update/{story-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response updateSelections(JsonObject body, @PathParam("story-id") Long storyId) {
        mustBeAuthenticated();

        // find story in db
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = "  + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        // should not be able to update selections for a story where the edition is published
        Edition edition = story.getEdition();
        if (edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Cannot update story as `" + edition.getFileName() + "` is published"
                )
                .build()
            ).build();
        }

        // fetch fields from request body
        JsonArray selections; 
        Map<Integer, List<Bounds> > pageBoundsMap;
        try {
            selections    = body.getJsonArray("selections");
            pageBoundsMap = parseSelections(selections);
        } catch (NullPointerException | ClassCastException e) {
            log.error(e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Fields in request body not instantiated correctly"
                )
                .build()
            ).build();
        }

        // create placeholder delta json file for this story
        try {

            // generate empty json for delta
            File deltaFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(DEFAULT_DELTA_JSON),
                story.getMinioId() + "-delta"
            );

            // generate empty json for html
            File htmlFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(DEFAULT_HTML_JSON),
                story.getMinioId() + "-html"
            );

            // upload files
            minio.uploadDelta(deltaFile, story.getMinioId());
            minio.uploadHtml(htmlFile, story.getMinioId());

            deltaFile.delete();
            htmlFile.delete();
        } catch (IOException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Could not generate default files for " + story.getTitle()
                )
                .build()
            ).build();
        }

        // wrap edition pdf for utilities
        PDFWrapper pdfWrapper;
        try {

            // fetch pdf file from minIO 
            GetObjectResponse resp = minio.downloadPdf(edition.getMinioId());

            // pdf could not be fetched for some reason
            if (resp == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    Json.createObjectBuilder()
                    .add(
                        "error", "PDF for article `" + story.getTitle() + "` could not be fetched"
                    )
                    .build()
                ).build();
            }
            pdfWrapper = new PDFWrapper(resp.readAllBytes());

        // pdf for story does not exist   
        // TODO: Go over scenarios where ErrorResponseException is thrown
        } catch (ErrorResponseException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "PDF for article `" + story.getTitle() + "` does not exist"
                )
                .build()
            ).build();

        // io issues with loading pdf into memory
        } catch (IOException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "PDF for article `" + story.getTitle() + "` could not be loaded on server"
                )
                .build()
            ).build();
        } 

        // Now that data relating to the selections is cleared, remove all the selections
        // and add the new selections
        story.setText("");
        story.deleteSelections();
        List<StorySelection> newSelections = story.addSelections(pageBoundsMap);
        story.persist();

        // generate list of selection ids in json format
        JsonArrayBuilder selectionIdsBuilder = Json.createArrayBuilder();
        for (StorySelection selection : newSelections){
            selectionIdsBuilder.add(selection.id);
        } 

        // build response
        JsonArray selectionIds = selectionIdsBuilder.build();
        JsonArray paragraphs = generateTextBySequence(pdfWrapper, pageBoundsMap);
        return Response.ok(
            Json.createObjectBuilder()
            .add("selectionIds",    selectionIds)
            .add("paragraphs",      paragraphs)
            .build()
        ).build();
    }

    /**
     * Deletes a particular story.
     * @param storyId Id of the story to delete 
     * @return Whether or not the story was deleted successfully. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @DELETE
    @Path("/{story-id}")
    @Transactional
    public Response deleteStory(@PathParam("story-id") Long storyId) {
        mustBeAuthenticated();

        // find story in db
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = "  + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        Edition edition = story.getEdition();
        if (edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Cannot delete story as `" + edition.getFileName() + "` is published"
                )
                .build()
            ).build();
        }

        // delete story and its minio data
        minio.deleteDelta(story.getMinioId());
        minio.deleteHtml(story.getMinioId());
        story.delete();

        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * Updates the content of a particular story.
     * @param storyId Id of the story to update the content for
     * @return Whether or not the story was updated successfully. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @PUT
    @Transactional
    @Path("/content/update/{story-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateContent(JsonObject body, @PathParam("story-id") long storyId) {
        mustBeAuthenticated();

        // find story in db 
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = " + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        Edition edition = story.getEdition();
        if (edition.isPublished()) {
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Cannot update story as `" + edition.getFileName() + "` is published"
                )
                .build()
            ).build();
        }

        // fetch fields from request body
        String text;
        String html;
        JsonObject delta;
        try {
            text  = body.getString("text");
            html  = body.getString("html");
            delta = body.getJsonObject("delta");
        } catch (NullPointerException e) {
            log.error("Error missing field: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Missing a field: either \"text\", \"delta\" or \"html\""
                )
                .build()
            ).build();
        } catch (ClassCastException e) {
            log.error("Error malformed field: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Error malformed field: " + e.getMessage()
                )
                .add("expected", Json.createObjectBuilder()
                    .add("text", "string")
                    .add("delta", "json object")
                    .build()
                )
                .build()
            ).build();
        }

        // write new delta json file to minio
        try {
            File deltaFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(
                    Json.createObjectBuilder()
                    .add("delta", delta)
                    .build()
                ), 
                story.getMinioId()
            );
            minio.uploadDelta(deltaFile, story.getMinioId());
            deltaFile.delete();
        } catch (IOException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Could not update delta for " + story.getTitle()
                )
                .build()
            ).build();
        }

        // write new html json file to minio
        try {
            File htmlFile = GeneralUtils.writeFile(
                GeneralUtils.JsonSerialise(
                    Json.createObjectBuilder()
                    .add("html", html)
                    .build()
                ),
                story.getMinioId()
            );
            minio.uploadHtml(htmlFile, story.getMinioId());
            htmlFile.delete();
        } catch (IOException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Could not update html for " + story.getTitle()
                )
                .build()
            ).build();
        }

        // set text
        // story.setText(getDeltaRawText(delta));
        story.setText(text);
        story.persist();

        return Response.ok().build();
    }

    /**
     * Fetches the delta of a particular story,
     * @param storyId Id of the story to fetch the delta from.
     * @return Delta of the story as a JsonObject. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @GET
    @Path("/content/fetch/delta/{story-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDelta(@PathParam("story-id") long storyId) {
        mustBeAuthenticated();

        // find story in db 
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = " + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        JsonObject deltaJson;
        try {
            // fetch delta json file from minIO 
            GetObjectResponse resp = minio.downloadDelta(story.getMinioId());

            // delta could not be fetched for some reason
            if (resp == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    Json.createObjectBuilder()
                    .add(
                        "error", "Delta for story `" + story.getTitle() + "` could not be fetched"
                    )
                    .build()
                ).build();
            }
 
            // deserialise json data
            deltaJson = GeneralUtils.JsonDeserialise(resp.readAllBytes());
            
        } catch (ErrorResponseException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Delta for story `" + story.getTitle() + "` does not exist"
                )
                .build()
            ).build();

        // io issues with loading delta into memory
        } catch (IOException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", 
                    "Delta for story `" + story.getTitle() + "` could not be loaded on server"
                )
                .build()
            ).build();
        } 

        return Response.ok(
            deltaJson
        ).build();
    }

    /**
     * Fetches the raw text of a particular story,
     * @param storyId Id of the story to fetch the raw text from.
     * @return Raw text of the story as a JsonString. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @GET
    @Path("/content/fetch/text/{story-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getText(@PathParam("story-id") long storyId) {
        mustBeAuthenticated();

        // find story in db 
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = " + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        return Response.ok(
            Json.createObjectBuilder()
            .add("text", story.getText())
            .build()
        ).build();
    }

    /**
     * Fetches the html of a particular story,
     * @param storyId Id of the story to fetch the html from.
     * @return HTML of the story as a JsonString. See
     * {@link https://nishgang.atlassian.net/wiki/spaces/KB/pages/7798788/API+Documentation}
     * for more information.
     * */
    @GET
    @Path("/content/fetch/html/{story-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHtml(@PathParam("story-id") long storyId) {

        // find story in db 
        Story story = Story.findById(storyId);
        if (story == null) {
            return Response.status(Status.NOT_FOUND).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Story with id = " + storyId + " does not exist"
                )
                .build()
            ).build();
        }

        JsonObject htmlJson;
        try {
            // fetch delta json file from minIO 
            GetObjectResponse resp = minio.downloadHtml(story.getMinioId());

            // delta could not be fetched for some reason
            if (resp == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    Json.createObjectBuilder()
                    .add(
                        "error", "Html for story `" + story.getTitle() + "` could not be fetched"
                    )
                    .build()
                ).build();
            }
 
            // deserialise json data
            htmlJson = GeneralUtils.JsonDeserialise(resp.readAllBytes());
            
        } catch (ErrorResponseException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", "Html for story `" + story.getTitle() + "` does not exist"
                )
                .build()
            ).build();

        // io issues with loading delta into memory
        } catch (IOException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                Json.createObjectBuilder()
                .add(
                    "error", 
                    "Html for story `" + story.getTitle() + "` could not be loaded on server"
                )
                .build()
            ).build();
        } 

        return Response.ok(
            htmlJson
        ).build();
    }

    /**
     * Parses new selections into a mapping between page numbers and the selections
     * contained on each page.
     * @param selections JsonArray of selections in json format
     * @return a map between page numbers and a list of bounds located on each page
     * */
    public Map<Integer, List<Bounds>> parseSelections(JsonArray selections) 
        throws NullPointerException, ClassCastException {

            // add selections to story
            Map<Integer, List<Bounds> > pageBoundsMap = new HashMap<>();
            for (JsonValue selection : selections) {
                JsonObject selectionObject = selection.asJsonObject();

                // extract fields from selection object
                int x           = selectionObject.getJsonNumber("x").intValue();
                int y           = selectionObject.getJsonNumber("y").intValue();
                int width       = selectionObject.getJsonNumber("width").intValue();
                int height      = selectionObject.getJsonNumber("height").intValue();
                int pageNum     = selectionObject.getJsonNumber("pageNumber").intValue();
                int sequenceNum = selectionObject.getJsonNumber("sequenceNumber").intValue(); 

                // add rectangle to Map<PageNumber, Rect>
                List<Bounds> bounds = pageBoundsMap.getOrDefault(pageNum, new ArrayList<>());
                if (!pageBoundsMap.containsKey(pageNum)) {
                    pageBoundsMap.put(pageNum, bounds);
                }
                bounds.add(new Bounds(x, y, width, height, sequenceNum));
            } 
            return pageBoundsMap;
    }

    /**
     * Scans text from a pdf given a page number and a list of bounds
     * @param pdfWrapper a pdf document wrapped in a PDFWrapper
     * @param pageNum page of pdf file to scan text from
     * @param bounds sections of the page to scan textfrom
     * @return a JsonArray of strings, with each denoting a new paragraph, returning 
     * an empty JsonArray when an error has occured. This should never be null.
     * */
    public JsonArray generateText(PDFWrapper pdfWrapper, Integer pageNum, List<Bounds> bounds) {

        // check if python script is extracting correctly
        // null means the script failed to extract text from the pdf in an orderly fashion
        JsonObject textChunk = pdfWrapper.getText(pageNum, bounds);
        if (textChunk == null) {
            log.info(
                "Text on page " + pageNum + " failed to generate"
            );
            return Json.createArrayBuilder().build();
        }

        try {
            return textChunk.getJsonArray("paragraphs");
        } catch (NullPointerException e) {
            log.error(ANSIColour.doRed(e.getMessage()));
            return Json.createArrayBuilder().build();
        }
    }

    /**
     * Generates text in order of the bounds' sequences number, attempting to group
     * selections in order on the same page for text extraction
     * @param pdfWrapper pdf documnet wrapped in PDFWrapper
     * @param pageBoundsMap a mapping of a page number to a list of bounds on that page
     * @return a JsonArray of strings, with each denoting a new paragraph and otherwise returns 
     * an empty JsonArray when an error has occured. This should never be null.
     * */
    public JsonArray generateTextBySequence(PDFWrapper pdfWrapper, Map<Integer, List<Bounds>> pageBoundsMap) {

        // output builder
        JsonArrayBuilder paragraphBuilder = Json.createArrayBuilder();

        // no bounds means no text to extract
        if (pageBoundsMap.isEmpty())
            return paragraphBuilder.build();

        // flatten pageBoundsMap to a list of tuples
        List<Pair<Bounds, Integer>> boundsPageNumberPairs = new ArrayList<>();
        for (Map.Entry<Integer, List<Bounds>> entry: pageBoundsMap.entrySet()) {
            Integer pageNum = entry.getKey();
            List<Bounds> boundsOnPage = entry.getValue();
            for(Bounds bounds : boundsOnPage) {
                boundsPageNumberPairs.add(new ImmutablePair<>(bounds, pageNum));
            }
        }

        // sort bounds by sequence number
        Collections.sort(boundsPageNumberPairs, (x, y) -> {
            return Integer.compare(x.getKey().getSequenceNum(), y.getKey().getSequenceNum());
        });

        // generate text in sequence by page
        List<Bounds> boundsOnCurrentPage = new ArrayList<>();
        Integer currentPage = null;
        for (Pair<Bounds, Integer> pair : boundsPageNumberPairs) {

            Bounds bounds = pair.getKey();
            Integer pageNum = pair.getValue();

            if (pageNum == currentPage) {
                boundsOnCurrentPage.add(bounds);
            } else if (currentPage == null) {
                currentPage = pageNum;
                boundsOnCurrentPage.add(bounds);
            } else {

                // generate text
                JsonArray paragraphs = generateText(pdfWrapper, currentPage, boundsOnCurrentPage);

                // add generated paragraphs to paragraphBuilder
                for (JsonValue paragraph : paragraphs) {
                    paragraphBuilder.add(paragraph);
                }

                // reset boundsOnPage and append the bounds for the new page
                currentPage = pageNum;
                boundsOnCurrentPage.clear();
                boundsOnCurrentPage.add(bounds);
            }
        }

        // last grouping of bounds on the page that didn't get scanned for text
        if (!boundsOnCurrentPage.isEmpty()) {

            // generate text
            JsonArray paragraphs = generateText(pdfWrapper, currentPage, boundsOnCurrentPage);
            for (JsonValue paragraph : paragraphs) {
                paragraphBuilder.add(paragraph);
            }
        }

        // build output
        return paragraphBuilder.build();
    }
}
