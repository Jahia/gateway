/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
