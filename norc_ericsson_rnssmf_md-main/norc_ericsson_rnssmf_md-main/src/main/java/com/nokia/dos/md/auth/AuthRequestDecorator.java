package com.nokia.dos.md.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

public interface AuthRequestDecorator {
    void enhanceRequest(WebClient.RequestHeadersSpec<?> request);

    /**
     * Given the response for some operation, this method specifies whether the
     * operation execution should be immediately retried after some corrective
     * action is applied to the way we enhance the original request.
     *
     * An example use case of this is if OAuth2 authentication fails due to the
     * current access token being revoked by the server. In this case, the server
     * will return a 401 Unauthorized for our operation. We want the mechanism
     * driver to be able to handle this case in a transparent way.
     * Hence, the implementing request decorator must override this method
     * accordingly to flag that response accordingly. If a call to this method
     * returns `true`, a corrective action (supplied by overriding the
     * `applyCorrectiveAction()` method) will be executed, and then, the
     * request will be enhanced and executed a second time. Note that for this
     * use case and if the retry succeeds, we do not want the user to know of
     * this happening behind the scenes. However, if the retry also fails,
     * the user will find out about it by getting a probably erroneous or
     * unexpected response.
     *
     * Note that this method should only return true if the problem is likely
     * to be fixed by the request decorator alone, such as that of the OAuth2
     * example. Now consider the following scenario: The user sends GET request
     * and the server returns a 404 - not found. The problem is not inherent to
     * the request decorator, nor it should be expected that the request
     * decorator can fix it. Hence, this should not be contemplated and no
     * automatic retry should be re-triggered for that case.
     *
     * @return Whether the operation should be retried after executing some corrective action to the request decorator
     */
    default boolean operationFailedAndShouldBeAutomaticallyRetried(HttpStatus httpStatus) {
        return false;
    }

    /**
     * If an operation is to be automatically retried, this method will define
     * which action should the request decorator perform before retrying again.
     *
     * For example, consider an OAuth2 access token being revoked by the server.
     * If this is the case, the access token must be refreshed and the request
     * executed again, since this is something expected and that can be possibly
     * fixed by the request decorator without the need to notify the user about it.
     */
    default void applyCorrectiveAction() { }
}
