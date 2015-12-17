<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<span>${currentNode.properties["jcr:title"].string}</span>

<div>
    ${currentNode.properties["note"].string}
</div>
<c:forEach items="${currentNode.nodes}" var="files">
    <template:module node="${files}"/>
</c:forEach>