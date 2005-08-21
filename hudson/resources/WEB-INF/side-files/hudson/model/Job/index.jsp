<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Project ${it.name}</h1>
  <div>
    <c:out value="${it.description}" escapeXml="false" />
  </div>

  <h2>Permalinks</h2>
  <ul>
    <c:if test="${it.lastBuild!=null}">
      <li>
        <a href="lastBuild">Last build
          (#${it.lastBuild.number}), ${it.lastBuild.timestampString} ago</a>
      </li>
    </c:if>
    <c:if test="${it.lastSuccessfulBuild!=null}">
      <li>
        <a href="lastSuccessfulBuild">Last successful build
          (#${it.lastSuccessfulBuild.number}), ${it.lastSuccessfulBuild.timestampString} ago</a>
      </li>
    </c:if>
    <c:if test="${it.lastFailedBuild!=null}">
      <li>
        <a href="lastFailedBuild">Last failed build
          (#${it.lastFailedBuild.number}), ${it.lastFailedBuild.timestampString} ago</a>
      </li>
    </c:if>
  </ul>
</l:main-panel>
<l:footer/>
