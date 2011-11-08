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

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/7/11
 */
public class GatewayService implements CamelContextAware {
    private transient static Logger logger = Logger.getLogger(GatewayService.class);
    private CamelContext camelContext;
    private JSONToJCRDeserializer deserializer;
    private MailToJSON mailToJSON;
    /**
     * Injects the {@link org.apache.camel.CamelContext}
     *
     * @param camelContext the Camel context
     */
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
        try {
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("imaps://imap.gmail.com?username=cedric.mailleux@gmail.com&password=aillas&consumer.delay=60000").bean(mailToJSON).bean(deserializer);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Get the {@link org.apache.camel.CamelContext}
     *
     * @return camelContext the Camel context
     */
    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setDeserializer(JSONToJCRDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    public void setMailToJSON(MailToJSON mailToJSON) {
        this.mailToJSON = mailToJSON;
    }
}
