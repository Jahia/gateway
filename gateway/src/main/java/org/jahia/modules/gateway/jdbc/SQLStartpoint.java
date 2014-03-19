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
package org.jahia.modules.gateway.jdbc;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.log4j.Logger;
import org.jahia.modules.gateway.CamelStartPoint;
import org.jahia.modules.gateway.Constants;
import org.jahia.modules.gateway.GatewayService;
import org.jahia.modules.gateway.admin.forms.StartPointFormHandler;
import org.jahia.modules.gateway.admin.forms.StartPointFormHandlerException;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;

import static org.apache.camel.builder.Builder.body;
import static org.apache.camel.builder.Builder.constant;

/**
 * @author rincevent
 * Created : 11/14/11
 */
public class SQLStartpoint implements StartPointFormHandler, CamelStartPoint {
    private transient static Logger logger = Logger.getLogger(SQLStartpoint.class);
    private JCRTemplate template;
    private String sql;
    private String datasource;
    private Long frequency;
    private boolean update = false;
    public static final String LATEST_ID = "org.jahia.modules.gateway.header.latest.id";
    private Object latestID;



    public void setTemplate(JCRTemplate template) {
        this.template = template;
    }

    public void initStartPoint(JCRNodeWrapper node) throws RepositoryException {
        frequency = node.getProperty("frequency").getValue().getLong();
        sql = node.getProperty("sql").getValue().getString();
        datasource = node.getProperty("datasource").getValue().getString();
        update = node.getProperty("update").getValue().getBoolean();
    }

    public ProcessorDefinition startRoute(RouteBuilder routeBuilder) {
        return routeBuilder.from("timer://gtwTimer" + (frequency != null ? "?period=" + frequency + "s" : "")).bean(
                this, "getLatestID").to("sql:" + sql + "?dataSourceRef=" + datasource).split(body()).bean(this,
                "setLatestID");
        /*return routeBuilder.from("timer://gtwTimer" + (frequency != null ? "?period=" + frequency + "s" : "")).
                setBody(constant(sql)).to("jdbc:" + datasource).split(body());*/
    }

    public void parseForm(HttpServletRequest request, String startPointType, final String startPointName,
                          GatewayService gatewayService) throws StartPointFormHandlerException {
        frequency = Long.valueOf(request.getParameter("frequency"));
        sql = request.getParameter("sql");
        datasource = request.getParameter("datasource");
        String update1 = request.getParameter("update");
        if(update1!=null) {
            update = true;
        }
        try {
            template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                    JCRNodeWrapper jcrNodeWrapper = node.addNode(startPointName, "jnt:sqlstartpoint");
                    jcrNodeWrapper.setProperty("frequency", frequency);
                    jcrNodeWrapper.setProperty("sql", sql);
                    jcrNodeWrapper.setProperty("datasource", datasource);
                    jcrNodeWrapper.setProperty("update", update);
                    session.save();
                    return null;
                }
            });
            gatewayService.getRouteStartPoints().put(startPointName, this);
        } catch (RepositoryException e) {
            logger.error("Error adding JCR node", e);
        }
    }

    public void getLatestID(Exchange exchange) {
        DefaultMessage in = new DefaultMessage();
        in.setBody(Arrays.asList(latestID));
        exchange.setOut(in);
    }

    public void setLatestID(Exchange exchange) {
        latestID = exchange.getIn().getHeader(LATEST_ID);
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setHeader(Constants.UPDATE_ONLY, update);
        exchange.getOut().setBody(exchange.getIn().getBody());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{datasource='").append(datasource).append('\'');
        sb.append(", sql='").append(sql).append('\'');
        sb.append(", frequency=").append(frequency);
        sb.append(", update=").append(update);
        sb.append('}');
        return sb.toString();
    }
}
