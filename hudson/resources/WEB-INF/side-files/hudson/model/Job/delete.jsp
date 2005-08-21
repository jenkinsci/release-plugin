<%-- Delete project --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <form method="get" action="doDelete">
    Are you sure about deleting the job?
    <input type="submit" value="Yes" />
  </form>
</l:main-panel>
<l:footer/>
