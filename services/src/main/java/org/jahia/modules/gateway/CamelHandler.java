package org.jahia.modules.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.model.ProcessorDefinition;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface CamelHandler {
    @Handler
    void handleExchange(Exchange exchange);

    ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition);
}
