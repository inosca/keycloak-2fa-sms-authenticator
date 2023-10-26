<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('mobile_number'); section>
<#if section = "header">
    ${msg("updateMobileTitle")}
<#elseif section = "form">
    <script src="https://cdn.jsdelivr.net/npm/vue/dist/vue.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>

    <div id="vue-app">
        <p>${msg("updateMobileText")}</p>
        <form id="kc-mobile-update-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="mobile_number"class="${properties.kcLabelClass!}">${msg("updateMobileFieldLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="tel" id="mobile_number" name="mobile_number" class="${properties.kcInputClass!}"
                value="${mobile_number!''}" required aria-invalid="<#if messagesPerField.existsError('mobile_number')>true</#if>"/>
                    <#if messagesPerField.existsError('mobile_number')>
                        <span id="input-error-mobile-number" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('mobile_number'))?no_esc}
                        </span><br />
                    </#if>
                    <span class="uk-text-small uk-text-muted">${msg("mobileNumberHelp")}</span>
                </div>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <input
                class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                v-model="sendButtonText" :disabled='sendButtonText !== initSendButtonText'
                v-on:click="sendVerificationCode()"
                type="button" value="${msg("sendVerificationCode")}"/>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <label for="code" class="${properties.kcLabelClass!}">${msg("verificationCode")}</label>
                <input id="code" class="${properties.kcInputClass!}" name="code"
            type="text" <#if mobile_number??>autofocus</#if>
                autocomplete="off"/>
            </div>
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}"/>
                </div>
            </div>
        </form>
    </div>
    <script type="text/javascript">
        function req(phoneNumber) {
            const params = {params: {phoneNumber}}
            axios.get(window.location.origin + '/auth/realms/${realm.name}/sms/verification-code', params)
                .then(res => app.disableSend(10))
                .catch(e => app.errorMessage = e.response.data.error);
        }

        const app = new Vue({
            el: '#vue-app',
            data: {
                errorMessage: '',
                phoneNumber: '',
                sendButtonText: '${msg("sendVerificationCode")}',
                    initSendButtonText: '${msg("sendVerificationCode")}',
                    disableSend: function (seconds) {
                        if (seconds <= 0) {
                            app.sendButtonText = app.initSendButtonText;
                        } else {
                            const minutes = Math.floor(seconds / 60) + '';
                            const seconds_ = seconds % 60 + '';
                                app.sendButtonText = String(minutes.padStart(2, '0') + ":" + seconds_.padStart(2, '0'));
                                setTimeout(function () {
                                    app.disableSend(seconds - 1);
                                }, 1000);
                            }
                        },
                        sendVerificationCode: function () {
                            this.errorMessage = '';
                            const phoneNumber = document.getElementById('mobile_number').value.trim();
                            if (!phoneNumber) {
                                this.errorMessage = '${msg("requiredPhoneNumber")}';
                                document.getElementById('mobile_number').focus();
                                return;
                            }
                            if (this.sendButtonText !== this.initSendButtonText) return;
                            req(phoneNumber);
                        }
                    }
                });
        <#if mobile_number??>
            req('${mobile_number}');
        </#if>
      </script>
    </#if>
</@layout.registrationLayout>
