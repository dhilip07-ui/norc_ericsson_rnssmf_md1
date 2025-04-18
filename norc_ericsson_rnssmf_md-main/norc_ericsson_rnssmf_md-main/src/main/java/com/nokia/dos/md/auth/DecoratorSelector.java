package com.nokia.dos.md.auth;

import java.util.Map;
import java.util.Set;

import com.nokia.dos.md.utils.CommonUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.nokia.fo.nero.Authentication;
import com.nokia.fo.nero.BasicAuth;
import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.Connection;
import com.nokia.fo.nero.OAuth20;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class DecoratorSelector {

    public static final String AUTHENTICATION_TYPE = "AUTHENTICATION-TYPE";
    // The following are the non-natively supported by Nero authentication
    // types, meaning their configuration is read from the connection
    // properties field, and not from the connection authentication.
    public static final String FORM_BASED_AUTHENTICATION_TYPE = "FORM-BASED";

    public static AuthRequestDecorator selectDecorator(Connection connection, WebClient webClient) {
        Authentication authentication = connection.getAuthentication();

        // Check for the authentication types natively supported by Nero
        if (authentication instanceof BasicAuth) {
            log.debug("BasicAuth decorator selected as decorator for connection {}", connection);
            BasicAuth basicAuth = (BasicAuth) authentication;
            return new BasicAuthDecorator(basicAuth.getUsername(), basicAuth.getPassword());
        }
        if (authentication instanceof OAuth20) {
            log.debug("OAuth 2.0 decorator selected as decorator for connection {}", connection);
            Set<CertificateStore> cs = CommonUtils.getCertificateStore(connection);
            
            OAuth20 oAuth20 = (OAuth20) authentication;
            return new EBearerAuthDecorator(oAuth20, cs, webClient);
        }
        // Check for authentication types not natively supported by Nero
        String authenticationType = getAuthenticationType(connection);
        if (authenticationType != null && authenticationType.equals(FORM_BASED_AUTHENTICATION_TYPE)) {
            log.debug("Form-based decorator selected as decorator for connection {}", connection);
            return new FormBasedDecorator(connection);
        }

        log.debug("Could not select request decorator for connection {}. Defaulting to NO-OP...", connection);
        return new NoOpDecorator();
    }

    private static String getAuthenticationType(Connection connection) {
        String result = null;

        Map<String, String> properties = connection.getProperties();
        if (properties != null) {
            result = properties.get(AUTHENTICATION_TYPE);
        }

        return result;
    }
}
