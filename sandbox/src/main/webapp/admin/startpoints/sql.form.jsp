<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ page import="org.jahia.bin.JahiaAdministration" %>
<utility:setBundle basename="JahiaGatewaySandbox" var="gatewayBundle" templateName="Jahia Gateway Sandbox" useUILocale="true"/>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.routes.startpoint.add.sql" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <form action="<%=JahiaAdministration.composeActionURL(request, response, "", "")%>">
        <input type="hidden" name="operation" value="addStartPoint"/>
        <input type="hidden" name="do" value="gateway"/>
        <input type="hidden" name="startPointType" value="sql"/>
        <input type="text" name="startPointName" maxlength="32" size="16"/>
        <input type="text" name="sql" maxlength="64" size="16"/>
        <input type="text" name="datasource" maxlength="64" size="16"/>
        <input type="text" name="frequency" maxlength="12" size="8" value="60"/>
        <input type="checkbox" name="update"/>
        <input type="submit"/>
    </form>
</div>