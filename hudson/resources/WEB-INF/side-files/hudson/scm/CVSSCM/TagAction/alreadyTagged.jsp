<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<l:header title="Tag the build" />
<l:side-panel />
<l:main-panel>
  <h1>Build #${it.build.number}</h1>
  <p>
    This build is already tagged as <tt>${it.tagName}</tt>
  </p>
  <pre><c:out value="${it.workerThread.log}" escapeXml="true" /></pre>
</l:main-panel>
<l:footer/>