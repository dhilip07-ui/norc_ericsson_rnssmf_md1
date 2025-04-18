package com.nokia.dos.md;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.nokia.dos.md.auth.AuthRequestDecorator;
import com.nokia.dos.md.auth.NoOpDecorator;
import com.nokia.dos.md.model.AsyncResponse;
import com.nokia.dos.md.model.ENSSMFOrderStatus;
import com.nokia.dos.md.polling.PollingInformation;
import com.nokia.dos.md.utils.Operation;
import com.nokia.dos.md.utils.TransformUtils;
import com.nokia.dos.md.utils.UrlUtils;
import static com.nokia.dos.md.utils.Operation.getOperationStatus;
import static com.nokia.dos.md.utils.WebClientFactory.*;
import com.nokia.fo.nero.NeInterfaceAddress;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nokia.dos.domainadaptation.mechanismdriver.AsyncOperationStatusResponse;
import com.nokia.dos.domainadaptation.mechanismdriver.MechanismDriverException;
import com.nokia.dos.domainadaptation.mechanismdriver.OperationResponse;
import com.nokia.dos.domainadaptation.mechanismdriver.OperationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.LoopResources;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/*
 * The class that creates the operation session to the network element based on the received operation info from the request.
 */
@Slf4j
@Builder
public class ENSSMFMechanismDriverSession implements com.nokia.dos.domainadaptation.mechanismdriver.MechanismDriverSession {
    /*
     * Initiate as background task
     */
    private LoopResources daemonResource;
    /*
     * Initiate as non-polling operation.
     */
    private static final String NO_POLLING_INFORMATION = null;
    /*
     * Whether non 2XX responses should make the response status FAILED
     */
    @Builder.Default
    private boolean failOnErrorDefault = true;
    /*
     * Define the authentication mode.
     */
    @Builder.Default
    private AuthRequestDecorator authRequestDecorator = new NoOpDecorator();
    /*
     * A web client object
     */
    @NotNull
    private WebClient webClient;
    /*
     * A network element interface address
     */
    private NeInterfaceAddress targetNeiAddress;
    
    @Builder.Default
	@NonNull
	private final ObjectMapper objectMapper = new ObjectMapper();
    
    private String taskId;

    /*
     * Received the operation type from the mechanism driver class and return the operation response based
     * on the operation type such as EXECUTE & etc.
     * @param operationType such as EXECUTE & etc.
     * @param arguments a list of arguments from the task request.
     */
    @Override
    public OperationResponse executeOperation(String operationType, Map<String, Object> arguments) {
        switch (operationType) {
            case Operation.EXECUTE:
                return execute(operationType,arguments);
            case Operation.ASYNC:
                return execute(operationType,arguments);
            default:
                throw new UnsupportedOperationException("Unknown operation: " + operationType);
        }
    }

    private OperationResponse execute(String operationType, Map<String, Object> arguments) {
        String httpOperation = (String) arguments.get("httpOperation");
        if (arguments.containsKey("taskId")) {
            taskId = (String) arguments.get("taskId");
        }
        String endpoint = UrlUtils.constructEndpoint(arguments, targetNeiAddress);
        Map<String, String> headers = readHeaders(arguments);
        Map<String, String> cookies = readCookies(arguments);
        String body = readBody(arguments);
        boolean failOnError = (boolean) arguments.getOrDefault("failOnError", failOnErrorDefault);

        return execute(operationType, httpOperation, endpoint, headers, cookies, body, failOnError);
    }

    private OperationResponse execute(
    		String operationType,
            String operation,
            String endpoint,
            Map<String, String> headers,
            Map<String, String> cookies,
            String body,
            boolean failOnError) {
        log.debug(
                "Got a request to operation EXECUTE: {} against {} with headers: {}, cookies: {}, body: {}, failOnError: {}",
                operation, endpoint, headers, cookies.isEmpty() ? cookies : "<HIDDEN>", body, failOnError);
try {  
            final Map<String, Object> response = getClientResponse( operation, endpoint, body, headers, cookies);
            OperationStatus operationStatus = getOperationStatus(failOnError, HttpStatus.valueOf((int)response.get("httpStatusCode")));
            log.debug("Response for {} request: {}", operation, response);
        if (operationType.equals(Operation.EXECUTE)) {
            return new OperationResponse(operationType, operationStatus, NO_POLLING_INFORMATION, response);
            
        }else {
    
        	 Map<String, Object> responseMap=objectMapper.readValue(objectMapper.writeValueAsString(response), Map.class);
 	        
 	        AsyncResponse asyncResponse = objectMapper.readValue(objectMapper.writeValueAsString(responseMap.get("body")), AsyncResponse.class);
 	        

        	String pollingInformation = serializePollingInformationToString(operationType.toUpperCase(), taskId, asyncResponse.getStatus());
			OperationStatus tmfoperationStatus = asyncResponse.getStatus().equals(ENSSMFOrderStatus.IN_PROGRESS)
					? OperationStatus.IN_PROGRESS
					: OperationStatus.FAILED;
			 tmfoperationStatus = asyncResponse.getStatus().equals(ENSSMFOrderStatus.COMPLETED)
					? OperationStatus.COMPLETED
					: tmfoperationStatus;
			
			return new OperationResponse(operationType.toUpperCase(), tmfoperationStatus , pollingInformation,
					asyncResponse);
        }

        } catch(Exception e) {
            log.error(e.getMessage());
            //any exception need handle in MD, so that OH UI able to display proper response message
            return new OperationResponse(Operation.EXECUTE + "_" + operation, OperationStatus.FAILED, NO_POLLING_INFORMATION, "Mechanism Driver Exception: "+e.getMessage());
        }
    }
    
    
    
    
    @Override
  	public AsyncOperationStatusResponse getAsyncOperationStatus(String serializedPollingInformation) {
  		log.debug("Got an operation request with polling information: {}", serializedPollingInformation);
  		PollingInformation pollingInformation;
  		try {
  			pollingInformation = objectMapper.readValue(serializedPollingInformation, PollingInformation.class);
  		} catch (IOException e) {
  			throw new MechanismDriverException("Failed to deserialize: " + serializedPollingInformation, e);
  		}

  		String pollingOperation = pollingInformation.getOperation().toUpperCase();
  		
  		try {
  			AsyncOperationStatusResponse resposne = getOperationStatusResponse(pollingOperation, pollingInformation);
  			return resposne;
  			
  		}catch (Exception e) {
  			e.printStackTrace();
  			throw new MechanismDriverException("Failed to poll: " +  e.getMessage());
  		}

  	
  		
  	}
      
