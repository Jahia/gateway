<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%--
  Created by IntelliJ IDEA.
  User: rincevent
  Date: 11/15/11
  Time: 11:28 AM
  To change this template use File | Settings | File Templates.
--%>
<jsp:useBean id="currentDate" class="java.util.Date"/>
<span>SQL - ${currentNode.properties["jcr:title"].string} : <fmt:formatDate type="both" value="${currentDate}"
                                                                      dateStyle="long" timeStyle="medium"/></span>
<c:set var="pricing" value="${currentNode.properties['priceRef'].node}"/>
<sql:query var="pricings" dataSource="jdbc/testdb">
    select * from pricing where name='${pricing.properties['name'].string}';
</sql:query>
<p>
    ${currentNode.properties["description"].string}<br/>
    <span><c:forEach items="${pricings.rows}" var="price">${price.price}€</c:forEach></span>
</p>
<div>
    <img src="${currentNode.properties['image'].node.url}" width="260px" height="200px"/>
</div>
