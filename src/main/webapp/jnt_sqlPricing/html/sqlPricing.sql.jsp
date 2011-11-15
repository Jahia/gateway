<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%--
  Created by IntelliJ IDEA.
  User: rincevent
  Date: 11/15/11
  Time: 11:28 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<sql:query var="pricings" dataSource="jdbc/testdb">
    select * from pricing;
</sql:query>
<jsp:useBean id="currentDate" class="java.util.Date"/>
<span>${currentNode.properties["jcr:title"].string} : <fmt:formatDate type="both" value="${currentDate}" dateStyle="long" timeStyle="medium"/></span>

<div>
    <ol>
        <c:forEach items="${pricings.rows}" var="pricing">
            <li>${pricing.name} = ${pricing.price}€</li>
        </c:forEach>
    </ol>
</div>