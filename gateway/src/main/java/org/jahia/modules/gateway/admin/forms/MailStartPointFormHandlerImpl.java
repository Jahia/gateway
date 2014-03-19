/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.gateway.admin.forms;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.gateway.CamelStartPoint;
import org.jahia.modules.gateway.GatewayService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for the {@link CamelStartPoint} and form handler for an IMAP/POP3 mail box.
 * 
 * @author Cédric Mailleux
 * @author Sergiy Shyrkov
 */
public class MailStartPointFormHandlerImpl implements StartPointFormHandler, CamelStartPoint {

    private static final Logger logger = LoggerFactory
            .getLogger(MailStartPointFormHandlerImpl.class);

    private static final String URI_TEMPLATE = "${protocol}://${host}${port}?username=${username}&password=${password}&consumer.delay=${delay}&delete=${delete}";

    public static String buildUri(Map<String, String> parameters) {
        String protocol = StringUtils.defaultIfEmpty(parameters.get("protocol"), "imap");
        String host = StringUtils.defaultIfEmpty(parameters.get("host"), "imap.gmail.com");
        if ("imap.gmail.com".equals(host)) {
            protocol = "imaps";
        }
        String port = StringUtils.defaultIfEmpty(parameters.get("port"), "");
        if (port.length() > 0) {
            port = ":"+port;
        }
        String username = StringUtils.defaultString(parameters.get("username"));
        String delay = StringUtils.defaultIfEmpty(parameters.get("delay"), "60000");
        String delete = StringUtils.defaultString(parameters.get("delete"), "false");

        return StringUtils.replaceEachRepeatedly(URI_TEMPLATE, new String[] { "${protocol}",
                "${host}", "${port}", "${username}", "${delay}", "${delete}" }, new String[] {
                protocol, host, port, username, delay, delete });
    }

    public static String getInterpolatedUri(String uri, String clearTextPassword) {
        return uri.contains("${password}") ? StringUtils.replace(uri, "${password}",
                clearTextPassword) : uri;
    }

    private String password;

    private String uri;

    public void initStartPoint(JCRNodeWrapper node) throws RepositoryException {
        uri = node.getProperty("uri").getValue().getString();
        password = node.hasProperty("password") ? node.getProperty("password").getString() : "";
        password = StringUtils.isNotEmpty(password) ? EncryptionUtils.passwordBaseDecrypt(password)
                : "";
    }

    public void parseForm(HttpServletRequest request, String startPointType,
            final String startPointName, final GatewayService gatewayService)
            throws StartPointFormHandlerException {

        Map<String, String> params = new HashMap<String, String>();
        params.put("protocol", request.getParameter("protocol"));
        params.put("host", request.getParameter("host"));
        params.put("port", request.getParameter("port"));
        params.put("username", request.getParameter("username"));
        params.put("delay", request.getParameter("delay"));
        params.put("delete", request.getParameter("delete"));

        password = StringUtils.defaultString(request.getParameter("password"));
        uri = buildUri(params);

        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                    JCRNodeWrapper jcrNodeWrapper = node.addNode(startPointName,
                            "jnt:mailStartPoint");
                    jcrNodeWrapper.setProperty("uri", uri);
                    jcrNodeWrapper.setProperty(
                            "password",
                            (StringUtils.isNotEmpty(password) ? EncryptionUtils
                                    .passwordBaseEncrypt(password) : ""));
                    session.save();
                    return null;
                }
            });

            logger.info("Registering route start point: {}", uri);

            gatewayService.getRouteStartPoints().put(startPointName, this);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public ProcessorDefinition startRoute(RouteBuilder routeBuilder) {
        return routeBuilder.from(getInterpolatedUri(uri, password));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{uri='").append(uri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
