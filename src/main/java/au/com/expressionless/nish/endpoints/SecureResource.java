package au.com.expressionless.nish.endpoints;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * An abstract SecureResource
 */
@ApplicationScoped
public abstract class SecureResource {

    @Inject
    JsonWebToken token;

    /**
     * Test whether or not the token is authenticated
     * @return
     */
    public boolean isAuthenticated() {
        return token != null && !StringUtils.isBlank(token.getRawToken());
    }

    /**
     * Force Authentication. If the token is not authenticated according to {@link SecureResource#isAuthenticated isAuthenticated}
     * then this will throw a ForbiddenException
     */
    public void mustBeAuthenticated() throws ForbiddenException {
        if(!isAuthenticated())
            throw new ForbiddenException("User Not Authorized");
    }
}
