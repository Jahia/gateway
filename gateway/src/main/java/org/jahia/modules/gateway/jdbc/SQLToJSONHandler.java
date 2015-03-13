/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
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
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
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
 */
package org.jahia.modules.gateway.jdbc;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.log4j.Logger;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.ConfigurableCamelHandler;
import org.jahia.modules.gateway.GatewayTransformerConfigurationException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author rincevent
 * Created : 11/14/11
 */
public class SQLToJSONHandler implements ConfigurableCamelHandler {
    private transient static Logger logger = Logger.getLogger(SQLToJSONHandler.class);

    public void configure(HttpServletRequest request) throws GatewayTransformerConfigurationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void handleExchange(Exchange exchange) {
        assert exchange.getIn().getBody() instanceof Map;
        Map<String, Object> datas = (Map<String, Object>) exchange.getIn().getBody();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nodetype", "jnt:sqlunstructuredContent");
            jsonObject.put("name", datas.get("name"));
            jsonObject.put("locale", "en");
            jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
            jsonObject.put("path", "/sites/systemsite/contents/sqls");
            Map<String, String> properties = new LinkedHashMap<String, String>();
            for (String key : datas.keySet()) {
                properties.put(key, datas.get(key).toString());
            }
            jsonObject.put("properties", properties);
            DefaultMessage defaultMessage = new DefaultMessage();
            defaultMessage.setBody(jsonObject.toString());
            exchange.setOut(defaultMessage);
            exchange.getOut().setHeaders(exchange.getIn().getHeaders());
            exchange.getOut().setHeader(SQLStartpoint.LATEST_ID,datas.get("id"));
        } catch (JSONException e) {
            logger.error(e.getMessage(),e);
        }
    }

    public ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition) {
        return processorDefinition.bean(this);
    }
}
