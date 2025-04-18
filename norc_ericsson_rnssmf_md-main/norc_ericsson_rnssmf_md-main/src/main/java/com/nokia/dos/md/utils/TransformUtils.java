package com.nokia.dos.md.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j 
public class TransformUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * If the input is a valid JSON, it will be parsed and returned as a POJO.
     * Otherwise, it will be returned as-is.
     */
    public static Object parseAsJsonObjectOrString(String input) {
        if (input == null) {
            return null;
        }

        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(input);
            return OBJECT_MAPPER.convertValue(jsonNode, Object.class);
        } catch (Exception e) {
            log.error("Failed to parse as JSON or String: " + e);
            return input;
        }
    }

    /**
     * Extracts and returns the headers as strings to avoid (de-)serialization problems.
     */
    private static Map<String, List<String>> extractHeadersAsStrings(ClientResponse clientResponse) {
        if (clientResponse.headers() == null) {
            return null;
        }

        Map<String, List<String>> result = new HashMap<>();

        HttpHeaders headers = clientResponse.headers().asHttpHeaders();

        for (String header : headers.keySet()) {
            List<String> values = headers.get(header);
            result.put(header, values);
        }

        return result;
    }

    /**
     * Extracts and returns the cookies as strings to avoid (de-)serialization problems.
     */
    private static Map<String, List<String>> extractCookiesAsStrings(ClientResponse clientResponse) {
        if (clientResponse.cookies() == null) {
            return null;
        }

        Map<String, List<String>> result = new HashMap<>();

        MultiValueMap<String, ResponseCookie> cookies = clientResponse.cookies();

        for (String cookie : cookies.keySet()) {
            List<String> valuesAsStrings = cookies.get(cookie).stream()
                .map(ResponseCookie::toString)
                .collect(Collectors.toList());
            result.put(cookie, valuesAsStrings);
        }

        return result;
    }

    public static Map<String, Object> clientResponseToMap(ClientResponse clientResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("headers", extractHeadersAsStrings(clientResponse));
        response.put("cookies", extractCookiesAsStrings(clientResponse));
        response.put("httpStatusCode", clientResponse.statusCode() != null? clientResponse.statusCode().value() : 0);
        // Try to parse the body as a JSON
        response.put("body", parseAsJsonObjectOrString(clientResponse.bodyToMono(String.class).block()));

        return response;
    }

    public static Map<String, Object> monoClientResponseToMap(ClientResponse clientResponse) {
        Map<String, Object> response = new HashMap<>();
        response.put("headers", extractHeadersAsStrings(clientResponse));
        response.put("cookies", extractCookiesAsStrings(clientResponse));
        response.put("httpStatusCode", clientResponse.statusCode() != null? clientResponse.statusCode().value() : 0);
        response.put("statusCode", clientResponse.statusCode());

        return response;
    }
}
