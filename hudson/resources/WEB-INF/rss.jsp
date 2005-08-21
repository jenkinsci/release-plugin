<%@ page contentType="text/xml;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="rootURL" value="${pageContext.request.rootPath}" scope="request" />

<feed version="0.3" xmlns="http://purl.org/atom/ns#">
  <title>${it.displayName} history</title>
  <link rel="alternate" type="text/html" href="${rootURL}/${it.url}"/>

  <c:choose>
    <c:when test="${fn:length(runs)==0}">
      <modified>2001-01-01T00:00:00Z</modified>
    </c:when>
    <c:otherwise>
      <modified>${build.timestampString2}</modified>
    </c:otherwise>
  </c:choose>
  <author>
    <name>Hudson</name>
  </author>

  <c:forEach var="build" items="${runs}" >
    <entry>
      <title>${build.parent.name} #${build.number} (${build.result})</title>
      <link rel="alternate" type="text/html"
        href="${rootURL}/${build.url}"/>
      <id>tag:${build.parent.name}:${build.id}</id>
      <issued  >${build.timestampString2}</issued>
      <modified>${build.timestampString2}</modified>
    </entry>
  </c:forEach>
</feed>