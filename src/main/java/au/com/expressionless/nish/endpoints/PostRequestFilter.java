package au.com.expressionless.nish.endpoints;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

// @Provider
public class PostRequestFilter implements ContainerResponseFilter {

    Logger log = Logger.getLogger("PostRequestFilter");

    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
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

        log.info("=================================================");
        log.info("Response:");
        logResponse(responseContext);
    }
    private void logResponse(ContainerResponseContext resp) {
        log.info("Responding with " + resp.getStatus());
        log.info("Headers: ");
        if(resp.getHeaders() != null) {
            log.info("Header count: " + resp.getHeaders().size());
            
            Iterator<Map.Entry<String, List<Object>>> iter = resp.getHeaders().entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry<String, List<Object>> e = iter.next();
                String line = e.getKey() + " = ";
                for(Object l : e.getValue()) {
                    line += l.toString();
                }
                log.info(line);
            }
        } else {
            log.info("No headers present");
        }
    }
}