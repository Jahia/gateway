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

import org.apache.log4j.Logger;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.MailDecoder;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.Address;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/8/11
 */
public class NewsMailDecoderImpl implements MailDecoder {
    private transient static Logger logger = Logger.getLogger(NewsMailDecoderImpl.class);
    private Map<String, String> paths;
    private JahiaUserManagerService userManagerService;

    public String decode(String title, String nodepath, String body, Address[] from) throws Exception {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nodetype", "jnt:news");
            jsonObject.put("name", title);
            jsonObject.put("locale", "en");
            jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
            jsonObject.put("path", paths.get(nodepath));
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("desc", body);
            properties.put("jcr:title", title);
            jsonObject.put("properties", properties);
            if (from != null && from.length > 0) {
                Properties userProperties = new Properties();
                userProperties.setProperty("j:email", from[0].toString());
                try {
                    Set<Principal> principals = userManagerService.searchUsers(userProperties);
                    if (!principals.isEmpty()) {
                        properties.put("username", principals.iterator().next().getName());
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    public void addPath(String name, String path) {
        paths.put(name, path);
    }

    public Map<String, String> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, String> paths) {
        this.paths = paths;
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }
}
