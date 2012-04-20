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
package org.jahia.modules.gateway.decoders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Message;

import org.apache.commons.lang.time.DateFormatUtils;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.mail.MailContent;
import org.jahia.services.usermanager.JahiaUser;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mail decoder that handles unmatched messages.
 * 
 * @author Cédric Mailleux
 * @author Sergiy Shyrkov
 */
public class DefaultMailDecoder extends BaseMailDecoder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMailDecoder.class);

    public String decode(Pattern matchingPattern, MailContent parsedMailContent, Message originalMessage) throws Exception {
        String subject = originalMessage.getSubject();
        JahiaUser sender = getSender(originalMessage);
        if (sender == null) {
            Address[] from = originalMessage.getFrom();
            logger.warn(
                    "Unable to find the Jahia user with the e-mail corresponding to the sender of the e-mail message: {}",
                    (from != null && from.length > 0 ? from[0] : null));
            return null;
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("nodetype", "jnt:privateNote");
        jsonObject.put("name", subject);
        jsonObject.put("locale", "en");
        jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
        jsonObject.put("saveFileUnderNewlyCreatedNode", Boolean.TRUE);

        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("note", parsedMailContent.getBody());
        properties.put("jcr:title", subject);
        properties.put("date",
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis()));
        jsonObject.put("properties", properties);

        jsonObject.put("path",
                getUserManagerService().getUserSplittingRule().getPathForUsername(sender.getName())
                        + "/contents");

        if (!parsedMailContent.getFiles().isEmpty()) {
            jsonObject.put("files", BaseMailDecoder.toJSON(parsedMailContent.getFiles()));
        }

        return jsonObject.toString();
    }

    @Override
    public String getKey() {
        return "<default>";
    }

}