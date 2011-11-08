package org.jahia.modules.gateway.decoders

import java.security.Principal
import javax.mail.Address
import org.jahia.api.Constants
import org.jahia.modules.gateway.MailDecoder
import org.jahia.services.usermanager.JahiaUserManagerService
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * @author : rincevent
 * @since : JAHIA 6.1
 * Created : 11/8/11
 */
class ArticleMailDecoderImpl implements MailDecoder {
    Map<String, String> paths;
    JahiaUserManagerService userManagerService;

    String decode(String title, String nodepath, String body, Address[] from) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nodetype", "jnt:article");
            jsonObject.put("name", title);
            jsonObject.put("locale", "en");
            jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
            jsonObject.put("path", paths.get(nodepath));
            Map<String, String> properties = new LinkedHashMap<String, String>();
            properties.put("jcr:title", title);
            if (from != null && from.length > 0) {
                Properties userProperties = new Properties();
                userProperties.setProperty("j:email", from[0].toString());
                try {
                    Set<Principal> principals = userManagerService.searchUsers(userProperties);
                    if (!principals.isEmpty()) {
                        properties.put("username", principals.iterator().next().getName());
                    }
                } catch (Exception e) {
                }
            }
            // Parse body
            def text = "";
            boolean intro = true;
            body.replaceAll("<br>", "\n").replaceAll("<br/>", "\n").eachLine {line, idx ->
                if (!"".equals(line)) {
                    if (line.startsWith("tags:")) {
                        jsonObject.put("tags", line.substring(5))
                    } else {
                        text += line;
                    }
                } else if (!"".equals(text)) {
                    if (intro) {
                        properties.put("intro", text)
                        intro = false
                    } else {
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("nodetype", "jnt:paragraph");
                        jsonObject1.put("name", "paragraph-" + idx);
                        Map<String, String> childProperties = new LinkedHashMap<String, String>();
                        childProperties.put("body", text)
                        jsonObject1.put("properties", childProperties)
                        jsonObject.append("childs", jsonObject1);
                    }
                    text = "";
                }
            }
            if (!"".equals(text)) {
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("nodetype", "jnt:paragraph");
                jsonObject1.put("name", "paragraph-last");
                Map<String, String> childProperties = new LinkedHashMap<String, String>();
                childProperties.put("body", text)
                jsonObject1.put("properties", childProperties)
                jsonObject.append("childs", jsonObject1);
            }
            jsonObject.put("properties", properties);
            return jsonObject.toString();
        } catch (JSONException e) {
            throw e;
        }
    }

}
