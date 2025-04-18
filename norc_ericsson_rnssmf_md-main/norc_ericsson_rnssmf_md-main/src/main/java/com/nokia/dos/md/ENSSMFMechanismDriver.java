package com.nokia.dos.md;

import java.util.Map;
import java.util.Set;

import com.nokia.dos.md.auth.AuthRequestDecorator;
import com.nokia.dos.md.utils.ArgumentsHandler;
import com.nokia.dos.md.utils.CommonUtils;
import com.nokia.dos.md.utils.Operation;
import com.nokia.dos.md.utils.WebClientFactory;
import static com.nokia.dos.md.auth.DecoratorSelector.selectDecorator;

import com.nokia.dos.domainadaptation.mechanismdriver.*;
import com.nokia.fo.nero.CertificateStore;
import com.nokia.fo.nero.Connection;
import com.nokia.fo.nero.NeInterfaceAddress;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Mechanism drivers (MD) are the components who implement the actual functionality to interact
 * with/manage target network elements (NEs). Generally, each MD is tailored to a specific NE.
 * MDs are loaded at runtime by the type driver (TD).
 */
@Slf4j
public class ENSSMFMechanismDriver implements com.nokia.dos.domainadaptation.mechanismdriver.MechanismDriver {

    /*
     * ID value of this mechanism driver.
     */
    private static final String ID = "Ericsson-NSSMF";

    /*
     * Return the id of this mechanism driver back to type driver.
     * When the type driver loads this mechanism driver, this id is returned as part of metadata and
     * shown in log message.
     * This method is deprecated.
     */
    @Override
    @SuppressWarnings("deprecation")
    public String getId() {
        return ID;
    }

    /*
     * Returns metadata of this mechanism driver to the type driver upon loading.
     * Example of the log message from type driver: ""message":"Loaded mechanism driver MechanismDriverRef
     * [id=Mechanism-Driver] (metadata: MechanismDriverMetadata(id=Mechanism-Driver, description=Description for this
     * mechanism driver, buildInformation=Build information for this mechanism driver, gitRevision=Git revision for this
     * mechanism driver, buildDate=null, otherProperties=null)"
     */
    @Override
    public MechanismDriverMetadata getMetadata() {
        return MechanismDriverMetadata.builder()
                .id(ID)
                .description("Ericsson NSSMF async")
                .buildInformation("Build information for this mechanism driver")
                .gitRevision("Git revision for this mechanism driver")
                .build();
    }

    /*
     * Creates a session from type driver to network element.
     * This method builds a connection session with the network element.
     * @param  connection a connection object to network element.
     * @return the connection session.
     */
    @Override
    public ENSSMFMechanismDriverSession createSession(Connection connection) {
        log.debug("Creating a session with connection: {}", connection);

        Set<CertificateStore> cs = CommonUtils.getCertificateStore(connection);

        WebClient webClient = connection.getToNeInterface() == null || connection.getToNeInterface().getRequestResponseTimeMax() == null?
                WebClientFactory.createSecureWebClient(true, cs) :
                WebClientFactory.createSecureWebClient(true, cs,
                        connection.getToNeInterface().getRequestResponseTimeMax(),
                        connection.getToNeInterface().getRequestResponseTimeMax());
        AuthRequestDecorator authRequestDecorator = selectDecorator(connection, webClient);

        NeInterfaceAddress targetNeiAddress = null;
        if (connection.getToNeInterface() != null && connection.getToNeInterface().getAddress() != null) {
            targetNeiAddress = connection.getToNeInterface().getAddress();
        }

        return ENSSMFMechanismDriverSession
                .builder()
                .webClient(webClient)
                .daemonResource(WebClientFactory.getDaemonResource())
                .authRequestDecorator(authRequestDecorator)
                .targetNeiAddress(targetNeiAddress)
                .build();
    }

    /*
     * Get the operation info from the type driver which is defined in the request.
     * Operation info contains task weight, operation type
     * @param operationType contains the task weight and operation type such as HEAVY, LIGHT, EXECUTE & etc.
     * @param arguments a list of arguments from the request that contains task type such as REGULAR, PHASE_1 & PHASE_2 from the request.
     * @return return operation info back to type driver.
     */
    @Override
 public OperationInfo getOperationInfo(String operationType, Map<String, Object> arguments) {
    	
    	if (operationType.equals(Operation.ASYNC)) {
    		
    		 return new OperationInfo(
    	                TaskWeight.HEAVY,
    	                OperationType.ASYNC_POLLING,
    	                TaskType.REGULAR);
    	}
    	
        return new OperationInfo(
                TaskWeight.LIGHT,
                OperationType.SYNC,
                TaskType.REGULAR);
    }
}