      private AsyncOperationStatusResponse getOperationStatusResponse(String pollingOperation,
  			PollingInformation pollingInformation) throws JsonMappingException, JsonProcessingException {
      	Map<String, Object> arguments = new HashMap<String, Object>();
      	arguments.put("endpoint", "/ObjectManagement/jobs/"+pollingInformation.getOrderId());
      	arguments.put("headers", new HashMap<String, String>(){{ put("content_type", "application/json"); }}); 
      	
      	String endpoint = UrlUtils.constructEndpoint(arguments, targetNeiAddress);
      	   Map<String, String> headers = readHeaders(arguments);
             Map<String, String> cookies = readCookies(arguments);
      	 final Map<String, Object> response = getClientResponse( "GET", endpoint, null, headers, cookies);
           OperationStatus operationStatus = getOperationStatus(true, HttpStatus.valueOf((int)response.get("httpStatusCode")));
           log.debug("Response for {} request: {}", "GET", response);      
           if (operationStatus.equals(OperationStatus.FAILED)) {
           	
           	throw new MechanismDriverException("Service returned HTTP"+  String.valueOf(response.get("httpStatusCode")));
           }
      
           Map<String, Object> responseMap=objectMapper.readValue(objectMapper.writeValueAsString(response), Map.class);
	        
	        AsyncResponse asyncResponse = objectMapper.readValue(objectMapper.writeValueAsString(responseMap.get("body")), AsyncResponse.class);
	        
  	    OperationStatus status;
  		switch ( asyncResponse.getStatus()) {
  		case ENSSMFOrderStatus.IN_PROGRESS:
  			status = OperationStatus.IN_PROGRESS;
  			break;
  		case ENSSMFOrderStatus.COMPLETED:
  			status = OperationStatus.COMPLETED;
  			break;
  		case ENSSMFOrderStatus.ERRORED:
  			status = OperationStatus.FAILED;
  			break;
  		case ENSSMFOrderStatus.SUSPENDED:
  			status = OperationStatus.FAILED;
  			break;
  		default:
  			throw new MechanismDriverException(
  					"Unknown workingState " + asyncResponse.getStatus() + " for polling request " + pollingInformation);
  		}
  		return new AsyncOperationStatusResponse(pollingOperation, status, asyncResponse,
  				pollingInformation.getOrderId());
  		

  	}
    
      private String serializePollingInformationToString(String operationType, String orderId, String status) {
  		return serializeToString(new PollingInformation(operationType, orderId, status));
  	}
      
      private String serializeToString(Object content) {
  		try {
  			return objectMapper.writeValueAsString(content);
  		} catch (JsonProcessingException e) {
  			throw new MechanismDriverException("Failed to serialize to string: " + content, e);
  		}
  	}
      
      private Map<String, Object> getClientResponse( String operation, String endpoint, String body, Map<String, String> headers,  Map<String, String> cookies ) {
    	    
      	
     	 WebClient.RequestHeadersSpec<?> request = generateRequestSkeleton(webClient, operation, endpoint, body);      
              setHeadersForRequest(request, headers);
              setCookiesForRequest(request, cookies);
              authRequestDecorator.enhanceRequest(request);

              final Map<String, Object> response = new HashMap<String, Object>();
              
              log.debug("Sending request {}",request );
              
              String stringBody = request.exchangeToMono(rs -> {
                  response.putAll(TransformUtils.monoClientResponseToMap(rs));
                  return rs.bodyToMono(String.class);
              }).block();
              response.put("body", TransformUtils.parseAsJsonObjectOrString(stringBody));
              if (authRequestDecorator.operationFailedAndShouldBeAutomaticallyRetried((HttpStatus) response.get("statusCode"))) {
                  authRequestDecorator.applyCorrectiveAction();

                  // Create a new request and enhance it again
                  request = generateRequestSkeleton(webClient, operation, endpoint, body);
                  setHeadersForRequest(request, headers);
                  setCookiesForRequest(request, cookies);
                  authRequestDecorator.enhanceRequest(request);

                  response.clear();
                   stringBody = request.exchangeToMono(rs -> {
                      response.putAll(TransformUtils.monoClientResponseToMap(rs));
                      return rs.bodyToMono(String.class);
                  }).block();
                  response.put("body", TransformUtils.parseAsJsonObjectOrString(stringBody));
              }

              return response;
     	
     }

    /*
     * Upon connection session is ended, dispose this background task.
     */
    @Override
    public void close() {
        if (daemonResource != null) {
            daemonResource.dispose();
        }
    }
}
