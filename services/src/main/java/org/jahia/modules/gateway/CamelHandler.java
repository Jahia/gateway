package org.jahia.modules.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.model.ProcessorDefinition;

/**
 * User: rincevent
 * Date: 09/11/11
 * Time: 9:09 AM
 */
public interface CamelHandler {
    @Handler
    void handleExchange(Exchange exchange);

    ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition);
}
