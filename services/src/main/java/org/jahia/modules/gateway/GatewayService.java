/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2012 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.gateway;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.services.JahiaAfterInitializationService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * Content gateway service implementation that handles Camel routes.
 *  
 * @author rincevent
 * @since JAHIA 6.6
 */
public class GatewayService implements CamelContextAware, JahiaAfterInitializationService {
    private static Logger logger = LoggerFactory.getLogger(GatewayService.class);
    private CamelContext camelContext;
    private Map<String, CamelHandler> deserializers;
    private Map<String, CamelHandler> transformers;
    private Map<String, CamelStartPoint> routeStartPoints;
    private Map<String, String> routes;
    private CamelStartPointFactory camelStartPointFactory;
    private JCRTemplate template;

    /**
     * Injects the {@link org.apache.camel.CamelContext}
     *
     * @param camelContext the Camel context
     */
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    /**
     * Get the {@link org.apache.camel.CamelContext}
     *
     * @return camelContext the Camel context
     */
    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setDeserializers(Map<String, CamelHandler> deserializers) {
        this.deserializers = deserializers;
    }

    public void setTransformers(Map<String, CamelHandler> transformers) {
        this.transformers = transformers;
    }

    public void setRouteStartPoints(Map<String, CamelStartPoint> routeStartPoints) {
        this.routeStartPoints = routeStartPoints;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }

    public void initAfterAllServicesAreStarted() throws JahiaInitializationException {
        if (transformers == null) {
            transformers = new HashMap<String, CamelHandler>();
        }
        if (deserializers == null) {
            deserializers = new HashMap<String, CamelHandler>();
        }
        if (routeStartPoints == null) {
            routeStartPoints = new HashMap<String, CamelStartPoint>();
        }
        if (routes == null) {
            routes = new HashMap<String, String>();
        }
        // load start points and routes from jcr

        try {
            template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    try {
                        session.getNode("/gateway");
                        JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                        NodeIterator nodes = node.getNodes();
                        while (nodes.hasNext()) {
                            JCRNodeWrapper next = (JCRNodeWrapper) nodes.nextNode();
                            addStartPoint(next);
                        }
                        node = session.getNode("/gateway/routes");
                        nodes = node.getNodes();
                        while (nodes.hasNext()) {
                            JCRNodeWrapper next = (JCRNodeWrapper) nodes.nextNode();
                            routes.put(next.getName(), next.getProperty("route").getString());
                        }
                    } catch (PathNotFoundException e) {
                        logger.debug("Gateway not imported yet");
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            throw new JahiaInitializationException(e.getMessage(),e);
        }
        for (Map.Entry<String, String> entry : routes.entrySet()) {
            String[] strings = entry.getValue().split("->");
            if (strings.length > 1) {
                List<String> handlers = new ArrayList<String>();
                handlers.addAll(Arrays.asList(strings).subList(1, strings.length));
                addRouteInternal(entry.getKey(), strings[0], handlers);
            }
        }
    }

    public void addStartPoint(JCRNodeWrapper startPointNode) throws RepositoryException {
        try {
            routeStartPoints.put(startPointNode.getName(), camelStartPointFactory.createCamelStartPoint(startPointNode));
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        } catch (InstantiationException e) {
            logger.error(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void addRoute(final String name, final String routeStartPointKey, final List<String> handlers, boolean createNodeForRoute) {
        if (addRouteInternal(name, routeStartPointKey, handlers)) {
            final StringBuilder route = new StringBuilder(routeStartPointKey);
            for (String handler : handlers) {
                route.append("->").append(handler);
            }
            routes.put(name, route.toString());
            if (createNodeForRoute) {
                try {
                    template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            JCRNodeWrapper node = session.getNode("/gateway/routes");
                            JCRNodeWrapper jcrNodeWrapper = node.addNode(name, "jnt:gtwRoute");
                            jcrNodeWrapper.setProperty("route", route.toString());
                            session.save();
                            return null;
                        }
                    });
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private boolean addRouteInternal(final String name, final String routeStartPointKey, final List<String> handlers) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    ProcessorDefinition route = routeStartPoints.get(routeStartPointKey).startRoute(this);
                    for (String handler : handlers) {
                        if(transformers.containsKey(handler)) {
                            route = transformers.get(handler).appendToRoute(route);
                        } else if(deserializers.containsKey(handler)) {
                            route = deserializers.get(handler).appendToRoute(route);
                        }
                    }
                    route.setId(name);
                }
            });
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public Map<String, CamelStartPoint> getRouteStartPoints() {
        return routeStartPoints;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public Map<String, CamelHandler> getDeserializers() {
        return deserializers;
    }

    public Map<String, CamelHandler> getTransformers() {
        return transformers;
    }

    public void setTemplate(JCRTemplate template) {
        this.template = template;
    }

    public void setCamelStartPointFactory(CamelStartPointFactory camelStartPointFactory) {
        this.camelStartPointFactory = camelStartPointFactory;
    }
}
