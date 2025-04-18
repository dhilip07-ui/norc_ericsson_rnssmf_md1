package com.nokia.dos.md.auth;

import static com.nokia.dos.md.utils.Base64Utils.encodeBase64;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nokia.dos.domainadaptation.mechanismdriver.MechanismDriverException;
import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.OAuth20;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * This implementation for OAuth 2.0 is very naive.
 * - It only supports resource owner password credentials
 *   (https://tools.ietf.org/html/rfc6749#section-1.3.3)
 * - A new token is requested on-demand for each request
 *
 * Future improvements should include the usage of OAuth2AuthorizedClientManager
 * (https://docs.spring.io/spring-security/site/docs/current/reference/html5/#oauth2Client-authorized-manager-provider)
 */
@Slf4j
public class OAuth2Decorator implements AuthRequestDecorator {

    WebClient webClientForAccessToken = null;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String REFRESH_EXPIRES_IN = "refresh_expires_in";
    private static final String GRANT_TYPE = "grant_type";

    private OAuth20 oAuth20;

    private Token accessToken;
    private Token refreshToken;

    public OAuth2Decorator(OAuth20 oAuth20, Set<CertificateStore> certificateStore, WebClient webClient) {
        log.debug("Creating OAuth2Decorator with configuration: {}", oAuth20);

        this.oAuth20 = oAuth20;
        this.webClientForAccessToken = webClient;
        
        clearTokens();
    }

    @Override
    public void enhanceRequest(WebClient.RequestHeadersSpec<?> request) {
        log.debug("OAuth 2.0 decorator got a request to enhance request {}", request);
        request.header("Authorization", "Bearer " + getAccessToken());
    }

    @Override
    public boolean operationFailedAndShouldBeAutomaticallyRetried(HttpStatus httpStatus) {
        return httpStatus.equals(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void applyCorrectiveAction() {
        log.info("OAuth2 authentication failed for latest executed request. Refreshing tokens before retrying");
        refreshTokens();
    }

    private String getAccessToken() {
        if (accessToken == null) {
            log.debug("No access token found. Refreshing tokens");
            refreshTokens();
        } else if (!accessToken.isValid()) {
            if (refreshToken != null && refreshToken.isValid()) {
                log.debug("Current access token is expired. Requesting new access token with valid refresh token.");
                refreshTokensWithRefreshToken();
            } else {
                log.debug("Current access and refresh tokens are expired. Refreshing tokens");
                refreshTokens();
            }
        } else {
            log.debug("Current access token is valid.");
        }

        return accessToken.getValue();
    }

    private void refreshTokensWithRefreshToken() {
        log.debug("Refreshing tokens with refresh token.");
        String accessTokenUri = generateAccessTokenUri();
        String authorizationHeader = generateTokenRequestAuthorizationHeader(oAuth20);
        String requestBody = generateGrantRequestBodyWithRefreshToken(oAuth20);

        String responseBody = retrieveTokenFromServer(accessTokenUri, authorizationHeader, requestBody);

        JsonNode responseJson;
        try {
            responseJson = OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new MechanismDriverException("Failed to read response as JSON: " + responseBody, e);
        }

        if (responseJson.has(ACCESS_TOKEN) && responseJson.has(EXPIRES_IN)) {
            accessToken = new Token(responseJson.get(ACCESS_TOKEN).asText(), responseJson.get(EXPIRES_IN).asInt());
            log.debug("Access token successfully refreshed with refresh token.");
        } else {
            log.warn("Could not read access token data from refresh token response.");
        }

        if (responseJson.has(REFRESH_TOKEN) && responseJson.has(REFRESH_EXPIRES_IN)) {
            refreshToken = new Token(responseJson.get(REFRESH_TOKEN).asText(), responseJson.get(REFRESH_EXPIRES_IN).asInt());
            log.debug("Refreshed token successfully refreshed with refresh token.");
        } else {
            // Apparently, if the refresh_token is not present, it means that it can keep being reused
            log.debug("Could not read refresh token data from refresh token response.");
        }
    }

    /**
     * Generates the URI for getting an access token.
     *
     * If the port is specified, it will be taken into account, otherwise it will not be part of the URI.
     *
     * @return URI to use for getting an access token.
     */
    private String generateAccessTokenUri() {
        if (oAuth20.getPort() == null) {
            return String.format("%s%s", oAuth20.getHost(), oAuth20.getTokenUrlPath());
        } else {
            return String.format("%s:%s%s", oAuth20.getHost(), oAuth20.getPort(), oAuth20.getTokenUrlPath());
        }
    }

    private String retrieveTokenFromServer(String accessTokenUri, String authorizationHeader, String requestBody) {
        return webClientForAccessToken.post()
            .uri(accessTokenUri)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", authorizationHeader)
            .bodyValue(requestBody)
            .exchange().block()
            .bodyToMono(String.class).block();
    }

    private String generateGrantRequestBodyWithRefreshToken(OAuth20 oAuth20) {
        switch (oAuth20.getGrantType()) {
            case "password":
                return generatePasswordGrantRequestBodyWithRefreshToken(oAuth20);
            case "client_credentials":
                return generateClientCredentialsGrantRequestBodyWithRefreshToken(oAuth20);
            default:
                throw new UnsupportedOperationException("Unsupported grant type: " + oAuth20.getGrantType());
        }
    }

    private String generatePasswordGrantRequestBodyWithRefreshToken(OAuth20 oAuth20) {
        return encodeUrlFormBody(
            entryOf(GRANT_TYPE, oAuth20.getGrantType()),
            entryOf("username", oAuth20.getUsername()),
            entryOf("password", oAuth20.getPassword()),
            entryOf(REFRESH_TOKEN, refreshToken.getValue()));
    }

    private String generateClientCredentialsGrantRequestBodyWithRefreshToken(OAuth20 oAuth20) {
        List<Map.Entry<String, String>> variables = new ArrayList<>();
        variables.add(entryOf(GRANT_TYPE, oAuth20.getGrantType()));
        variables.add(entryOf(REFRESH_TOKEN, refreshToken.getValue()));

        return encodeUrlFormBody(variables);
    }

    private void refreshTokens() {
        clearTokens();

        log.debug("Refreshing tokens");
        String accessTokenUri = generateAccessTokenUri();
        String authorizationHeader = generateTokenRequestAuthorizationHeader(oAuth20);
        String requestBody = generateGrantRequestBody(oAuth20);

        String responseBody = retrieveTokenFromServer(accessTokenUri, authorizationHeader, requestBody);

        JsonNode responseJson;
        try {
            responseJson = OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new MechanismDriverException("Failed to read response as JSON: " + responseBody, e);
        }

        if (responseJson.has(ACCESS_TOKEN) && responseJson.has(EXPIRES_IN)) {
            accessToken = new Token(responseJson.get(ACCESS_TOKEN).asText(), responseJson.get(EXPIRES_IN).asInt());
            log.debug("Access token successfully refreshed.");
        } else {
            log.warn("Could not read access token data.");
        }

        if (responseJson.has(REFRESH_TOKEN) && responseJson.has(REFRESH_EXPIRES_IN)) {
            refreshToken = new Token(responseJson.get(REFRESH_TOKEN).asText(), responseJson.get(REFRESH_EXPIRES_IN).asInt());
            log.debug("Refresh token successfully refreshed.");
        } else {
            log.warn("Could not read refresh token data.");
        }
    }

    private String generateTokenRequestAuthorizationHeader(OAuth20 oAuth20) {
        String clientId = oAuth20.getClientId();

        if (clientId == null) {
            log.debug("Cannot generate Basic authorization header since the clientId is null");
            return null;
        }

        String clientSecret = oAuth20.getClientSecret() == null ? "" : oAuth20.getClientSecret();
        String clientCredentials = clientId + ":" + clientSecret;

        return String.format("Basic %s", encodeBase64(clientCredentials));
    }

    private String generateGrantRequestBody(OAuth20 oAuth20) {
        switch (oAuth20.getGrantType()) {
            case "password":
                return generatePasswordGrantRequestBody(oAuth20);
            case "client_credentials":
                return generateClientCredentialsGrantRequestBody(oAuth20);
            default:
                throw new UnsupportedOperationException("Unsupported grant type: " + oAuth20.getGrantType());
        }
    }

    private String generatePasswordGrantRequestBody(OAuth20 oAuth20) {
        return encodeUrlFormBody(
            entryOf(GRANT_TYPE, oAuth20.getGrantType()),
            entryOf("username", oAuth20.getUsername()),
            entryOf("password", oAuth20.getPassword()));
    }

    private String generateClientCredentialsGrantRequestBody(OAuth20 oAuth20) {
        List<Map.Entry<String, String>> variables = new ArrayList<>();
        variables.add(entryOf(GRANT_TYPE, oAuth20.getGrantType()));

        return encodeUrlFormBody(variables);
    }

    private Map.Entry<String, String> entryOf(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private void clearTokens() {
        accessToken = null;
        refreshToken = null;
    }

    // Cannot use a Map for parameters because duplicate keys are allowed
    @SafeVarargs
    @SuppressWarnings("varargs")
    private String encodeUrlFormBody(Map.Entry<String, String>... parameters) {
        return encodeUrlFormBody(Arrays.stream(parameters).collect(Collectors.toList()));
    }

    // Cannot use a Map for parameters because duplicate keys are allowed
    private String encodeUrlFormBody(List<Map.Entry<String, String>> parameters) {
        List<String> encodedVariables = new ArrayList<>(parameters.size());

        for (var parameter : parameters) {
            encodedVariables.add(String.format("%s=%s", parameter.getKey(), parameter.getValue()));
        }

        return String.join("&", encodedVariables);
    }
}

@Value
class Token {
    private String value;
    private Instant creationTime;
    private int validTimeInSeconds;

    public Token(String value, int validTimeInSeconds) {
        this.value = value;
        this.validTimeInSeconds = validTimeInSeconds;

        this.creationTime = Instant.now();
    }

    public boolean isValid() {
        Duration age = Duration.between(creationTime, Instant.now());
        return age.getSeconds() < validTimeInSeconds;
    }
}
