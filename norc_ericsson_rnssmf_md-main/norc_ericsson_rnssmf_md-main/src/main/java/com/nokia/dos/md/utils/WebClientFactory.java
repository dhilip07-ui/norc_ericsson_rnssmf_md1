package com.nokia.dos.md.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;
import java.util.*;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpMethod;

import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.CertificateStoreType;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import static java.util.Collections.emptyMap;

@Slf4j
public class WebClientFactory {
    private static final String CONNECTION_PROVIDER_NAME = "connectionProviderName";
    private static final int MAX_CONN_TOTAL = 100;
    private static final int CONNECTION_TIMEOUT_MILLIS = 10000;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    static LoopResources daemonResource = null;

    private WebClientFactory() {
    }

    public static WebClient createSecureWebClient(boolean followRedirect, Set<CertificateStore> certificateStores) {
        try {
            return WebClientFactory.secureWebClient(false, followRedirect, certificateStores, READ_TIMEOUT_SECONDS, WRITE_TIMEOUT_SECONDS);
        } catch (SSLException ex) {
            throw new RuntimeException("Failed to instantiate WebClient", ex);
        }
    }

    public static WebClient createSecureWebClient(boolean followRedirect, Set<CertificateStore> certificateStores, int readTimeoutSeconds, int writeTimeoutSeconds) {
        try {
            return WebClientFactory.secureWebClient(false, followRedirect, certificateStores, readTimeoutSeconds, writeTimeoutSeconds);
        } catch (SSLException ex) {
            throw new RuntimeException("Failed to instantiate WebClient", ex);
        }
    }

