package com.nokia.dos.md.auth;

import org.springframework.web.reactive.function.client.WebClient;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * No operation decorator (i.e.: do nothing).
 */
@NoArgsConstructor
@Slf4j
public class NoOpDecorator implements AuthRequestDecorator {
    @Override
    public void enhanceRequest(WebClient.RequestHeadersSpec<?> request) {
        log.debug("NO-OP decorator got a request to enhance request {}", request);
    }
}
