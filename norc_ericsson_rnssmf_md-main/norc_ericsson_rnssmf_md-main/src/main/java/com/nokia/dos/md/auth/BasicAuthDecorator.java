package com.nokia.dos.md.auth;

import static com.nokia.dos.md.utils.Base64Utils.encodeBase64;

import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class BasicAuthDecorator implements AuthRequestDecorator {

    private String username;
    private String password;

    @Override
    public void enhanceRequest(WebClient.RequestHeadersSpec<?> request) {
        log.debug("BasicAuth decorator got a request to enhance request {}", request);
        String encodedCredentials = encodeBase64(username + ":" + password);
        request.header("Authorization", "Basic " + encodedCredentials);
    }
}
