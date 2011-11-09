/**
 *
 * This file is part of Jahia: An integrated WCM, DMS and Portal Solution
 * Copyright (C) 2002-2009 Jahia Limited. All rights reserved.
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
 * in Jahia's FLOSS exception. You should have recieved a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license"
 *
 * Commercial and Supported Versions of the program
 * Alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms contained in a separate written agreement
 * between you and Jahia Limited. If you are unsure which license is appropriate
 * for your use, please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.gateway;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanEndpoint;
import org.apache.log4j.Logger;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.6
 *        Created : 11/7/11
 */
public class GatewayService implements CamelContextAware, InitializingBean {
    private transient static Logger logger = Logger.getLogger(GatewayService.class);
    private CamelContext camelContext;
    private Map<String, Deserializer> deserializers;
    private Map<String, Transformer> transformers;
    private Map<String, String> routeStartPoints;
    private Map<String, String> routes;
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

    public void setDeserializers(Map<String, Deserializer> deserializers) {
        this.deserializers = deserializers;
    }

    public void setTransformers(Map<String, Transformer> transformers) {
        this.transformers = transformers;
    }

    public void setRouteStartPoints(Map<String, String> routeStartPoints) {
        this.routeStartPoints = routeStartPoints;
    }

    public void setRoutes(Map<String, String> routes) {
        this.routes = routes;
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        if (transformers == null) {
            transformers = new HashMap<String, Transformer>();
        }
        if (deserializers == null) {
            deserializers = new HashMap<String, Deserializer>();
        }
        if (routeStartPoints == null) {
            routeStartPoints = new HashMap<String, String>();
        }
        if (routes == null) {
            routes = new HashMap<String, String>();
        }
        // load start points and routes from jcr

        template.doExecuteWithSystemSession(new JCRCallback<Object>() {
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                try {
                    session.getNode("/gateway");
                    JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                    NodeIterator nodes = node.getNodes();
                    while (nodes.hasNext()) {
                        JCRNodeWrapper next = (JCRNodeWrapper) nodes.nextNode();
                        routeStartPoints.put(next.getName(), next.getProperty("uri").getString());
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
        for (Map.Entry<String, String> entry : routes.entrySet()) {
            String[] strings = entry.getValue().split("->");
            if (strings.length == 3) {
                addRoute(strings[0], strings[1], strings[2]);
            }
        }
    }

    public void addRoute(final String name, final String routeStartPointKey, final String transformerKey, final String deserializerKey) {
        if (addRoute(routeStartPointKey, transformerKey, deserializerKey)) {
            final String route = routeStartPointKey + "->" + transformerKey + "->" + deserializerKey;
            routes.put(name, route);
            try {
                template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        JCRNodeWrapper node = session.getNode("/gateway/routes");
                        JCRNodeWrapper jcrNodeWrapper = node.addNode(name, "jnt:gtwRoute");
                        jcrNodeWrapper.setProperty("route", route);
                        session.save();
                        return null;
                    }
                });
            } catch (RepositoryException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private boolean addRoute(final String routeStartPointKey, final String transformerKey, final String deserializerKey) {
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(routeStartPoints.get(routeStartPointKey)).bean(transformers.get(transformerKey)).bean(deserializers.get(deserializerKey));

                }
            });
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public void addRouteStartPoint(final String name, final String route) {
        routeStartPoints.put(name, route);
        try {
            template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                    JCRNodeWrapper jcrNodeWrapper = node.addNode(name, "jnt:gtwStartPoint");
                    jcrNodeWrapper.setProperty("uri", route);
                    session.save();
                    return null;
                }
            });
        } catch (RepositoryException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Map<String, String> getRouteStartPoints() {
        return routeStartPoints;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public Map<String, Deserializer> getDeserializers() {
        return deserializers;
    }

    public Map<String, Transformer> getTransformers() {
        return transformers;
    }

    public void setTemplate(JCRTemplate template) {
        this.template = template;
    }
}
