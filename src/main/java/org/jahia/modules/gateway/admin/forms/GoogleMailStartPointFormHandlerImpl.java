package org.jahia.modules.gateway.admin.forms;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Handle form for gmail account.
 * User: rincevent
 * Date: 09/11/11
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoogleMailStartPointFormHandlerImpl implements StartPointFormHandler {
    public String parseForm(HttpServletRequest request) throws StartPointFormHandlerException {
        String username = request.getParameter("username");
        if(username!=null) {
        String password = request.getParameter("password");
        String delay = request.getParameter("delay");
            try {
                return "imaps://imap.gmail.com?username="+ URLDecoder.decode(username,"UTF-8")+"&password="+ URLDecoder.decode(password,"UTF-8")+"&consumer.delay="+delay;
            } catch (UnsupportedEncodingException e) {
               throw new StartPointFormHandlerException(e);
            }
        } else {
            throw new StartPointFormHandlerException("username should not be empty");
        }
    }
}
