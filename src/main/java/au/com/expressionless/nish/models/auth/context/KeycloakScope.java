package au.com.expressionless.nish.models.auth.context;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import au.com.expressionless.nish.models.auth.KeycloakToken;
import io.quarkus.arc.Arc;

/**
 * Keycloak Scope initializer (for injecting KeycloakTokens)
 **/
// @ApplicationScoped
public class KeycloakScope {

	static final Logger log = Logger.getLogger(KeycloakScope.class);
	static final Jsonb jsonb = JsonbBuilder.create();

	// @Inject
	// KeycloakToken keycloakToken;

	/**
	 * Default Constructor.
	 **/
    @Deprecated
	public KeycloakScope() { }

	/**
	 * Activate the KeycloakToken using the request context 
	 *
	 * @param data The bearer token
	 **/
	public void init(String rawToken) { 

		// activate request scope and fetch UserToken
		Arc.container().requestContext().activate();

		if (rawToken == null) {
			log.error("Null data received at Scope Init");
			return;
		}

		try {

			JsonObject json = jsonb.fromJson(rawToken, JsonObject.class);
			String token = json.getString("token");

			// init GennyToken from token string
			// keycloakToken.init(token);
			// log.debug("Token Initialized: " + keycloakToken);

		} catch (Exception e) {
			log.error("Error initializing token: " + rawToken);
			e.printStackTrace();
		}
    }

	/**
	 * Destroy the UserToken using the request context.
	 **/
	public void destroy() { 
		Arc.container().requestContext().activate();
	}
}
