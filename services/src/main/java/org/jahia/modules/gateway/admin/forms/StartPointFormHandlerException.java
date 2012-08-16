package org.jahia.modules.gateway.admin.forms;

import java.io.UnsupportedEncodingException;

/**
 * User: rincevent
 * Date: 09/11/11
 * Time: 11:33 AM
 */
public class StartPointFormHandlerException extends Exception {
    public StartPointFormHandlerException(String message) {
        super(message);
    }

    public StartPointFormHandlerException(Exception e) {
        super(e);
    }
}
