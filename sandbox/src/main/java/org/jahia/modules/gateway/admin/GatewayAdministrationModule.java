package org.jahia.modules.gateway.admin;

import org.jahia.admin.AbstractAdministrationModule;
import org.jahia.bin.Jahia;
import org.jahia.bin.JahiaAdministration;
import org.jahia.modules.gateway.CamelHandler;
import org.jahia.modules.gateway.ConfigurableCamelHandler;
import org.jahia.modules.gateway.GatewayService;
import org.jahia.modules.gateway.admin.forms.StartPointFormHandler;
import org.jahia.params.ProcessingContext;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.i18n.JahiaResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.util.Arrays;
import java.util.Map;

/**
 * This class manage the gateway module, allows to create, delete, update camel routes.
 * User: rincevent
 * Date: 09/11/11
 * Time: 8:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayAdministrationModule extends AbstractAdministrationModule {
    private GatewayService gatewayService;
    private static final String MODULE_NAME = "Jahia Gateway";
    private static final String BUNDLE_NAME = "JahiaGateway";
    public static final String FEATURE = "org.jahia.gateway";
    public static final String JSP_NAME = "admin/gateway.jsp";
    private JahiaTemplateManagerService templateManagerService;
    private Map<String, StartPointFormHandler> startPointFormHandlers;

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JahiaSite site = (JahiaSite) request.getSession().getAttribute(ProcessingContext.SESSION_SITE);
        request.setAttribute("site", site);
        request.setAttribute("uilocale", Jahia.getThreadParamBean().getUILocale());
        String operation = request.getParameter("operation");
        if (null == operation) {
            operation = "display";
        }
        if ("addStartPoint".equals(operation)) {
            String startPointType = request.getParameter("startPointType");
            String startPointName = request.getParameter("startPointName");
            startPointFormHandlers.get(startPointType).parseForm(request, startPointType, startPointName,
                    gatewayService);
        } else if ("addRoute".equals(operation)) {
            String startPointName = request.getParameter("startPointName");
            String transformerName = request.getParameter("transformerName");
            String deserializerName = request.getParameter("deserializerName");
            String routeName = request.getParameter("routeName");
            if (startPointName != null && transformerName != null && deserializerName != null && routeName != null) {
                gatewayService.addRoute(routeName, startPointName, Arrays.asList(transformerName, deserializerName));
            }
        } else if ("configureTransformer".equals(operation)) {
            String transformerType = request.getParameter("transformerType");
            CamelHandler camelHandler = gatewayService.getTransformers().get(transformerType);
            if (camelHandler instanceof ConfigurableCamelHandler) {
                ((ConfigurableCamelHandler) camelHandler).configure(request);
            }
        }
        request.setAttribute("routeStartPoints", gatewayService.getRouteStartPoints());
        request.setAttribute("routes", gatewayService.getRoutes());
        request.setAttribute("transformers", gatewayService.getTransformers());
        request.setAttribute("deserializers", gatewayService.getDeserializers().keySet());
        request.setAttribute("formHandlers", startPointFormHandlers.keySet());
        JahiaAdministration.doRedirect(request, response, request.getSession(), getModuleRoot() + "/" + JSP_NAME);
    }

    public void setGatewayService(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override
    public LocalizationContext getLocalizationContext() {
        return new LocalizationContext(new JahiaResourceBundle(BUNDLE_NAME, Jahia.getThreadParamBean().getUILocale(),
                MODULE_NAME), Jahia.getThreadParamBean().getUILocale());
    }

    @Override
    public String getIcon() {
        return Jahia.getContextPath() + getModuleRoot() + "/" + super.getIcon();
    }

    private String getModuleRoot() {
        return templateManagerService.getTemplatePackage(MODULE_NAME).getRootFolderPath();
    }

    @Override
    public String getIconSmall() {
        return Jahia.getContextPath() + getModuleRoot() + "/" + super.getIconSmall();
    }

    @Override
    public final boolean isEnabled(JahiaUser user, String siteKey) {
        return true;
    }

    public void setTemplateManagerService(JahiaTemplateManagerService templateManagerService) {
        this.templateManagerService = templateManagerService;
    }

    public void setStartPointFormHandlers(Map<String, StartPointFormHandler> startPointFormHandlers) {
        this.startPointFormHandlers = startPointFormHandlers;
    }
}
