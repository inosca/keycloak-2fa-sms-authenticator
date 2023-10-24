package dasniko.keycloak.authenticator;

import java.util.Locale;
import java.util.logging.Logger;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import dasniko.keycloak.authenticator.gateway.SmsServiceFactory;
import jakarta.ws.rs.core.Response;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class SmsAuthenticator implements Authenticator {
	private static final Logger log = Logger.getLogger(SmsAuthenticator.class.getName());

	private static final String MOBILE_NUMBER_FIELD = "mobile_number";
	private static final String TPL_CODE = "login-sms.ftl";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		log.warning("authenticate");

		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		KeycloakSession session = context.getSession();
		UserModel user = context.getUser();

		String mobileNumber = user.getFirstAttribute(MOBILE_NUMBER_FIELD);
		if (mobileNumber == null) {
			log.warning("no mobile number configured, showing form");
			Response challenge = context.form().createForm("update-mobile-number.ftl");
			context.challenge(challenge);
			return;
		}

		codeChallenge(context);

	}

	private void codeChallenge(AuthenticationFlowContext context) {
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		KeycloakSession session = context.getSession();
		UserModel user = context.getUser();

		String mobileNumber = user.getFirstAttribute(MOBILE_NUMBER_FIELD);

		int length = Integer.parseInt(config.getConfig().get(SmsConstants.CODE_LENGTH));
		int ttl = Integer.parseInt(config.getConfig().get(SmsConstants.CODE_TTL));

		String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		authSession.setAuthNote(SmsConstants.CODE, code);
		authSession.setAuthNote(SmsConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));

		try {
			Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
			Locale locale = session.getContext().resolveLocale(user);
			String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
			String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));

			SmsServiceFactory.get(config.getConfig(), session).send(mobileNumber, smsText);

			context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TPL_CODE));
		} catch (Exception e) {
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
					context.form().setError("smsAuthSmsNotSent", e.getMessage())
							.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		log.warning("action");
		String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst(SmsConstants.CODE);

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		String code = authSession.getAuthNote(SmsConstants.CODE);
		String ttl = authSession.getAuthNote(SmsConstants.CODE_TTL);

		if (code == null || ttl == null) {
			String enteredMobileNr = context.getHttpRequest().getDecodedFormParameters().getFirst("mobile_number");
			log.warning("no code found, saving mobile number: " + enteredMobileNr);
			UserModel user = context.getUser();
			user.setSingleAttribute(MOBILE_NUMBER_FIELD, enteredMobileNr);

			codeChallenge(context);
			return;
		}

		boolean isValid = enteredCode.equals(code);
		if (isValid) {
			if (Long.parseLong(ttl) < System.currentTimeMillis()) {
				// expired
				context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
					context.form().setError("smsAuthCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
			} else {
				// valid
				context.success();
			}
		} else {
			// invalid
			AuthenticationExecutionModel execution = context.getExecution();
			if (execution.isRequired()) {
				context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
					context.form().setAttribute("realm", context.getRealm())
						.setError("smsAuthCodeInvalid").createForm(TPL_CODE));
			} else if (execution.isConditional() || execution.isAlternative()) {
				context.attempted();
			}
		}
	}

	@Override
	public boolean requiresUser() {
		return true;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}

}
