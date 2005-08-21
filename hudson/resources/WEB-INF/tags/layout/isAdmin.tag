<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="rq" uri="http://jakarta.apache.org/taglibs/request-1.0" %>
<c:choose>
  <c:when test="${app.useSecurity}">
    <rq:isUserInRole role="admin">
      <jsp:doBody />
    </rq:isUserInRole>
  </c:when>
  <%-- if the security is disabled, show it anyway --%>
  <c:otherwise>
    <jsp:doBody />
  </c:otherwise>
</c:choose>
