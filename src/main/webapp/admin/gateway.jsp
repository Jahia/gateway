<%--
  Created by IntelliJ IDEA.
  User: rincevent
  Date: 09/11/11
  Time: 9:02 AM
  To change this template use File | Settings | File Templates.
--%>
<%@include file="/admin/include/header.inc" %>
<%@ page import="org.jahia.bin.JahiaAdministration" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<utility:setBundle basename="JahiaGateway" var="gatewayBundle" templateName="Jahia Gateway" useUILocale="true"/>
<div id="topTitle">
    <h1>Jahia</h1>

    <h2 class="edit"><fmt:message key="label.gateway" bundle="${gatewayBundle}"/></h2>
</div>
<div id="main">
    <table style="width: 100%;" class="dex-TabPanel" cellpadding="0" cellspacing="0">
        <tbody>
        <tr>
            <td style="vertical-align: top;" align="left">
                <%@include file="/admin/include/tab_menu.inc" %>
            </td>
        </tr>
        <tr>
            <td style="vertical-align: top;" align="left" height="100%">
                <div class="dex-TabPanelBottom">
                    <div class="tabContent">
                        <jsp:include page="/admin/include/left_menu.jsp">
                            <jsp:param name="mode" value="server"/>
                        </jsp:include>
                        <div id="content" class="fit">
                            <div class="head headtop">
                                <div class="object-title">
                                    <fmt:message key="label.routes.list" bundle="${gatewayBundle}"/>
                                </div>
                            </div>
                            <div class="content-item-noborder">
                                <c:forEach items="${routes}" var="route" varStatus="status">
                                    <span>${status.count}&nbsp;:&nbsp;${route}</span><br/>
                                </c:forEach>
                            </div>
                            <div class="head headtop">
                                <div class="object-title">
                                    <fmt:message key="label.routes.startpoint.list" bundle="${gatewayBundle}"/>
                                </div>
                            </div>
                            <div class="content-item-noborder">
                                <%--@elvariable id="routeStartPoints" type="java.util.Map"--%>
                                <c:forEach items="${routeStartPoints}" var="route" varStatus="status">
                                    <span>${status.count}&nbsp;:&nbsp;${route}</span><br/>
                                </c:forEach>
                            </div>
                            <div class="head headtop">
                                <div class="object-title">
                                    <fmt:message key="label.routes.add" bundle="${gatewayBundle}"/>
                                </div>
                            </div>
                            <div class="content-item-noborder">
                                <form action="<%=JahiaAdministration.composeActionURL(request, response, "", "")%>">
                                    <input type="hidden" name="operation" value="addRoute"/>
                                    <input type="hidden" name="do" value="gateway"/>
                                    <input type="text" name="routeName" maxlength="32" size="16"/>
                                    <select name="startPointName">
                                        <c:forEach items="${routeStartPoints}" var="startPoint">
                                            <option value="${startPoint.key}">
                                                    ${startPoint.key}
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <select name="transformerName">
                                        <%--@elvariable id="transformers" type="java.util.Map"--%>
                                        <c:forEach items="${transformers}" var="transformer">
                                            <option value="${transformer.key}">
                                                    ${transformer.key}
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <select name="deserializerName">
                                        <%--@elvariable id="deserializers" type="java.util.Set"--%>
                                        <c:forEach items="${deserializers}" var="deserializer">
                                            <option value="${deserializer}">
                                                    ${deserializer}
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <input type="submit"/>
                                </form>
                            </div>
                            <div class="head headtop">
                                <div class="object-title">
                                    <fmt:message key="label.routes.startpoint.add" bundle="${gatewayBundle}"/>
                                </div>
                            </div>
                            <%--@elvariable id="formHandlers" type="java.util.Set"--%>
                            <c:forEach items="${formHandlers}" var="handler">
                                <jsp:include page="startpoints/${handler}.form.jsp"/>
                            </c:forEach>
                            <c:forEach items="${transformers}" var="transformer">
                                <jsp:include page="transformers/${transformer.key}.configuration.jsp">
                                    <jsp:param name="transformerName" value="${transformer.key}"/>
                                </jsp:include>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</div>
<div id="actionBar">
          <span class="dex-PushButton">
            <span class="first-child">
              <a class="ico-back"
                 href='<%=JahiaAdministration.composeActionURL(request,response,"displaymenu","")%>'><fmt:message
                      key="label.backToMenu"/></a>
            </span>
          </span>
    <%--
    <span class="dex-PushButton">
      <span class="first-child">
        <a class="ico-ok" href="#ok" onclick="document.jahiaAdmin.submit(); return false;"><fmt:message key="org.jahia.admin.saveChanges.label"/></a>
      </span>
    </span>
    --%>
</div>
</div>
<%@include file="/admin/include/footer.inc" %>