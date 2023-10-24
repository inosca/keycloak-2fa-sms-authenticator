package dasniko.keycloak.resource;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

import jakarta.ws.rs.Path;

public class SmsResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }

    public SmsResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Path("verification-code")
    public VerificationCodeResource getVerificationCodeResource() {
        return new VerificationCodeResource(session);
    }
}
