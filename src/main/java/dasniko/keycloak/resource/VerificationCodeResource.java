package dasniko.keycloak.resource;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import java.util.Map;
import java.util.Locale;

import dasniko.keycloak.authenticator.SmsAuthenticator;
import dasniko.keycloak.authenticator.SmsAuthenticatorFactory;
import dasniko.keycloak.authenticator.SmsConstants;
import dasniko.keycloak.authenticator.gateway.SmsServiceFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class VerificationCodeResource {

    private static final Logger logger = Logger.getLogger(VerificationCodeResource.class);
    private final KeycloakSession session;
    private final AuthResult auth;

    VerificationCodeResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @GET
    @NoCache
    @Path("")
    @Produces(APPLICATION_JSON)
    public Response getVerificationCode(@QueryParam("phoneNumber") String phoneNumber) {

        if (phoneNumber == null)
            throw new BadRequestException("Must inform a phone number");

        // get authentication session

        UserModel user = auth.getUser();

        RealmModel realm = session.getContext().getRealm();
        Map<String, String> config = realm.getAuthenticatorConfigByAlias("ecall").getConfig();
        try {
            // TODO: get currently selected locale, maybe from Headers?
            String code = SmsAuthenticator.codeChallenge(config, session, null, phoneNumber);
            user.setSingleAttribute(SmsConstants.CODE, code);
            int ttl = Integer.parseInt(config.get(SmsConstants.CODE_TTL));
            user.setSingleAttribute(SmsConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
            logger.warn(String.format("Verification code %s sent to %s", code, phoneNumber));
        } catch (Exception e) {
            logger.error("Error while generating verification code", e);
            return Response.serverError().build();
        }
        // SmsServiceFactory.get(config, session).send(phoneNumber, "Code: 123");

        return Response.noContent().build();
    }
}
