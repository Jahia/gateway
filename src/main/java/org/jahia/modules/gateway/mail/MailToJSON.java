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
package org.jahia.modules.gateway.mail;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.jahia.api.Constants;
import org.jahia.modules.gateway.ConfigurableCamelHandler;
import org.jahia.modules.gateway.GatewayTransformerConfigurationException;
import org.jahia.services.content.*;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.getInstance;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/7/11
 */
public class MailToJSON implements ConfigurableCamelHandler, InitializingBean {
    private transient static Logger logger = Logger.getLogger(MailToJSON.class);
    private List<Pattern> regexps;
    private Map<String, MailDecoder> decoders;
    private JCRTemplate jcrTemplate;
    private JahiaUserManagerService userManagerService;

    @Handler
    public void handleExchange(Exchange exchange) {
        assert exchange.getIn() instanceof MailMessage;
        final Message mailMessage = ((MailMessage) exchange.getIn()).getMessage();
        try {
            String subject = mailMessage.getSubject();
            boolean matches = false;
            Address[] from = mailMessage.getFrom();
            for (Pattern regexp : regexps) {
                Matcher matcher = regexp.matcher(subject);
                if (matcher.matches()) {
                    // Handle content and multipart
                    MailContent mailContent = new MailContent();
                    handleMailMessage(mailMessage, mailContent);

                    if (decoders.containsKey(matcher.group(1))) {
                        DefaultMessage in = new DefaultMessage();
                        String decode = decoders.get(matcher.group(1)).decode(matcher.group(2), matcher.group(3),
                                mailContent, from);
                        in.setBody(decode);
                        exchange.setOut(in);
                        matches = true;
                    }
                }
            }
            if (!matches) {
                //Todo create personal note in the user folder
                MailContent mailContent = new MailContent();
                handleMailMessage(mailMessage, mailContent);
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("nodetype", "jnt:privateNote");
                    jsonObject.put("name", subject);
                    jsonObject.put("locale", "en");
                    jsonObject.put("workspace", Constants.EDIT_WORKSPACE);
                    jsonObject.put("saveFileUnderNewlyCreatedNode",Boolean.TRUE);
                    Map<String, String> properties = new LinkedHashMap<String, String>();
                    properties.put("note", mailContent.getBody());
                    properties.put("jcr:title", subject);
                    properties.put("date", DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(getInstance()));
                    jsonObject.put("properties", properties);
                    //Add a file if needed
                    if (from != null && from.length > 0) {
                        Properties userProperties = new Properties();
                        userProperties.setProperty("j:email", ((InternetAddress) from[0]).getAddress());
                        try {
                            Set<Principal> principals = userManagerService.searchUsers(userProperties);
                            if (!principals.isEmpty()) {
                                Principal principal = principals.iterator().next();
                                jsonObject.put("path", userManagerService.getUserSplittingRule().getPathForUsername(
                                                principal.getName()) + "/contents");
                                List<File> files = mailContent.getFiles();
                                List<String> filesPath = new LinkedList<String>();
                                if(!files.isEmpty()) {
                                    for (File file : files) {
                                        filesPath.add(file.getAbsolutePath());
                                    }
                                    jsonObject.put("files",filesPath);
                                }
                                DefaultMessage in = new DefaultMessage();
                                in.setBody(jsonObject.toString());
                                exchange.setOut(in);
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } catch (JSONException e) {
                    logger.error(e.getMessage(), e);
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleMailMessage(Part part, MailContent content) throws IOException, MessagingException {
        Object mailContent = part.getContent();
        if (mailContent instanceof MimeMultipart) {
            MimeMultipart mailMessageContent = (MimeMultipart) mailContent;
            // We have some attachments
            for (int i = 0; i < mailMessageContent.getCount(); i++) {
                BodyPart bodyPart = mailMessageContent.getBodyPart(i);
                handleMailMessage(bodyPart, content);
            }
        } else if (mailContent instanceof String) {
            if (content.getBody() == null || part.isMimeType("text/html")) {
                content.setBody((String) mailContent);
            }
        } else if (mailContent instanceof InputStream) {
            String filename = part.getFileName();
            File tempFile = File.createTempFile(StringUtils.substringBeforeLast(filename, "."),
                    "." + StringUtils.substringAfterLast(filename, "."));
            FileUtils.copyInputStreamToFile((InputStream) mailContent, tempFile);
            content.getFiles().add(tempFile);
        }
        assert content.getBody() != null;
    }

    public ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition) {
        return processorDefinition.bean(this);
    }

    public void configure(HttpServletRequest request) throws GatewayTransformerConfigurationException {
        String operation = request.getParameter("mailtojsonOperation");
        if ("addPath".equals(operation)) {
            final String decoderName = request.getParameter("decoderName");
            final String pathName = request.getParameter("pathName");
            final String path = request.getParameter("path");
            if (decoders.containsKey(decoderName)) {
                decoders.get(decoderName).addPath(pathName, path);
                try {
                    jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            JCRNodeWrapper nodeWrapper = session.getNode("/gateway/transformersData");
                            JCRNodeWrapper node;
                            if (!nodeWrapper.hasNode("mailtojson")) {
                                node = nodeWrapper.addNode("mailtojson", "jnt:gtwMailtoJson");
                            } else {
                                node = nodeWrapper.getNode("mailtojson");
                            }
                            JCRNodeWrapper jcrNodeWrapper = node.addNode(JCRContentUtils.findAvailableNodeName(node,
                                    decoderName), "jnt:gtwDecoderPath");
                            jcrNodeWrapper.setProperty("decoderName", decoderName);
                            jcrNodeWrapper.setProperty("pathName", pathName);
                            jcrNodeWrapper.setProperty("pathReference", path);
                            session.save();
                            return null;
                        }
                    });
                } catch (RepositoryException e) {
                    throw new GatewayTransformerConfigurationException(e);
                }
            }
        } else if ("addRegexp".equals(operation)) {
            try {
                final String regexp = URLDecoder.decode(request.getParameter("regexp"), "UTF-8");
                regexps.add(Pattern.compile(regexp));
                jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        JCRNodeWrapper nodeWrapper = session.getNode("/gateway/transformersData");
                        JCRNodeWrapper node;
                        if (!nodeWrapper.hasNode("mailtojson")) {
                            node = nodeWrapper.addNode("mailtojson", "jnt:gtwMailtoJson");
                        } else {
                            node = nodeWrapper.getNode("mailtojson");
                        }
                        JCRNodeWrapper regexpsNode;
                        if (node.hasNode("regexps")) {
                            regexpsNode = node.getNode("regexps");
                        } else {
                            regexpsNode = node.addNode("regexps", "jnt:gtwRegexps");
                        }
                        JCRNodeWrapper jcrNodeWrapper = regexpsNode.addNode(JCRContentUtils.findAvailableNodeName(
                                regexpsNode, "regexp"), "jnt:gtwRegexp");
                        jcrNodeWrapper.setProperty("regexp", regexp);
                        session.save();
                        return null;
                    }
                });
            } catch (UnsupportedEncodingException e) {
                throw new GatewayTransformerConfigurationException(e);
            } catch (RepositoryException e) {
                throw new GatewayTransformerConfigurationException(e);
            }
        }
    }

    public void setRegexps(List<String> regexps) {
        this.regexps = new LinkedList<Pattern>();
        for (String regexp : regexps) {
            this.regexps.add(Pattern.compile(regexp));
        }
    }

    public void setDecoders(Map<String, MailDecoder> decoders) {
        this.decoders = decoders;
    }

    public List<Pattern> getRegexps() {
        return regexps;
    }

    public Map<String, MailDecoder> getDecoders() {
        return decoders;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    public void afterPropertiesSet() throws Exception {
        if (regexps == null) {
            regexps = new LinkedList<Pattern>();
        }
        if (decoders == null) {
            throw new BeanCreationException("Decoders should not be empty for this bean to work");
        }
        jcrTemplate.doExecuteWithSystemSession(new JCRCallback<Object>() {
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                try {
                    session.getNode("/gateway");
                    JCRNodeWrapper nodeWrapper = session.getNode("/gateway/transformersData");
                    if (!nodeWrapper.hasNode("mailtojson")) {
                        nodeWrapper.addNode("mailtojson", "jnt:gtwMailtoJson");
                    } else {
                        JCRNodeWrapper mailtojsonNode = nodeWrapper.getNode("mailtojson");
                        // init regexps from jcr
                        if (mailtojsonNode.hasNode("regexps")) {
                            JCRNodeWrapper regexpsNode = mailtojsonNode.getNode("regexps");
                            NodeIterator nodes = regexpsNode.getNodes();
                            while (nodes.hasNext()) {
                                JCRNodeWrapper next = (JCRNodeWrapper) nodes.next();
                                regexps.add(Pattern.compile(next.getProperty("regexp").getString()));
                            }
                        }
                        // init decoders path
                        List<JCRNodeWrapper> decoderPaths = JCRContentUtils.getChildrenOfType(mailtojsonNode,
                                "jnt:gtwDecoderPath");
                        for (JCRNodeWrapper decoderPath : decoderPaths) {
                            if (decoders.containsKey(decoderPath.getProperty("decoderName").getString())) {
                                decoders.get(decoderPath.getProperty("decoderName").getString()).addPath(
                                        decoderPath.getProperty("pathName").getString(), decoderPath.getProperty(
                                        "pathReference").getString());
                            }
                        }
                    }
                } catch (PathNotFoundException e) {
                    logger.debug("Gateway not imported yet");
                }
                return null;
            }
        });
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }
}
