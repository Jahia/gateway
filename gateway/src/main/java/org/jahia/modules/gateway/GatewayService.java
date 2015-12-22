/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.gateway;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.camel.CamelContext;
import org.jahia.services.content.JCRNodeWrapper;

/**
 * Content gateway service that handles Camel routes.
 * 
 * @author rincevent
 */
public interface GatewayService {

    void addRoute(String name, String routeStartPointKey, List<String> handlers, boolean createNodeForRoute);

    void addStartPoint(JCRNodeWrapper startPointNode) throws RepositoryException;

    /**
     * Get the {@link org.apache.camel.CamelContext}
     * 
     * @return camelContext the Camel context
     */
    CamelContext getCamelContext();

    Map<String, CamelHandler> getDeserializers();

    Map<String, String> getRoutes();

    Map<String, CamelStartPoint> getRouteStartPoints();

    Map<String, CamelHandler> getTransformers();

}