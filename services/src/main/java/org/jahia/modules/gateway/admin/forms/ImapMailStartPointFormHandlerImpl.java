package org.jahia.modules.gateway.admin.forms;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.gateway.CamelStartPoint;
import org.jahia.modules.gateway.GatewayService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for the {@link CamelStartPoint} and form handler for an IMAP mail box.
 * 
 * @author Cédric Mailleux
 * @author Sergiy Shyrkov
 */
public class ImapMailStartPointFormHandlerImpl implements StartPointFormHandler,CamelStartPoint {
    
    private static final Logger logger = LoggerFactory
            .getLogger(ImapMailStartPointFormHandlerImpl.class);
    
    private static final String URI_TEMPLATE = "${protocol}://${host}${port}?username=${username}&password=${password}&consumer.delay=${delay}&delete=${delete}";
    
    private String password;

    private String uri;

    private String getInterpolatedUri() {
        return uri.contains("${password}") ? StringUtils.replace(uri, "${password}", (StringUtils
                .isNotEmpty(password) ? EncryptionUtils.passwordBaseDecrypt(password) : "")) : uri;
    }

    public void initStartPoint(JCRNodeWrapper node) throws RepositoryException {
        uri = node.getProperty("uri").getValue().getString();
        password = node.hasProperty("password") ? node.getProperty("password").getString() : "";
    }

    public void parseForm(HttpServletRequest request, String startPointType, final String startPointName, final GatewayService gatewayService) throws StartPointFormHandlerException {
        String protocol = StringUtils.defaultIfEmpty(request.getParameter("protocol"), "imap");
        String host = StringUtils.defaultIfEmpty(request.getParameter("host"), "imap.gmail.com");
        if ("imap.gmail.com".equals(host)) {
            protocol = "imaps";
        }
        String port = StringUtils.defaultIfEmpty(request.getParameter("port"), "");
        if (port.length() > 0) {
            port += ":";
        }
        String username = StringUtils.defaultString(request.getParameter("username"));
        password = StringUtils.defaultString(request.getParameter("password"));
        String delay = StringUtils.defaultIfEmpty(request.getParameter("delay"), "60000");
        String delete = StringUtils.defaultString(request.getParameter("delete"), "true");
        
        uri = StringUtils.replaceEachRepeatedly(URI_TEMPLATE, new String[] { "${protocol}",
                "${host}", "${port}", "${username}", "${delay}", "${delete}" }, new String[] {
                protocol, host, port, username, delay, delete });

        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper node = session.getNode("/gateway/startPoints");
                    JCRNodeWrapper jcrNodeWrapper = node.addNode(startPointName,
                            "jnt:gmailstartpoint");
                    jcrNodeWrapper.setProperty("uri", uri);
                    jcrNodeWrapper.setProperty("password", password);
                    session.save();
                    return null;
                }
            });
            
            logger.info("Registering route start point: {}", uri);
            
            gatewayService.getRouteStartPoints().put(startPointName, this);
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public ProcessorDefinition startRoute(RouteBuilder routeBuilder) {
        return routeBuilder.from(getInterpolatedUri());
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{uri='").append(uri).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
