<%-- Show files in the workspace --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <ul>
    <li><a href="..">..</a></li>
    <c:forEach var="f" items="${files}">
      <li><a href="${f.name}">${f.name}</a>
    </c:forEach>
  </ul>
</l:main-panel>
<l:footer/>
