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
 *     This program is free software; you can redistribute it and/or
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
package org.jahia.modules.gateway.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

/**
 * Bean post processor class that is aimed for registering and unregistering {@link MailDecoder}s.
 * 
 * @author Sergiy Shyrkov
 */
public class MailDecoderRegistrator implements BeanPostProcessor, DestructionAwareBeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MailDecoderRegistrator.class);

    private MailToJSON mailToJsonHandler;

    /**
     * Initializes an instance of this class injecting the corresponding mail to JSON handler.
     * 
     * @param mailToJsonHandler
     *            the mail to JSON handler instance
     */
    public MailDecoderRegistrator(MailToJSON mailToJsonHandler) {
        super();
        this.mailToJsonHandler = mailToJsonHandler;
    }

    private boolean canHandle(Object bean) {
        if (!(bean instanceof MailDecoder)) {
            return false;
        }
        MailDecoder decoder = (MailDecoder) bean;
        return decoder.getParentHandlerKey() != null
                && mailToJsonHandler.getKey().equals(decoder.getParentHandlerKey())
                || decoder.getParentHandlerKey() == null && "mailtojson".equals(mailToJsonHandler.getKey());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (canHandle(bean)) {
            MailDecoder decoder = (MailDecoder) bean;
            logger.info("Registering mail decoder of type {} with the key {} for handler {}", new String[] {
                    decoder.getClass().getName(), decoder.getKey(), mailToJsonHandler.getKey() });
            mailToJsonHandler.getDecoders().put(decoder.getKey(), decoder);
        }

        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (canHandle(bean)) {
            MailDecoder decoder = (MailDecoder) bean;
            logger.info("Unregistering mail decoder of type {} with the key {} for handler {}", new String[] {
                    decoder.getClass().getName(), decoder.getKey(), mailToJsonHandler.getKey() });
            mailToJsonHandler.getDecoders().remove(decoder.getKey());
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
