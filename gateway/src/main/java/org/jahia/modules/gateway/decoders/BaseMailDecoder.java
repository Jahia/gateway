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

import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.gateway.mail.MailContent;
import org.jahia.modules.gateway.mail.MailContent.FileItem;
import org.jahia.modules.gateway.mail.MailDecoder;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract mail decoder containing basic functionality for e-mail handling.
 * 
 * @author Sergiy Shyrkov
 */
public abstract class BaseMailDecoder implements MailDecoder {

    public static final Pattern LINE_PATTERN = Pattern.compile("\\r?\\n");

    private static final Logger logger = LoggerFactory.getLogger(BaseMailDecoder.class);

    public String retrieveToken(MailContent mailContent, Pattern tokenPattern, int groupIndex) {
        String token = null;
        String content = mailContent.getBody();
        if (StringUtils.isNotBlank(content)) {
            Source source = new Source(content);
            SourceFormatter sourceFormatter = source.getSourceFormatter();
            content = sourceFormatter.toString();
            content = content.replaceAll("(<br ?/?>)", "$1\n");
            StringBuilder resultContent = new StringBuilder();
            StringTokenizer lineTokenizer = new StringTokenizer(content, "\r\n", true);
            while (lineTokenizer.hasMoreTokens()) {
                String line = lineTokenizer.nextToken();
                Matcher m = tokenPattern.matcher(line);
                if (m.matches()) {
                    token = m.group(groupIndex);
                    for (int g = 1; g <= m.groupCount(); g++) {
                        line = line.replaceFirst(m.group(g), "");
                    }
                    if (line.trim().equals("")) {
                        lineTokenizer.nextToken();
                    } else {
                        resultContent.append(line);
                    }
                    break;
                } else {
                    resultContent.append(line);
                }
                if (parsedTextDelimiter != null) {
                    Matcher endMatcher = parsedTextDelimiter.matcher(line);
                    if (endMatcher.matches()) {
                        break;
                    }
                }
            }
            while (lineTokenizer.hasMoreTokens()) {
                resultContent.append(lineTokenizer.nextToken());
            }
            mailContent.setBody(resultContent.toString());
        }
        return token;
    }

    public static JSONObject toJSON(FileItem fileItem) {
        JSONObject json = new JSONObject();
        try {
            if (fileItem.getName() != null) {
                json.put("name", fileItem.getName());
            }
            if (fileItem.getFile() != null) {
                json.put("file", fileItem.getFile().getAbsolutePath());
            }
            if (fileItem.getContentType() != null) {
                json.put("contentType", fileItem.getContentType());
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }

    public static JSONArray toJSON(List<FileItem> fileItems) {
        if (fileItems == null) {
            return null;
        }
        JSONArray json = new JSONArray();
        for (FileItem fileItem : fileItems) {
            json.put(BaseMailDecoder.toJSON(fileItem));
        }

        return json;
    }

    private String key;

    private String parentHandlerKey;

    private LinkedList<Pattern> patterns;

    private JahiaUserManagerService userManagerService;

    protected Pattern parsedTextDelimiter;

    public String getKey() {
        return key;
    }

    public String getParentHandlerKey() {
        return parentHandlerKey;
    }

    public LinkedList<Pattern> getPatterns() {
        return patterns;
    }

    protected JahiaUser getSender(Message message) {
        JahiaUser user = null;
        try {
            Address[] senders = message.getFrom();
            String from = null;
            if (senders != null && senders.length > 0) {
                from = ((InternetAddress) senders[0]).getAddress();
            }
            if (StringUtils.isNotBlank(from)) {
                Properties userProperties = new Properties();
                userProperties.setProperty("j:email", from);
                Set<JCRUserNode> users = getUserManagerService().searchUsers(userProperties);
                user = users != null && !users.isEmpty() ? users.iterator().next().getJahiaUser() : null;
            }
        } catch (MessagingException e) {
            logger.warn("Unable to retrieve Jahia user that corresponds"
                    + " to the e-mail sender. Cause: " + e.getMessage(), e);
        }

        return user;
    }

    protected String getSenderEmail(Message message) {
        String from = null;
        try {
            Address[] senders = message.getFrom();
            if (senders != null && senders.length > 0) {
                from = ((InternetAddress) senders[0]).getAddress();
            }
        } catch (MessagingException e) {
            logger.error(e.getMessage(), e);
        }
        return from;
    }

    protected JahiaUserManagerService getUserManagerService() {
        return userManagerService;
    }

    protected void removeDelimiter(MailContent mailContent) {
        retrieveToken(mailContent, parsedTextDelimiter, 1);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setParentHandlerKey(String parentHandlerKey) {
        this.parentHandlerKey = parentHandlerKey;
    }

    public void setPatternsToMatch(List<String> patterns) {
        this.patterns = new LinkedList<Pattern>();
        for (String regexp : patterns) {
            this.patterns.add(Pattern.compile(regexp, Pattern.CASE_INSENSITIVE));
        }
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    public void setParsedTextDelimiter(String parsedTextDelimiter) {
        this.parsedTextDelimiter = Pattern.compile(parsedTextDelimiter, Pattern.CASE_INSENSITIVE);
    }
    
    public String getParsedTextDelimiter() {
        return parsedTextDelimiter != null ? parsedTextDelimiter.toString() : "";
    }    

}