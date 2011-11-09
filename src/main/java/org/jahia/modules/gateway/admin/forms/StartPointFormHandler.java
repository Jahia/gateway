package org.jahia.modules.gateway.admin.forms;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
public interface StartPointFormHandler {

    String parseForm(HttpServletRequest request) throws StartPointFormHandlerException;
}
