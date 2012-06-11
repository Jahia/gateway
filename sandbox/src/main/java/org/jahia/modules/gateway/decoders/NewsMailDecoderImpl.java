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
package org.jahia.modules.gateway.decoders;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.mail.MailContent;
import org.jahia.services.usermanager.JahiaUser;
import org.json.JSONObject;

/**
 * Mail decoder for news items.
 * 
 * @author rincevent
 */
public class NewsMailDecoderImpl extends BaseMailDecoder {

    private Map<String, String> paths = new HashMap<String, String>();

    public String decode(Pattern matchingPattern, MailContent parsedMailContent,
            Message originalMessage) throws Exception {
        String title = originalMessage.getSubject();
        String nodepath = retrieveToken(parsedMailContent, matchingPattern, 2);

        nodepath = StringUtils.isNotBlank(nodepath) && paths.containsKey(nodepath) ? paths
                .get(nodepath) : "/sites/systemsite/contents/news";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodetype", "jnt:news");
        jsonObject.put("name", StringUtils.defaultIfBlank(title, "Unknown"));
        jsonObject.put("locale", "en");
        jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
        jsonObject.put("path", nodepath);
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("desc", parsedMailContent.getBody());
        properties.put("jcr:title", StringUtils.defaultIfBlank(title, "Unknown"));
        properties.put("date",
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis()));
        if (!parsedMailContent.getFiles().isEmpty()) {
            properties
                    .put("image", parsedMailContent.getFiles().get(0).getFile().getAbsolutePath());
        }
        jsonObject.put("properties", properties);

        JahiaUser sender = getSender(originalMessage);
        if (sender != null) {
            properties.put("username", sender.getName());
        }

        return jsonObject.toString();
    }

    public void setPaths(Map<String, String> paths) {
        this.paths.clear();
        if (paths != null) {
            this.paths.putAll(paths);
        }
    }
}
