<%--
  Displays the console output
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Console Output
</t:buildCaption>
<pre><c:out value="${it.log}" escapeXml="true" /></pre>
</l:main-panel>
<l:footer/>