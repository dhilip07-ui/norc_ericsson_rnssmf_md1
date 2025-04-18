package com.nokia.dos.md.test;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nokia.dos.domainadaptation.mechanismdriver.AsyncOperationStatusResponse;
import com.nokia.dos.domainadaptation.mechanismdriver.OperationResponse;
import com.nokia.dos.domainadaptation.mechanismdriver.OperationStatus;
import com.nokia.dos.md.ENSSMFMechanismDriver;
import com.nokia.dos.md.ENSSMFMechanismDriverSession;
import com.nokia.dos.md.model.AsyncResponse;
import com.nokia.dos.md.model.SyncResponse;
import com.nokia.dos.md.utils.Operation;
import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.CertificateStoreFormat;
import com.nokia.fo.nero.CertificateStoreType;
import com.nokia.fo.nero.Connection;
import com.nokia.fo.nero.NeInterface;
import com.nokia.fo.nero.NeInterfaceAddress;
import com.nokia.fo.nero.OAuth20;

public class ENSSMFTest {
	
	  private static ENSSMFMechanismDriverSession mdSession;
		public static void main(String[] args) throws InterruptedException, JsonProcessingException{
		
	    	basicTest();
	    }

	    public static void basicTest() throws InterruptedException, JsonMappingException, JsonProcessingException {
	        // Authentication example for this test taken from
	        // https://en.wikipedia.org/wiki/Basic_access_authentication#Client_side
	    	
	    	

	    	Set<CertificateStore> css = new HashSet();
	    	

	 		   try {
	 	    		CertificateStore cs = new CertificateStore();    	
	 	    		String strDate = "2024-10-30T14:05:57+02:00";
	 	            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	 	            Date dateStr =  formatter.parse(strDate);        
	 		    	cs.setCertificateStoreType(CertificateStoreType.TrustStore);
	 		    	cs.setCertificateStoreFormat(CertificateStoreFormat.JKS);
	 		    	cs.setPassword("Sh`,MWJK");
	 		    	cs.setFile(Base64.getDecoder().decode("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"));
	 		    	cs.setFileRef("ccrc", "ccrc");
	 		    	cs.setName("ccrc");
	 		    	cs.setExpirationDate(dateStr);
	 		    	css.add(cs);
	 		   }catch (Exception e) {
	 			e.printStackTrace();
	 		}
	 		   
	    	ENSSMFMechanismDriver mechanismDriver = new ENSSMFMechanismDriver();
	    	NeInterfaceAddress targetNeiAddress = new NeInterfaceAddress();
	    	targetNeiAddress.setProtocol("https");
	    	targetNeiAddress.setHost("so-ran-poc.t-mobile.lab");
	    	targetNeiAddress.setHost("localhost");
	    	targetNeiAddress.setPort(19043);
	    	targetNeiAddress.setBaseUri("/nssmf-agent/v2");
	    	
	        ObjectMapper objectMapper = new ObjectMapper();
	    	NeInterface targetNei=new NeInterface();
	    	targetNei.setAddress(targetNeiAddress);
	    	targetNei.setCertificateStores(css);
	    	  Connection connection = Connection.builder()
	    	            .authentication(OAuth20.builder()
				    	//.port(443)
	    	            		.port(19043)
				    	.password("V3n-*A\\5qVs}ukf")
				    	.username("nokiauser")
				    	.tokenUrlPath("/auth/v1")
				    	//.host("https://so-ran-poc.t-mobile.lab")
				    	.host("https://localhost")
				    	.grantType("password")		    	
			    	    //.clientSecret("E2CE1930D81B5B748D57F37A321A9B4379CB89B4F121649CA170B0136E303E09")
			    	    .clientId("nokiauser")
			    	    .build()).build();
	    
	    	System.out.println("creating connection object");
	     
	        System.out.println("creating session");
	        connection.setToNeInterface(targetNei);
	        mdSession = (ENSSMFMechanismDriverSession) mechanismDriver.createSession(connection);


	        
	        System.out.println("preparing request parameters/arguments");
	        Map<String, Object> arguments = new HashMap<>();
	        arguments.put("httpOperation", "POST");
	     
	        arguments.put("headers", new HashMap<String, String>(){{ put("Content-Type", "application/json"); }});
	     

	        arguments.put("endpoint", "/ObjectManagement/NSS/SliceProfiles");
	        
	        arguments.put("body", "{\r\n" + 
	        		" \"attributeListIn\": { \r\n" + 
	        		" \"pLMNInfoList\": [ \r\n" + 
	        		" { \r\n" + 
	        		" \"plmnId\": { \r\n" + 
	        		" \"mcc\": 310, \r\n" + 
	        		" \"mnc\": \"310\"\r\n" + 
	        		" }, \r\n" + 
	        		" \"snssai\": { \r\n" + 
	        		" \"sst\": 1, \r\n" + 
	        		" \"sd\": \"10000\"\r\n" + 
	        		" } \r\n" + 
	        		" } \r\n" + 
	        		" ], \r\n" + 
	        		" \"RANSliceSubnetProfile\": { \r\n" + 
	        		" \"maxNumberofUEs\": 1, \r\n" + 
	        		" \"uLThptPerUE\": { \r\n" + 
	        		" \"guaThpt\": 10.0, \r\n" + 
	        		" \"maxThpt\": 4.0\r\n" + 
	        		" }, \r\n" + 
	        		" \"dLThptPerUE\": { \r\n" + 
	        		" \"guaThpt\": 10.0, \r\n" + 
	        		" \"maxThpt\": 60.0\r\n" + 
	        		" }, \r\n" + 
	        		" \"coverageAreaTAList\": [ \r\n" + 
	        		"1921134\r\n" + 
	        		" ] \r\n" + 
	        		" }, \r\n" + 
	        		" \"qos_profile_name\": \"QoS_TMO\", \r\n" + 
	        		" \"cellIds\" : [21], \r\n" + 
	        		" \"latency\": 1, \r\n" + 
	        		" \"resourceSharingLevel\": \"Shared\", \r\n" + 
	        		" \"__type\": \"ALLOCATE_RAN_NSSI\", \r\n" + 
	        		" \"versionNumber\": 9001, \r\n" + 
	        		" \"sliceName\": \"ENM_1_6000_RAN_NSSI\", \r\n" + 
	        		" \"sliceDescription\":\"ENM_1_6000_RAN_NSSI\"\r\n" + 
	        		" } \r\n" + 
	        		"}");
	     
	     
	        System.out.println("executeOperation");
	        OperationResponse basicTestResponse = mdSession.executeOperation(Operation.EXECUTE, arguments);
	        System.out.println("OperationResponse = "+basicTestResponse);
	        
	       Map<String, Object> response=objectMapper.readValue(objectMapper.writeValueAsString(basicTestResponse.getResponse()), Map.class);
	        
	        SyncResponse syncResponse = objectMapper.readValue(objectMapper.writeValueAsString(response.get("body")), SyncResponse.class);
	        System.out.println(syncResponse.getJobId());

	        
	        	
	        	
	        	
	        	
	        	
	        	arguments = new HashMap<>();
	        arguments.put("httpOperation", "GET");
	        arguments.put("taskId", syncResponse.getJobId());
	     
	        arguments.put("headers", new HashMap<String, String>(){{ put("Content-Type", "application/json"); }});
	     

	        arguments.put("endpoint", "/ObjectManagement/jobs/"+syncResponse.getJobId());
	        	
	        	
	        basicTestResponse = mdSession.executeOperation(Operation.ASYNC, arguments);
	        
	        System.out.println("OperationResponse = "+basicTestResponse);
	        	
	        
	        OperationStatus status = basicTestResponse.getOperationStatus();
	        AsyncOperationStatusResponse getResponse=null;
	        while (status.equals(OperationStatus.IN_PROGRESS)) {
	        	System.out.println("status = "+status);
	        	 Thread.sleep(10000);
	        	  getResponse= mdSession.getAsyncOperationStatus(basicTestResponse.getPollingInformation());
	        	
	        	 System.out.println("AsyncOperationStatusResponse = "+ ((AsyncResponse)getResponse.getResponse()).getStatus());
	        	 System.out.println("AsyncOperationStatusResponse = "+ ((AsyncResponse)getResponse.getResponse()).getNssiId());
	        	 status=getResponse.getOperationStatus();
	       
	    }
	        System.out.println("Final status = "+getResponse);
	        
	         
    }   

}
