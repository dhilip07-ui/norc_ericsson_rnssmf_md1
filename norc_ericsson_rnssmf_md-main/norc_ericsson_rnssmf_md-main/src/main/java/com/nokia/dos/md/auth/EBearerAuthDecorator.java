package com.nokia.dos.md.auth;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.OAuth20;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EBearerAuthDecorator  implements AuthRequestDecorator {
	
	  WebClient webClientForAccessToken = null;
	  
	    private OAuth20 oAuth20;

	    private String accessToken;


	    public EBearerAuthDecorator(OAuth20 oAuth20, Set<CertificateStore> certificateStore, WebClient webClient) {
	        log.debug("Creating OAuth2Decorator with configuration: {}", oAuth20);

	        this.oAuth20 = oAuth20;
	        this.webClientForAccessToken = webClient;
	        
	        clearTokens();
	    }

	    @Override
	    public void enhanceRequest(WebClient.RequestHeadersSpec<?> request) {
	    	
	    	//--header 'Authorization: fd1f7ab2-a2f3-47fa-9f07-46f1e7a65ccf' --header 'Content-Type: application/json' --header 'Cookie: JSESSIONID=fd1f7ab2-a2f3-47fa-9f07-46f1e7a65ccf'
	        log.debug("OAuth 2.0 decorator got a request to enhance request {}", request);
	        
	        log.debug("Cookie JSESSIONID {}",  getAccessToken());
	        
	        request.header("Cookie", "JSESSIONID=" + getAccessToken());
	        request.header("Authorization", getAccessToken());
	        
	      //--header 'Authorization: fd1f7ab2-a2f3-47fa-9f07-46f1e7a65ccf' --header 'Content-Type: application/json' --header 'Cookie: JSESSIONID=fd1f7ab2-a2f3-47fa-9f07-46f1e7a65ccf'
	        log.debug("OAuth 2.0 decorator enhanced request {}", getAccessToken());
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
	        } 

	        return accessToken;
	    }
	  
	    private String generateAccessTokenUri() {
	        if (oAuth20.getPort() == null) {
	            return String.format("%s%s", oAuth20.getHost(), oAuth20.getTokenUrlPath());
	        } else {
	            return String.format("%s:%s%s", oAuth20.getHost(), oAuth20.getPort(), oAuth20.getTokenUrlPath());
	        }
	    }

	    private String retrieveTokenFromServer(String accessTokenUri, OAuth20 oAuth20) {
	       
	    	//    --header 'Conten-Type: application/json' --header 'X-Login: nokiauser' --header 'X-password: V3n-*A\5qVs}ukf'
	    	
	    	log.debug("AccessToken URL: {}, {}, {}", accessTokenUri, oAuth20.getUsername(), oAuth20.getPassword() );
	    	
	    	return webClientForAccessToken.post()
	            .uri(accessTokenUri)
	            .header("Content-Type", "application/json")
	            .header("X-Login", oAuth20.getUsername())
	            .header("X-password", oAuth20.getPassword())           
	            .exchange().block()
	            .bodyToMono(String.class).block();
	    }

  

	    private void refreshTokens() {
	        clearTokens();

	        log.debug("Refreshing tokens");
	        String accessTokenUri = generateAccessTokenUri();


	        accessToken  = retrieveTokenFromServer(accessTokenUri,  oAuth20);
	        
	        log.debug("Access token: {}", accessToken);

	      
	    }
  
	    private void clearTokens() {
	        accessToken = null;

	    }

	}

	

