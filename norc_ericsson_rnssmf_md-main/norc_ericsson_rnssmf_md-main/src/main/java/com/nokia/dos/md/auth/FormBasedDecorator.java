package com.nokia.dos.md.auth;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.nokia.dos.md.utils.CommonUtils;
import com.nokia.dos.md.utils.UrlUtils;
import com.nokia.dos.md.utils.WebClientFactory;

import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.Connection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FormBasedDecorator implements AuthRequestDecorator {

    public static final String USER_FORM_PARAMETER_KEY = "logInFormUsernameParameter";
    public static final String USER_KEY = "logInUsername";
    public static final String PASSWORD_FORM_PARAMETER_KEY = "logInFormPasswordParameter";
    public static final String PASSWORD_KEY = "logInPassword";
    public static final String URL_KEY = "logInUrl";
    public static final String CONTENT_TYPE_KEY = "logInContentType";

    private MultiValueMap<String, String> cookies;

    public FormBasedDecorator(Connection connection) {
        Map<String, String> properties = connection.getProperties();

        String usernameParameter = requireNonNull(properties.get(USER_FORM_PARAMETER_KEY), "Missing properties parameter: " + USER_FORM_PARAMETER_KEY);
        String username = requireNonNull(properties.get(USER_KEY), "Missing properties parameter: " + USER_KEY);
        String passwordParameter = requireNonNull(properties.get(PASSWORD_FORM_PARAMETER_KEY), "Missing properties parameter: " + PASSWORD_FORM_PARAMETER_KEY);
        String password = requireNonNull(properties.get(PASSWORD_KEY), "Missing properties parameter: " + PASSWORD_KEY);
        String url = requireNonNull(properties.get(URL_KEY), "Missing properties parameter: " + URL_KEY);
        String contentType = requireNonNull(properties.get(CONTENT_TYPE_KEY), "Missing properties parameter: " + CONTENT_TYPE_KEY);

        cookies = new LinkedMultiValueMap<>();

        // Use a temporary web client to avoid overhead if we were to
        // use the one of HttpMechanismDriverSession
        Set<CertificateStore> cs = CommonUtils.getCertificateStore(connection);

        WebClient webClientForLogIn = WebClientFactory.createSecureWebClient(true, cs);
        String requestBody = encodeBody(contentType, usernameParameter, username, passwordParameter, password);
        ClientResponse logInResponse = webClientForLogIn.post()
            .uri(url)
            .bodyValue(requestBody)
            .header("Content-Type", contentType)
            .exchange()
            .block();

        if (logInResponse == null) {
            log.warn("Response from log-in for the form-based decorator resulted in null");
            return;
        }

        storeAuthenticationCookies(logInResponse);
    }

    private String encodeBody(String contentType, String usernameParameter, String username, String passwordParameter, String password) {
        if (contentType.equals(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            String formattedData = String.format("%s=%s&%s=%s", usernameParameter, username, passwordParameter, password);
            return UrlUtils.encodeUtf8(formattedData);
        }

        throw new UnsupportedOperationException("No body encoding implementation for content type: " + contentType);
    }

    @Override
    public void enhanceRequest(WebClient.RequestHeadersSpec<?> request) {
        if (cookies == null) {
            throw new IllegalStateException("Cookies not initialized when FormBased decorator got a request to enhance request " + request);
        }
        log.debug("FormBased decorator got a request to enhance request {}", request);
        request.cookies(cookiesMultiValueMap -> cookiesMultiValueMap.addAll(cookies));
    }

    private void storeAuthenticationCookies(ClientResponse logInResponse) {
        MultiValueMap<String, ResponseCookie> responseCookies = logInResponse.cookies();

        log.debug("Setting {} cookie(s)", responseCookies.size());
        for (String key : responseCookies.keySet()) {
            for (ResponseCookie cookie : responseCookies.get(key)) {
                final String cookieName = cookie.getName();
                final String cookieValue = cookie.getValue();
                cookies.add(cookieName, cookieValue);
            }
        }
    }
}
