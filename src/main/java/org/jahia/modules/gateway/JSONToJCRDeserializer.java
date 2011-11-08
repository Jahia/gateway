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

import org.apache.camel.Handler;
import org.apache.log4j.Logger;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.tags.TaggingService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.Iterator;

/**
 * This class will create nodes based on their JSON serialization.
 * <p/>
 * {
 * nodetype:"jnt:news",
 * name:"name of the new node if available otherwise a name will be created based on this one"
 * username:"name of the user creating the node"
 * locale:"which language to use for creation"
 * workspace:"in which workspace we create the node (default,live)"
 * path:"where we create the node"
 * properties :
 * {
 * property_name : "property_value",
 * property_name : "property_value",
 * |
 * |
 * } "map of the properties for this node".
 * }
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/7/11
 */
public class JSONToJCRDeserializer {
    private transient static Logger logger = Logger.getLogger(JSONToJCRDeserializer.class);
    private JCRTemplate jcrTemplate;
    private TaggingService taggingService;

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Handler
    public void deserialize(String body) {
        if (body != null && body.startsWith("{")) {
            try {
                final JSONObject jsonObject = new JSONObject(body);

                final String nodetype = jsonObject.getString("nodetype");
                assert nodetype != null;

                // Check that nodetype exists
                NodeTypeRegistry.getInstance().getNodeType(nodetype);

                final String name = jsonObject.getString("name");
                assert name != null;

                String username = null;
                if (jsonObject.has("username")) {
                    username = jsonObject.getString("username");
                }
                String locale = jsonObject.getString("locale");
                assert locale != null;
                String workspace = jsonObject.getString("workspace");
                assert workspace != null;
                final String path = jsonObject.getString("path");
                assert path != null;
                final JSONObject properties = jsonObject.getJSONObject("properties");
                assert properties != null;
                jcrTemplate.doExecuteWithSystemSession(username, workspace,
                        org.jahia.utils.LanguageCodeConverters.languageCodeToLocale(locale), new JCRCallback<Object>() {
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        logger.debug("Getting parent node with path : " + path);
                        JCRNodeWrapper node = session.getNode(path);
                        String availableNodeName = JCRContentUtils.findAvailableNodeName(node,
                                JCRContentUtils.generateNodeName(name, 32));
                        logger.debug("adding subnode with name : " + availableNodeName);
                        JCRNodeWrapper newNode = node.addNode(availableNodeName, nodetype);
                        setPropertiesOnNode(newNode, properties);
                        //Manage childs
                        try {
                            if (jsonObject.has("childs")) {
                                JSONArray childs = jsonObject.getJSONArray("childs");
                                for (int i = 0; i < childs.length(); i++) {
                                    JSONObject childJSONObject = childs.getJSONObject(i);
                                    String childNodetype = childJSONObject.getString("nodetype");
                                    assert childNodetype != null;
                                    // Check that nodetype exists
                                    NodeTypeRegistry.getInstance().getNodeType(childNodetype);
                                    String childName = childJSONObject.getString("name");
                                    assert childName != null;
                                    JSONObject childProperties = childJSONObject.getJSONObject("properties");
                                    assert childProperties != null;
                                    String childAvailableNodeName = JCRContentUtils.findAvailableNodeName(node,
                                            JCRContentUtils.generateNodeName(childName, 32));
                                    logger.debug("adding subnode with name : " + availableNodeName);
                                    JCRNodeWrapper childNode = newNode.addNode(childAvailableNodeName, childNodetype);
                                    setPropertiesOnNode(childNode, childProperties);
                                }
                            }
                            if (jsonObject.has("tags")) {
                                String[] tags = jsonObject.getString("tags").split(",");
                                String siteKey = newNode.getResolveSite().getSiteKey();
                                for (String tag : tags) {
                                    taggingService.tag(newNode.getPath(), tag.trim(), siteKey, true, session);
                                }
                            }
                        } catch (JSONException e) {
                            logger.error(e.getMessage(), e);
                        }
                        session.save();
                        return null;
                    }
                });
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            } catch (RepositoryException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void setPropertiesOnNode(JCRNodeWrapper newNode, JSONObject properties) throws RepositoryException {
        Iterator keys = properties.keys();
        while (keys.hasNext()) {
            String property = (String) keys.next();
            try {
                String value = (String) properties.get(property);
                logger.debug("added property " + property + " with value " + value);
                newNode.setProperty(property.replaceAll("_", ":"), value);
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }
}
