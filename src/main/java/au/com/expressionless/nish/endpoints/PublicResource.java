package au.com.expressionless.nish.endpoints;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/api/public")
@ApplicationScoped
public class PublicResource extends SecureResource {
    
    private String version = "v0.0.3";

    @GET
    @Path("version")
    @Produces(MediaType.TEXT_PLAIN)
    public String fetchVersion() {
        return version;
    }
    
}
