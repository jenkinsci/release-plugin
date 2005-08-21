<%--
  Side panel for the build view.
--%>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:forEach var="action" items="${it.actions}" varStatus="i">
  <l:task icon="images/24x24/${action.iconFileName}" href="action?${i.index}" title="${action.title}" />
</c:forEach>
