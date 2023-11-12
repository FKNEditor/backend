package au.com.expressionless.nish.models.auth;

import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTransient;

// import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import au.com.expressionless.nish.exception.BadTokenException;

// @ApplicationScoped
public class KeycloakToken {
	static final Logger log = Logger.getLogger(KeycloakToken.class);
	static Jsonb jsonb = JsonbBuilder.create();

	@JsonbTransient
	private JsonObject decodedToken;

	private String encodedToken;

	public KeycloakToken(final String rawToken) {
		init(rawToken);
	}

	public void init(String rawToken) {
		decodedToken = decodeToken(rawToken);
		encodedToken = rawToken;
	}

	/**
	 * @param bearerToken the bearer token to decode
	 * @return JsonObject the decoded Json Object
	 * 
	 * @throws {@link BadTokenException} if the bearer token is invalid
	 */
	@JsonbTransient
	private JsonObject decodeToken(final String bearerToken) {

		final String[] chunks = bearerToken.split("\\.");
		if(false) {//chunks.length < 2 || StringUtils.isBlank(bearerToken)) {
			throw new BadTokenException(bearerToken);
		}
        
		Base64.Decoder decoder = Base64.getDecoder();
		String payload = new String(decoder.decode(chunks[1]));
		JsonObject json = jsonb.fromJson(payload, JsonObject.class);

		return json;
	}

	public void setToken(String token) {
		token = encodedToken;
	}

	public String getToken() {
		return encodedToken;
	}
}
