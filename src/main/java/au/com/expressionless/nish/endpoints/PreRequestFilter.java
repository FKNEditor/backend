package au.com.expressionless.nish.endpoints;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

// @Provider
// @PreMatching
public class PreRequestFilter implements ContainerRequestFilter {

    Logger log = Logger.getLogger("PreRequestFilter");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        log.info("Request to: " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri().toString());
        log.info("HEADERS");
        for(Map.Entry<String, List<String>> header : requestContext.getHeaders().entrySet()) {
            String end = "";
            Iterator<String> iter = header.getValue().iterator();
            while(iter.hasNext()) {
                end += iter.next() + ",";
            }
            String line = header.getKey() + " = " + end;
            log.info(line);
        }
        log.info("Media type: " + (requestContext.getMediaType() != null ? requestContext.getMediaType().toString() : "null"));
        if(requestContext.hasEntity()) {
            log.info("Entity detected");
            byte[] byteArr = requestContext.getEntityStream().readAllBytes();
            String str = new String(byteArr);
            log.info("Entity: ");
            log.info(str);
        } else {
            log.info("No entity provided");
        }
    }
    
}
