package org.jahia.modules.gateway.admin.forms;

import java.io.UnsupportedEncodingException;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class StartPointFormHandlerException extends Exception {
    public StartPointFormHandlerException(String message) {
        super(message);
    }

    public StartPointFormHandlerException(Exception e) {
        super(e);
    }
}
