<%--
  see tasks.tag
--%>
<%@attribute name="href" required="true" %>
<%@attribute name="icon" required="true" %>
<%@attribute name="title" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<div class="task">
  <a href="${href}">
    <img width="20" height="20" style="margin: 2px;" src="${rootURL}/${icon}"
  ></a>
  <a href="${href}">
    <c:choose>
      <c:when test="${f:endsWith(pageContext.request.requestURL,href)}">
        <b>${title}</b>
      </c:when>
      <c:otherwise>
        ${title}
      </c:otherwise>
    </c:choose>
  </a>
</div>
