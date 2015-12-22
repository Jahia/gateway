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