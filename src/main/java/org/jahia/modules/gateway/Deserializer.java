package org.jahia.modules.gateway;

import org.apache.camel.Handler;

/**
 * Created by IntelliJ IDEA.
 * User: rincevent
 * Date: 09/11/11
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Deserializer {
    @Handler
    void deserialize(String body);
}
