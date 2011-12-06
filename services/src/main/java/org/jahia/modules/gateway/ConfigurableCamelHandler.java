package org.jahia.modules.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 9:08 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ConfigurableCamelHandler extends CamelHandler {
    void configure(HttpServletRequest request) throws GatewayTransformerConfigurationException;
}
