/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.gateway.mail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;

import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.gateway.ConfigurableCamelHandler;
import org.jahia.modules.gateway.GatewayTransformerConfigurationException;
import org.jahia.modules.gateway.mail.MailContent.FileItem;
import org.jahia.services.JahiaAfterInitializationService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Message handler that dispatches the incoming {@link MailMessage} to a corresponding {@link MailDecoder} instance.
 * 
 * @author rincevent
 */
public class MailToJSON implements ConfigurableCamelHandler, JahiaAfterInitializationService, BeanNameAware {

    private static Logger logger = LoggerFactory.getLogger(MailToJSON.class);

    private Map<String, MailDecoder> decoders = new LinkedHashMap<String, MailDecoder>(2);

    private String key;

    public ProcessorDefinition appendToRoute(ProcessorDefinition processorDefinition) {
        return processorDefinition.bean(this);
    }

    public void configure(HttpServletRequest request)
            throws GatewayTransformerConfigurationException {
        // TODO implement me
    }

    public Map<String, MailDecoder> getDecoders() {
        return decoders;
    }

    public String getKey() {
        return key;
    }

    public boolean matches(MailContent mailContent, Pattern pattern) {
        String content = mailContent.getBody();
        if (StringUtils.isBlank(content)) {
            return false;
        }
        Source source = new Source(content);
        OutputDocument outputDocument = new OutputDocument(source);
        outputDocument.remove(source.getAllElements("head"));
        outputDocument.remove(source.getAllElements("base"));
        content = outputDocument.toString();
        content = content.replaceAll("<[^<>]+>", "\n");
        StringTokenizer lineTokenizer = new StringTokenizer(content, "\r\n", false);
        String line = null;
        while (lineTokenizer.hasMoreTokens() && StringUtils.isBlank(line)) {
            line = lineTokenizer.nextToken();
        }
        if (StringUtils.isBlank(line)) {
            return false;
        }
        return pattern.matcher(line).matches();
    }

    @Handler
    public void handleExchange(Exchange exchange) {
        assert exchange.getIn() instanceof MailMessage;
        long timer = System.currentTimeMillis();
        final Message mailMessage = ((MailMessage) exchange.getIn()).getMessage();
        try {
            String subject = mailMessage.getSubject();

            Address[] from = mailMessage.getFrom();
            Address sender = from != null && from.length > 0 ? from[0] : null;

            if (logger.isDebugEnabled()) {
                logger.debug("Got message from {} with the subject: {}", sender, subject);
            }

            // Parse content and multipart
            MailContent mailContent = new MailContent();
            parseMailMessage(mailMessage, mailContent);

            if (logger.isTraceEnabled()) {
                logger.trace("Parsed message body:\n{} \n\nFiles:\n{}", mailContent.getBody(),
                        mailContent.getFiles());
            }

            Pattern matchingPattern = null;
            MailDecoder decoder = decoders.get("<default>"); // get the default decoder if any

            decodersLoop:
            for (MailDecoder examinee : decoders.values()) {
                List<Pattern> patterns = examinee.getPatterns();
                if (patterns != null) {
                    for (Pattern regexp : patterns) {
                        if (matches(mailContent, regexp)) {
                            decoder = examinee;
                            matchingPattern = regexp;
                            break decodersLoop;
                        }
                    }
                }
            }

            if (decoder != null) {
                if (logger.isDebugEnabled()) {
                    if (matchingPattern == null) {
                        logger.debug(
                                "Using default decoder '{}' ({}) for the e-mail",
                                new String[] { decoder.getKey(), decoder.getClass().getName() });
                    } else {
                        logger.debug(
                                "Using decoder '{}' ({}) for the e-mail matching pattern \"{}\"",
                                new String[] { decoder.getKey(), decoder.getClass().getName(),
                                        matchingPattern.pattern() });
                    }
                }

                boolean deleteFiles = false;
                
                String jsonOutput = null;
                try {
                    jsonOutput = decoder.decode(matchingPattern, mailContent, mailMessage);
                } catch (Exception e) {
                    logger.error("Error processing e-mail message with subject \"" + subject
                            + "\" using decoder " + decoder.getKey() + " from " + sender
                            + ", Cause: " + e.getMessage(), e);
                    deleteFiles = true;
                }
                if (StringUtils.isNotBlank(jsonOutput)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Decoder output is:\n{}", jsonOutput);
                    }
                    DefaultMessage in = new DefaultMessage();
                    in.setBody(jsonOutput);
                    exchange.setOut(in);
                } else {
                    deleteFiles = true;
                }
                
                if (deleteFiles && !mailContent.getFiles().isEmpty()) {
                    for (FileItem file : mailContent.getFiles()) {
                        FileUtils.deleteQuietly(file.getFile());
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping e-mail as no decoder configured to match subject: {}\n",
                            subject);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Message from {} with the subject '{}' processed in {} ms",
                        new Object[] { sender, subject, System.currentTimeMillis() - timer });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void initAfterAllServicesAreStarted() throws JahiaInitializationException {
        if (System.getProperty("mail.mime.decodefilename") == null) {
            System.setProperty("mail.mime.decodefilename", "true");
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    try {
                        session.getNode("/gateway");
                        JCRNodeWrapper nodeWrapper = session.getNode("/gateway/transformersData");
                        if (!nodeWrapper.hasNode(getKey())) {
                            nodeWrapper.addNode(getKey(), "jnt:gtwMailtoJson");
                            session.save();
                        }
                    } catch (PathNotFoundException e) {
                        logger.debug("Gateway not imported yet");
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            throw new JahiaInitializationException(e.getMessage(), e);
        }
    }

    protected void parseMailMessage(Part part, MailContent content) throws IOException,
            MessagingException {
        Object mailContent = part.getContent();
        if (mailContent instanceof MimeMultipart) {
            MimeMultipart mailMessageContent = (MimeMultipart) mailContent;
            // We have some attachments
            for (int i = 0; i < mailMessageContent.getCount(); i++) {
                BodyPart bodyPart = mailMessageContent.getBodyPart(i);
                parseMailMessage(bodyPart, content);
            }
        } else if (mailContent instanceof String && part.getDisposition() == null) {
            boolean isHtml = false;
            if (content.getBody() == null || ((isHtml = part.isMimeType("text/html")) && !content.isHtml())) {
                if (isHtml) {
                    content.setBodyHtml((String) mailContent);
                } else {
                    content.setBody((String) mailContent);
                }
            }
        } else if (mailContent instanceof InputStream || mailContent instanceof String) {
            File tempFile = File.createTempFile("mail2json-", null);
            try {
                FileUtils.copyInputStreamToFile(mailContent instanceof InputStream ? (InputStream) mailContent : part.getInputStream(), tempFile);
                content.getFiles().add(new FileItem(StringUtils.defaultIfEmpty(part.getFileName(), "unknown"), tempFile));
            } catch (IOException e) {
                FileUtils.deleteQuietly(tempFile);
                throw e;
            }
        }
        assert content.getBody() != null;
    }

    public void setBeanName(String name) {
        key = name;
    }

    public void setDecoders(Map<String, MailDecoder> decoders) {
        this.decoders.clear();
        if (decoders != null) {
            this.decoders.putAll(decoders);
        }
    }
}
