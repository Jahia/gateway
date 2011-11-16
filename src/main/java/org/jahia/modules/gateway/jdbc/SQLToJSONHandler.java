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
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/14/11
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
