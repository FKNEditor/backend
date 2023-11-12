package au.com.expressionless.nish.endpoints;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import au.com.expressionless.nish.models.auth.KeycloakToken;
import au.com.expressionless.nish.models.auth.context.KeycloakScope;

@Path("/v1/user")
@RequestScoped
public class UserResource {

    // @Inject
    // KeycloakToken keycloakToken;

    // @Inject
    // KeycloakScope keycloakScope;
}
