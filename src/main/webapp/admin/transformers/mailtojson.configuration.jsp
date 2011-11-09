<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ page import="org.jahia.bin.JahiaAdministration" %>
<utility:setBundle basename="JahiaGateway" var="gatewayBundle" templateName="Jahia Gateway" useUILocale="true"/>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.mailtojson.configure" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.mailtojson.configure.regexp.list" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <%--@elvariable id="transformer" type="org.jahia.modules.gateway.MailToJSON"--%>
    <%--@elvariable id="transformers" type="java.util.Map"--%>
    <c:set var="transformer" value="${transformers[param.transformerName]}"/>
    <c:forEach items="${transformer.regexps}" var="regexp" varStatus="status">
        <span>${status.count}&nbsp;:&nbsp;${regexp}</span><br/>
    </c:forEach>
</div>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.mailtojson.configure.decoders.path.list" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <ol>
        <c:forEach items="${transformer.decoders}" var="decoder" varStatus="status">
            <li>${decoder.key}</li>
            <ol>
                <c:forEach items="${decoder.value.paths}" var="mailDecoder" varStatus="mailStatus">
                    <li>${mailStatus.count}&nbsp;:&nbsp;${mailDecoder}</li>
                </c:forEach>
            </ol>
        </c:forEach>
    </ol>
</div>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.mailtojson.configure.regexp.add" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <form action="<%=JahiaAdministration.composeActionURL(request, response, "", "")%>">
        <input type="hidden" name="operation" value="configureTransformer"/>
        <input type="hidden" name="do" value="gateway"/>
        <input type="hidden" name="transformerType" value="mailtojson"/>
        <input type="hidden" name="mailtojsonOperation" value="addRegexp"/>
        <input type="text" name="regexp" maxlength="64" size="64"/>
        <input type="submit"/>
    </form>
</div>
<div class="head headtop">
    <div class="object-title">
        <fmt:message key="label.mailtojson.configure.decoder.path.add" bundle="${gatewayBundle}"/>
    </div>
</div>
<div class="content-item-noborder">
    <form action="<%=JahiaAdministration.composeActionURL(request, response, "", "")%>">
        <input type="hidden" name="operation" value="configureTransformer"/>
        <input type="hidden" name="do" value="gateway"/>
        <input type="hidden" name="transformerType" value="mailtojson"/>
        <input type="hidden" name="mailtojsonOperation" value="addPath"/>
        <select name="decoderName">
            <c:forEach items="${transformer.decoders}" var="decoder">
                <option value="${decoder.key}">
                        ${decoder.key}
                </option>
            </c:forEach>
        </select>
        <input type="text" name="pathName" maxlength="16" size="16"/>
        <input type="text" name="path" maxlength="64" size="32"/>
        <input type="submit"/>
    </form>
</div>