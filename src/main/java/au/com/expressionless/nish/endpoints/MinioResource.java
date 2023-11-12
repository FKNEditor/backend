package au.com.expressionless.nish.endpoints;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jboss.logging.Logger;

import au.com.expressionless.nish.service.MinIO;
import io.minio.GetObjectResponse;
import io.minio.errors.ErrorResponseException;

@Path("v3/media/public")
@ApplicationScoped
public class MinioResource {

    @Inject
    MinIO minio;

    static final Logger log = Logger.getLogger(MinioResource.class);

    private Response toJaxResponse(okhttp3.Response okhttpResp) {
        ResponseBuilder builder = Response.status(okhttpResp.code());
        // TODO: COPY HEADERS AND BODY
        return builder.build();
    }

    /**
     * MinIO Endpoint for fetching and downloading thumbnail
     * @param minioId Id for the thumbnail that needs to be downloaded
     * @return The thumbnail file linked to the minioID
     * */
    @GET
    @Path("/fetch/tbn/{minio-id}")
    @Produces("application/jpeg")
    public Response downloadFile(@PathParam("minio-id") String minioId) {
        log.info("Downloading thumbnail for id = " + minioId);
        try {
            GetObjectResponse resp = minio.downloadThumbnail(minioId);
            if(resp == null) {
                return Response.serverError().build();
            }
            return Response.ok(resp)
                .header("Content-Disposition", "attachment; filename=\"" + minioId + "\"")
                .build();
        } catch (ErrorResponseException e) {
            return toJaxResponse(e.response());
        }
    }

    /**
     * MinIO Endpoint for fetching and downloading pdf file
     * @param minioId Id for the pdf that needs to be downloaded
     * @return The pdf file linked to the minioID
     * */
    @GET
    @Path("/fetch/pdf/{minio-id}")
    @Produces("multipart/form-data")
    public Response downLoadPdf(final @PathParam("minio-id") String minioId) {
        log.info("Downloading PDF with id = " + minioId);
        try {
            GetObjectResponse resp = minio.downloadPdf(minioId);
            if(resp == null) {
                return Response.serverError().build();
            }
            return Response.ok(resp)
                .header("Content-Disposition", "attachment; filename=\"" + minioId + "\"")
                .build();
        } catch (ErrorResponseException e) {
            return toJaxResponse(e.response());
        }
    }
}
