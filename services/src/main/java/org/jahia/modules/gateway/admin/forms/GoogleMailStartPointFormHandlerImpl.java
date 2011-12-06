package org.jahia.modules.gateway.admin.forms;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.jahia.modules.gateway.CamelStartPoint;
import org.jahia.modules.gateway.GatewayService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Handle form for gmail account.
 * User: rincevent
 * Date: 09/11/11
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoogleMailStartPointFormHandlerImpl implements StartPointFormHandler,CamelStartPoint {
    private JCRTemplate template;
    private String uri;

    public void setTemplate(JCRTemplate template) {
        this.template = template;
    }

    public void parseForm(HttpServletRequest request, String startPointType, final String startPointName, final GatewayService gatewayService) throws StartPointFormHandlerException {
        String username = request.getParameter("username");
        if (username != null) {
            String password = request.getParameter("password");
            String delay = request.getParameter("delay");
            try {
                uri = "imaps://imap.gmail.com?username=" + URLDecoder.decode(username, "UTF-8") + "&password=" +
                             URLDecoder.decode(password, "UTF-8") + "&consumer.delay=" + delay;
                try {
                    template.doExecuteWithSystemSession(new JCRCallback<Object>() {
                        public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                            JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                            JCRNodeWrapper jcrNodeWrapper = node.addNode(startPointName, "jnt:gmailstartpoint");
                            jcrNodeWrapper.setProperty("uri", uri);
                            session.save();
                            return null;
                        }
                    });
                    gatewayService.getRouteStartPoints().put(startPointName,this);
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                throw new StartPointFormHandlerException(e);
            }
        } else {
            throw new StartPointFormHandlerException("username should not be empty");
        }
    }

    public void initStartPoint(JCRNodeWrapper node) throws RepositoryException {
        uri = node.getProperty("uri").getValue().getString();
    }

    public ProcessorDefinition startRoute(RouteBuilder routeBuilder) {
        return routeBuilder.from(uri);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{uri='").append(uri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
