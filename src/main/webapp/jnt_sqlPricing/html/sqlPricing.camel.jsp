<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%--
  Created by IntelliJ IDEA.
  User: rincevent
  Date: 11/15/11
  Time: 11:28 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<jcr:node path="/sites/systemsite/contents/sqls" var="pricings"/>
<jsp:useBean id="currentDate" class="java.util.Date"/>
<span>${currentNode.properties["jcr:title"].string} : <fmt:formatDate type="both" value="${currentDate}" dateStyle="long" timeStyle="medium"/></span>

<div>
    <ol>
        <c:forEach items="${pricings.nodes}" var="pricing">
            <template:addCacheDependency node="${pricing}"/>
            <li>${pricing.properties["name"].string} = ${pricing.properties["price"].string}€</li>
        </c:forEach>
    </ol>
</div>