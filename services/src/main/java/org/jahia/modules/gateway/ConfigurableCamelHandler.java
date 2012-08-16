package org.jahia.modules.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;

import javax.servlet.http.HttpServletRequest;

/**
 * User: rincevent
 * Date: 09/11/11
 * Time: 9:08 AM
 */
public interface ConfigurableCamelHandler extends CamelHandler {
    void configure(HttpServletRequest request) throws GatewayTransformerConfigurationException;
}
