package org.jahia.modules.gateway.admin.forms;

import org.jahia.modules.gateway.GatewayService;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
public interface StartPointFormHandler {

    void parseForm(HttpServletRequest request, String startPointType, String startPointName, final GatewayService gatewayService) throws StartPointFormHandlerException;
}
