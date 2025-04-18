package com.nokia.dos.md.utils;

import com.nokia.dos.domainadaptation.mechanismdriver.OperationStatus;
import com.nokia.dos.domainadaptation.mechanismdriver.OperationType;
import com.nokia.dos.domainadaptation.mechanismdriver.TaskWeight;
import org.springframework.http.HttpStatus;

/**
 * Operations supported by mechanism driver.
 */
public class Operation {
    public static final String EXECUTE = "EXECUTE";
    public static final String ASYNC = "ASYNC";

    /**
     * Constructor made public so we can create an instance and test
     * all its fields for the different methods through reflection.
     */
    public Operation() {
    }

    public static TaskWeight getTaskWeight(String operationType) {
        switch (operationType.toUpperCase()) {
            case Operation.EXECUTE:
            default:
                throw new IllegalArgumentException("Failed to get task class for operation type: " + operationType);
        }
    }

    public static OperationType getOperationType(String operationType) {
        switch (operationType.toUpperCase()) {
            case Operation.EXECUTE:
            default:
                throw new IllegalArgumentException("Failed to get operation type for: " + operationType);
        }
    }

    public static OperationStatus getOperationStatus(boolean failOnError, HttpStatus httpStatus) {
        if (failOnError && httpStatus.isError()) {
            return OperationStatus.FAILED;
        }

        return OperationStatus.COMPLETED;
    }
}
