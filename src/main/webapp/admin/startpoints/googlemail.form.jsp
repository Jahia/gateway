<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ page import="org.jahia.bin.JahiaAdministration" %>      -
<utility:setBundle basename="JahiaGateway" var="gatewayBundle" templateName="Jahia Gateway" useUILocale="true"/>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.routes.startpoint.add" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <form action="<%=JahiaAdministration.composeActionURL(request, response, "", "")%>">
        <input type="hidden" name="operation" value="addStartPoint"/>
        <input type="hidden" name="do" value="gateway"/>
        <input type="hidden" name="startPointType" value="googlemail"/>
        <input type="text" name="startPointName" maxlength="32" size="16"/>
        <input type="text" name="username" maxlength="64" size="16"/>
        <input type="password" name="password" maxlength="64" size="16"/>
        <input type="text" name="delay" maxlength="12" size="8" value="60000"/>
        <input type="submit"/>
    </form>
</div>