    public static WebClient secureWebClient(boolean useDaemonThread, boolean followRedirect, Set<CertificateStore> certificateStores, int readTimeoutSeconds, int writeTimeoutSeconds) throws SSLException {
        SslContext sslContext = createSslContext(certificateStores);

        // Controls if web-client event loop is using daemon thread
        daemonResource = LoopResources.create("daemonResource", LoopResources.DEFAULT_IO_WORKER_COUNT, useDaemonThread);

        HttpClient httpClient = HttpClient
                .create(ConnectionProvider.create(CONNECTION_PROVIDER_NAME, MAX_CONN_TOTAL))
                // Set wiretap(true) for detailed logging of the HTTP requests/responses
                .wiretap(false)
                .followRedirect(followRedirect)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MILLIS)
                .runOn(daemonResource)
                .doOnConnected(
                        conn -> conn
                                .addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSeconds))
                )
               
                .secure(t -> t.sslContext(sslContext));

        ClientHttpConnector httpConnector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder().clientConnector(httpConnector).build();
    }

    @SuppressWarnings("squid:S134")
    private static SslContext createSslContext(Set<CertificateStore> certificateStores) throws SSLException {
        KeyStore trustStore = null;
        KeyStore keyStore = null;
        String keyStorePassword = null;
        PrivateKey privateKey = null;
        X509Certificate[] keyStoreX509CertificateChain = null;

        try {
            if (certificateStores != null) { //for http connection case cert =null
                HashMap<CertificateStoreType, CertificateStore> csMap = new HashMap<>();
                for (CertificateStore cs : certificateStores) {
                    if (!csMap.containsKey(cs.getCertificateStoreType())) { //only takes the first keystore and trust store found
                        csMap.put(cs.getCertificateStoreType(), cs);
                    }
                }

                if (csMap.containsKey(CertificateStoreType.TrustStore)) {
                    trustStore = loadKeyStore(csMap.get(CertificateStoreType.TrustStore));
                }

                if (csMap.containsKey(CertificateStoreType.KeyStore)) {
                    keyStore = loadKeyStore(csMap.get(CertificateStoreType.KeyStore));
                    keyStorePassword = csMap.get(CertificateStoreType.KeyStore).getPassword();
                    //creating certificate chain
                    Enumeration<?> en = keyStore.aliases();
                    while (en.hasMoreElements()) {
                        String keyAlias = (String) en.nextElement();
                        privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
                        Certificate[] certChain = keyStore.getCertificateChain(keyAlias);

                        keyStoreX509CertificateChain = Arrays.stream(certChain)
                                .map(certificate -> (X509Certificate) certificate).collect(Collectors.toList())
                                .toArray(new X509Certificate[certChain.length]);
                        break;
                    }
                }
            }
            return SslContextBuilder
                    .forClient()
                    .keyManager(privateKey, keyStorePassword, keyStoreX509CertificateChain)
                    .trustManager(new CustomiseTrustManager(trustStore))
                    .build();
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new SSLException("Error creating SslContext", e);
        }
    }

    private static class CustomiseTrustManager implements X509TrustManager {
        private X509TrustManager x509Tm;

        public CustomiseTrustManager(KeyStore truststore) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory tmf = null;
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);

            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    x509Tm = (X509TrustManager) tm;
                    break;
                }
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          
        	/* TODO If commented trust all -- not recommended in prod -please change and consider adding trustsore  !!!!  */
        	if (x509Tm == null)
                throw new CertificateException("Keystore could not be loaded");
            x509Tm.checkServerTrusted(arg0, arg1);

            for (X509Certificate i : arg0) {
                i.checkValidity();
            }
          
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private static KeyStore loadKeyStore(CertificateStore certificateStore) throws SSLException {
        try {
            String keyStorePassword = certificateStore.getPassword();
            KeyStore keyStore = KeyStore.getInstance(certificateStore.getCertificateStoreFormat().toString());
            try (InputStream inputStream = new ByteArrayInputStream(certificateStore.getFile())) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());
            }

            Key key = keyStore.getKey(keyStore.aliases().nextElement(), keyStorePassword.toCharArray());
            if ((key != null && certificateStore.getCertificateStoreType() == CertificateStoreType.TrustStore) ||
                    (key == null && certificateStore.getCertificateStoreType() == CertificateStoreType.KeyStore)) {
                throw new SSLException("Invalid " + certificateStore.getCertificateStoreType().toString() + " is provided for [" + keyStore.aliases().nextElement() + "]");
            }

            return keyStore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException |
                 UnrecoverableEntryException e) {
            log.error("Failed to load keystore: " + e.getMessage());
            throw new SSLException("Error loading keystore", e);
        }
    }

    public static WebClient.RequestHeadersSpec<?> generateRequestSkeleton(WebClient webClient, String httpOperation, String endpoint, String body) {
        switch (httpOperation.toUpperCase()) {
            case "GET":
                return webClient.get().uri(endpoint);
            case "POST":
                return webClient.post().uri(endpoint).bodyValue(body);
            case "DELETE":
                if (body.isEmpty()) {
                    return webClient.delete().uri(endpoint);
                }
                log.debug("A payload within a DELETE request message has no defined semantics; sending a payload body on a DELETE request might cause some existing implementations to reject the request. https://tools.ietf.org/html/rfc7231#section-4.3.5");
                return webClient.method(HttpMethod.DELETE).uri(endpoint).body(BodyInserters.fromValue(body));
            case "PATCH":
                return webClient.patch().uri(endpoint).bodyValue(body);
            case "PUT":
                return webClient.put().uri(endpoint).bodyValue(body);
            default:
                throw new UnsupportedOperationException("Unsupported operation: " + httpOperation);
        }
    }

    public static void setHeadersForRequest(WebClient.RequestHeadersSpec<?> request, Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.header(header.getKey(), header.getValue());
        }
    }

    public static void setCookiesForRequest(WebClient.RequestHeadersSpec<?> request, Map<String, String> cookies) {
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            request.cookie(cookie.getKey(), cookie.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> readHeaders(Map<String, Object> arguments) {
        if (arguments.containsKey("headers")) {
            return (Map<String, String>) arguments.get("headers");
        }

        return emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> readCookies(Map<String, Object> arguments) {
        if (arguments.containsKey("cookies")) {
            return (Map<String, String>) arguments.get("cookies");
        }

        return emptyMap();
    }

    public static String readBody(Map<String, Object> arguments) {
        if (arguments.containsKey("body")) {
            return (String) arguments.get("body");
        }
        return "";
    }

    public static LoopResources getDaemonResource() {
        return daemonResource;
    }
}
