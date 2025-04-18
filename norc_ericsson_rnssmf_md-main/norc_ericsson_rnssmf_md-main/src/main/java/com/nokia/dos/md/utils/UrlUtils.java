package com.nokia.dos.md.utils;

import static java.util.Objects.requireNonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

import com.nokia.fo.nero.NeInterfaceAddress;

@Slf4j
public class UrlUtils {

    private static String encode(String parameter, String encoding) {
        try {
            return URLEncoder.encode(parameter, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Failed to encode passed parameter: " + parameter, e);
        }
    }

    public static String encodeUtf8(String parameter) {
        return encode(parameter, "UTF-8");
    }

    public static String constructEndpoint(Map<String, Object> arguments, NeInterfaceAddress targetNeiAddress) {
        String endpointFromArguments = (String) arguments.get("endpoint");
        if (targetNeiAddress == null) {
            log.debug("Considering the endpoint from arguments as the full endpoint, since the target NE interface address is null");
            return endpointFromArguments;
        }

        log.debug("Constructing endpoint from address: {} and endpoint: {}", targetNeiAddress, endpointFromArguments);
        // Protocol and host are mandatory, everything else is optional
        String protocol = requireNonNull(targetNeiAddress.getProtocol(), "Failed to construct endpoint due to null protocol: " + targetNeiAddress);
        String host = requireNonNull(targetNeiAddress.getHost(), "Failed to construct endpoint due to null host: " + targetNeiAddress);
        String result = protocol + "://" + host;

        if (targetNeiAddress.getPort() != null) {
            result = result + ":" + targetNeiAddress.getPort();
        }
        if (StringUtils.isNotEmpty(targetNeiAddress.getBaseUri())) {
            result = result + "/" + targetNeiAddress.getBaseUri();
        }
        if (StringUtils.isNotEmpty(endpointFromArguments)) {
            boolean endsWithSlash = result.endsWith("/");
            if (endsWithSlash) {
                result = result + endpointFromArguments;
            } else {
                result = result + "/" + endpointFromArguments;
            }
        }

        log.debug("Constructed endpoint: {}", result);

        return result;
    }
}
