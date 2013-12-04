/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.gatewaysamples.decoders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.Message;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.decoders.BaseMailDecoder;
import org.jahia.modules.gateway.mail.MailContent;
import org.jahia.services.usermanager.JahiaUser;
import org.json.JSONObject;

/**
 * Test mail decoder which parses the content of the incoming e-mail and stores it as a <code>jnt:mainContent</code> node in system site.
 * 
 * @author Sergiy Shyrkov
 */
public class MainContentMailDecoder extends BaseMailDecoder {

    @Override
    public String decode(Pattern matchingPattern, MailContent parsedMailContent, Message originalMessage)
            throws Exception {
        String title = originalMessage.getSubject();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodetype", "jnt:mainContent");
        jsonObject.put("name", StringUtils.defaultIfBlank(title, "Unknown"));
        jsonObject.put("locale", "en");
        jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
        jsonObject.put("path", "/sites/systemsite/contents");
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("body", parsedMailContent.getBody());
        properties.put("jcr:title", StringUtils.defaultIfBlank(title, "Unknown"));
        if (!parsedMailContent.getFiles().isEmpty()) {
            properties.put("image", parsedMailContent.getFiles().get(0).getFile().getAbsolutePath());
        }
        jsonObject.put("properties", properties);

        JahiaUser sender = getSender(originalMessage);
        if (sender != null) {
            properties.put("username", sender.getName());
        }

        return jsonObject.toString();
    }

}