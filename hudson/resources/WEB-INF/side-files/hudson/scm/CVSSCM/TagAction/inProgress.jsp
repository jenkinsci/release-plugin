<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<l:header title="Tag the build" />
<l:side-panel />
<l:main-panel>
  <h1>Build #${it.build.number}</h1>
  <p>
    Tagging "<tt>${it.workerThread.tagName}</tt>" is in progress.
  </p>
  <pre><c:out value="${it.workerThread.log}" escapeXml="true" /></pre>
  <c:if test="${!it.workerThread.alive}">
    <form method="get" action="clearError">
      <input type=submit value="Clear error" />
    </form>
  </c:if>
</l:main-panel>
<l:footer/>