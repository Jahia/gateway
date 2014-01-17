/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.modules.gateway;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.ExtendedPropertyType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.tags.TaggingService;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

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
 * @author rincevent
 * @since JAHIA 6.6.1.0
 *        Created : 11/7/11
 */
public class JSONToJCRDeserializer implements CamelHandler {
    private static Logger logger = Logger.getLogger(JSONToJCRDeserializer.class);
    private JCRTemplate jcrTemplate;
    private TaggingService taggingService;

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Handler
    public void handleExchange(final Exchange exchange) {
        if (exchange != null && exchange.getIn() != null && exchange.getIn().getBody().toString().startsWith("{")) {
            try {
                String body = exchange.getIn().getBody().toString();
                final JSONObject jsonObject = new JSONObject(body);

                final String nodetype = jsonObject.getString("nodetype");
                assert nodetype != null;

                // Check that nodetype exists
                final ExtendedNodeType extendedNodeType = NodeTypeRegistry.getInstance().getNodeType(nodetype);
                assert extendedNodeType != null;
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
                if (!extendedNodeType.isNodeType("nt:file")) {
                    final JSONObject properties = jsonObject.getJSONObject("properties");
                    assert properties != null;
                    jcrTemplate.doExecuteWithSystemSession(username, workspace,
                            org.jahia.utils.LanguageCodeConverters.languageCodeToLocale(locale),
                            new JCRCallback<Object>() {
                                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                                    logger.debug("Getting parent node with path : " + path);
                                    boolean saveFileUnderNode = false;
                                    try {
                                        if (jsonObject.has("saveFileUnderNewlyCreatedNode")) {
                                            saveFileUnderNode = jsonObject.getBoolean("saveFileUnderNewlyCreatedNode");
                                        }
                                    } catch (JSONException e) {
                                        logger.error(e.getMessage(), e);
                                    }
                                    Object header = exchange.getIn().getHeader(Constants.UPDATE_ONLY);
                                    if (header == null || !(Boolean) header) {
                                        createNewNode(session, path, name, nodetype, properties, extendedNodeType,
                                                jsonObject, saveFileUnderNode);
                                    } else {
                                        updateExistingNode(session, path, name, properties, extendedNodeType,
                                                jsonObject, nodetype, saveFileUnderNode);
                                    }
                                    session.save();
                                    return null;
                                }
                            });
                } else if (jsonObject.has("files")) {
                    jcrTemplate.doExecuteWithSystemSession(username, workspace,
                            org.jahia.utils.LanguageCodeConverters.languageCodeToLocale(locale),
                            new JCRCallback<Object>() {
                                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Getting parent node with path : " + path);
                                    }
                                    try {
                                        JCRNodeWrapper node = session.getNode(path);
                                        boolean doUpdate = jsonObject.has("updateifexists") && Boolean.valueOf(jsonObject.getString("updateifexists"));
                                        JSONArray files = jsonObject.getJSONArray("files");
                                        for (int i = 0; i < files.length(); i++) {
                                            File file = null;
                                            String contentType = null;
                                            String nodeName = null;
                                            
                                            Object fileItem = files.get(i);
                                            if (fileItem instanceof JSONObject) {
                                                JSONObject fileDescriptor = (JSONObject) fileItem;
                                                file = new File(fileDescriptor.getString("file"));
                                                nodeName = StringUtils.defaultIfEmpty(
                                                        fileDescriptor.has("name") ? fileDescriptor
                                                                .getString("name") : null, file
                                                                .getName());
                                                contentType = fileDescriptor.has("contentType") ? fileDescriptor
                                                        .getString("contentType") : null;
                                            } else {
                                                file = new File(files.getString(i));
                                                nodeName = file.getName();
                                            }
                                            if (contentType == null) {
                                                contentType = JCRContentUtils.getMimeType(nodeName);
                                            }
                                            
                                            if (file == null || nodeName == null || contentType == null) {
                                                continue;
                                            }
                                            
                                            InputStream is = null;
                                            try {
                                                is = FileUtils.openInputStream(file);
                                                final JCRNodeWrapper newNode = node.uploadFile(
                                                        doUpdate ? nodeName : JCRContentUtils.findAvailableNodeName(
                                                                node, nodeName), is, contentType);
                                                if (jsonObject.has("tags")) {
                                                    String siteKey = newNode.getResolveSite().getSiteKey();
                                                    taggingService.tag(newNode.getPath(), jsonObject.getString("tags"), siteKey, true, session);
                                                }
                                            } catch (IOException e) {
                                                logger.error(e.getMessage(), e);
                                            } finally {
                                                IOUtils.closeQuietly(is);
                                                FileUtils.deleteQuietly(file);
                                            }
                                        }
                                        session.save();
                                    } catch (JSONException e) {
                                        logger.error(e.getMessage(), e);
                                    }
                                    return null;
                                }
                            });
                }
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            } catch (NoSuchNodeTypeException e) {
                logger.error(e.getMessage(), e);
            } catch (RepositoryException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void updateExistingNode(JCRSessionWrapper session, String path, String name, JSONObject properties,
                                    ExtendedNodeType extendedNodeType, JSONObject jsonObject, String nodetype,
                                    boolean saveFileUnderNode) throws RepositoryException {
        try {
            JCRNodeWrapper node = session.getNode(path + "/" + JCRContentUtils.generateNodeName(name, 32));
            setPropertiesOnNode(node, properties, extendedNodeType);
        } catch (PathNotFoundException e) {
            createNewNode(session, path, name, nodetype, properties, extendedNodeType, jsonObject, saveFileUnderNode);
        }
    }

    private void createNewNode(JCRSessionWrapper session, String path, String name, String nodetype,
                               JSONObject properties, ExtendedNodeType extendedNodeType, JSONObject jsonObject,
                               boolean saveFileUnderNode) throws RepositoryException {
        JCRNodeWrapper node = session.getNode(path);
        String availableNodeName = JCRContentUtils.findAvailableNodeName(node, JCRContentUtils.generateNodeName(name,
                32));
        logger.debug("adding subnode with name : " + availableNodeName);
        JCRNodeWrapper newNode = node.addNode(availableNodeName, nodetype);
        setPropertiesOnNode(newNode, properties, extendedNodeType);
        //Manage childs
        try {
            if (jsonObject.has("childs")) {
                JSONArray childs = jsonObject.getJSONArray("childs");
                for (int i = 0; i < childs.length(); i++) {
                    JSONObject childJSONObject = childs.getJSONObject(i);
                    String childNodetype = childJSONObject.getString("nodetype");
                    assert childNodetype != null;
                    // Check that nodetype exists
                    ExtendedNodeType childNodeType = NodeTypeRegistry.getInstance().getNodeType(childNodetype);
                    String childName = childJSONObject.getString("name");
                    assert childName != null;
                    JSONObject childProperties = childJSONObject.getJSONObject("properties");
                    assert childProperties != null;
                    String childAvailableNodeName = JCRContentUtils.findAvailableNodeName(node,
                            JCRContentUtils.generateNodeName(childName, 32));
                    logger.debug("adding subnode with name : " + availableNodeName);
                    JCRNodeWrapper childNode = newNode.addNode(childAvailableNodeName, childNodetype);
                    setPropertiesOnNode(childNode, childProperties, childNodeType);
                }
            }
            if (jsonObject.has("tags")) {
                String siteKey = newNode.getResolveSite().getSiteKey();
                taggingService.tag(newNode.getPath(), jsonObject.getString("tags"), siteKey, true, session);
            }
            if (saveFileUnderNode && jsonObject.has("files")) {
                JSONArray files = jsonObject.getJSONArray("files");
                for (int i = 0; i < files.length(); i++) {
                    FileInputStream is = null;
                    File file = null;
                    try {
                        file = new File(files.getString(i));
                        is = FileUtils.openInputStream(file);
                        newNode.uploadFile(file.getName(), is, JCRContentUtils.getMimeType(file.getName()));
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        IOUtils.closeQuietly(is);
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition) {
        return processorDefinition.bean(this);
    }

    private void setPropertiesOnNode(JCRNodeWrapper newNode, JSONObject properties, ExtendedNodeType nodeType)
            throws RepositoryException {
        Iterator keys = properties.keys();
        while (keys.hasNext()) {
            String property = (String) keys.next();
            try {
                String value = (String) properties.get(property);
                boolean needUpdate;
                logger.debug("added property " + property + " with value " + value);
                String name = property.replaceAll("_", ":");
                try {
                    needUpdate = !(newNode.getProperty(name).getValue().getString().equals(value));
                } catch (RepositoryException e1) {
                    needUpdate = true;
                }
                if (needUpdate) {
                    ExtendedPropertyDefinition propertyDefinition = nodeType.getPropertyDefinition(name);
                    if (propertyDefinition == null) {
                        ExtendedNodeType[] declaredSupertypes = nodeType.getDeclaredSupertypes();
                        for (ExtendedNodeType declaredSupertype : declaredSupertypes) {
                            propertyDefinition = declaredSupertype.getPropertyDefinition(name);
                            if (propertyDefinition != null) {
                                break;
                            }
                        }
                    }
                    int requiredType = 0;
                    if (propertyDefinition != null) {
                        requiredType = propertyDefinition.getRequiredType();
                    }
                    switch (requiredType) {
                        case ExtendedPropertyType.DATE:
                            DateTime dateTime = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(value);
                            newNode.setProperty(name, dateTime.toCalendar(Locale.ENGLISH));
                            break;
                        case ExtendedPropertyType.REFERENCE:
                        case ExtendedPropertyType.WEAKREFERENCE:
                            File file = new File(value);
                            JCRNodeWrapper files = newNode.getSession().getNode(
                                    newNode.getResolveSite().getPath() + "/files");
                            FileInputStream is = null;
                            try {
                                is = FileUtils.openInputStream(file);
                                JCRNodeWrapper reference = files.uploadFile(file.getName(), is,
                                        JCRContentUtils.getMimeType(file.getName()));
                                newNode.setProperty(name, reference);
                            } finally {
                                IOUtils.closeQuietly(is);
                                FileUtils.deleteQuietly(file);
                            }
                            break;
                        default:
                            newNode.setProperty(name, value);
                    }
                }
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void setTaggingService(TaggingService taggingService) {
        this.taggingService = taggingService;
    }
}
