# Keycloak 2FA SMS Authenticator

Originally based on https://github.com/dasniko/keycloak-2fa-sms-authenticator and extended to include mobile number verification in required action.

## Building

```cmd
mvn clean package
```

After, put the JAR file in `/target` into your Keycloak's `/opt/keycloak/providers/` folder.
