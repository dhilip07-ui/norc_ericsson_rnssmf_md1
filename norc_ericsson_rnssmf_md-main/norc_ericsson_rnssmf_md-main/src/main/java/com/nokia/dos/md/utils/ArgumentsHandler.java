package com.nokia.dos.md.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.nokia.dos.domainadaptation.mechanismdriver.TaskType;

import static java.util.Objects.requireNonNull;

/**
 * Helper class to work with the `Map<String, Object> arguments` parameter of
 * `executeOperation()`, which is then passed to other methods too.
 */
public class ArgumentsHandler {

    private static final String COMMANDS = "commands";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String MAX_RETRY = "maxRetry";
    private static final String WAIT_INTERVAL = "waitInterval";
    private static final String ENDPOINT_OS = "endpointOS";
    public static final String TASK_TYPE = "taskType";
    public static final String TASK_CORRELATION_ID = "correlationId";

    private ArgumentsHandler() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCommands(Map<String, Object> arguments) {
        Object commands = requireNonNull(arguments.get(COMMANDS));

        if (commands instanceof List) {
            return (List<String>) commands;
        } else {
            return Collections.singletonList(commands.toString());
        }
    }

    public static String getNewPassword(Map<String, Object> arguments) {
        Object newPassword = requireNonNull(arguments.get(NEW_PASSWORD));
        return newPassword.toString();
    }

    public static String getMaxRetry(Map<String, Object> arguments, String _default) {
        Object maxRetry = arguments.get(MAX_RETRY);
        if (maxRetry == null || maxRetry.toString().isEmpty()) {
            return _default;
        }
        return maxRetry.toString();
    }

    public static String getWaitInterval(Map<String, Object> arguments, String _default) {
        Object waitInterval = arguments.get(WAIT_INTERVAL);
        if (waitInterval == null || waitInterval.toString().isEmpty()) {
            return _default;
        }
        return waitInterval.toString();
    }

    public static String getEndpointOS(Map<String, Object> arguments, String _default) {
        Object endpointOS = arguments.get(ENDPOINT_OS);
        if (endpointOS == null || endpointOS.toString().isEmpty()) {
            return _default;
        }
        return endpointOS.toString();
    }
    
    public static TaskType getTaskType(Map<String, Object> arguments) {
        String taskType = (String) arguments.get(TASK_TYPE);
        switch (taskType != null ? taskType.toUpperCase() : "") {
            case "PHASE_1":
                return TaskType.PHASE_1;
            case "PHASE_2":
                return TaskType.PHASE_2;
            default:
                return TaskType.REGULAR;
        }
    }

    public static String getTaskCorrelationId(Map<String, Object> arguments) {
        return (String) arguments.get(TASK_CORRELATION_ID);
    }
}
