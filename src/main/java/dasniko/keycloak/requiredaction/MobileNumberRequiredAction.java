package dasniko.keycloak.requiredaction;

import java.util.logging.Logger;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.validation.Validation;

import java.util.function.Consumer;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class MobileNumberRequiredAction implements RequiredActionProvider {
	private static final Logger log = Logger.getLogger(MobileNumberRequiredAction.class.getName());

	public static final String PROVIDER_ID = "mobile-number-ra";

	private static final String MOBILE_NUMBER_FIELD = "mobile_number";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		if (context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD) == null) {
			context.getUser().addRequiredAction(PROVIDER_ID);
			context.getAuthenticationSession().addRequiredAction(PROVIDER_ID);
		}
	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// show initial form
		context.challenge(createForm(context, null));
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// submitted form

		UserModel user = context.getUser();

		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String mobileNumber = formData.getFirst(MOBILE_NUMBER_FIELD);
		String code = formData.getFirst("code");

		if (Validation.isBlank(mobileNumber) || mobileNumber.length() < 5) {
			context.challenge(createForm(context, form -> form.addError(new FormMessage(
					MOBILE_NUMBER_FIELD,
					"Bitte erfassen Sie eine gültige Telefonnummer"))));
			return;
		}

		if (user.getFirstAttribute("code") == null
				|| !mobileNumber.equals(user.getFirstAttribute(MOBILE_NUMBER_FIELD))) {
			String generatedCode = String.valueOf((int) Math.floor(Math.random() * 100000));
			log.warning("no code found or mobile number changed, sending code" + generatedCode);
			user.setSingleAttribute(MOBILE_NUMBER_FIELD, mobileNumber);
			user.setSingleAttribute("code", generatedCode);
			context.challenge(createForm(context, form -> {
				form.setAttribute("codeSent", true);
				form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);
			}));
			return;
		}

		if (Validation.isBlank(code)) {
			context.challenge(createForm(context, form -> {
				form.addError(new FormMessage("code", "Bitte Code eingeben"));
				form.setAttribute("codeSent", true);
				form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);
			}));
			return;
		}
		if (!code.equals(user.getFirstAttribute("code"))) {
			context.challenge(createForm(context, form -> {
				form.addError(new FormMessage("code", "Code ungültig"));
				form.setAttribute("codeSent", true);
				form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);
			}));
			return;
		}

		user.setSingleAttribute("verifiedMobileNr", "true");
		user.removeAttribute("code");
		user.removeRequiredAction(PROVIDER_ID);
		context.getAuthenticationSession().removeRequiredAction(PROVIDER_ID);

		context.success();
	}

	@Override
	public void close() {
	}

	private Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formConsumer) {
		LoginFormsProvider form = context.form();
		form.setAttribute("username", context.getUser().getUsername());

		String mobileNumber = context.getUser().getFirstAttribute(MOBILE_NUMBER_FIELD);
		form.setAttribute(MOBILE_NUMBER_FIELD, mobileNumber == null ? "" : mobileNumber);

		if (formConsumer != null) {
			formConsumer.accept(form);
		}

		return form.createForm("update-mobile-number.ftl");
	}

}
