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

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.impl.DefaultMessage;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rincevent
 * @since : JAHIA 6.1
 *        Created : 11/7/11
 */
public class MailToJSON {
    private transient static Logger logger = Logger.getLogger(MailToJSON.class);
    private List<Pattern> regexps;
    private Map<String, MailDecoder> decoders;

    @Handler
    public void transformMailToJSON(Exchange exchange) {
        assert exchange.getIn() instanceof MailMessage;
        final Message mailMessage = ((MailMessage) exchange.getIn()).getMessage();
        try {
            final String subject = mailMessage.getSubject();
            MimeMultipart mailMessageContent = (MimeMultipart) mailMessage.getContent();
            final String content;
            if (mailMessageContent.getCount() > 1) {
                content = (String) mailMessageContent.getBodyPart(1).getContent();
            } else {
                content = (String) mailMessageContent.getBodyPart(0).getContent();
            }
            for (Pattern regexp : regexps) {
                Matcher matcher = regexp.matcher(subject);
                if (matcher.matches()) {
                    if (decoders.containsKey(matcher.group(1))) {
                        DefaultMessage in = new DefaultMessage();
                        String decode = decoders.get(matcher.group(1)).decode(matcher.group(2), matcher.group(3),
                                content, mailMessage.getFrom());
                        in.setBody(decode);
                        exchange.setOut(in);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
}
