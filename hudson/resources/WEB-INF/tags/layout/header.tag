<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="rq" uri="http://jakarta.apache.org/taglibs/request-1.0" %>
<%@attribute name="title" required="true" %>
<%--
    defines the header
--%>
<c:set var="rootURL" value="${pageContext.request.contextPath}" scope="request" />
<html>
<head>
  <title>${title}</title>
  <link rel="stylesheet" href="${rootURL}/css/style.css" type="text/css">
  <link rel="stylesheet" href="${rootURL}/css/color.css" type="text/css">
  <meta name="ROBOTS" content="INDEX,NOFOLLOW">
  <c:if test="${param.auto_refresh}">
    <meta http-equiv="Refresh" content="10">
  </c:if>
</head>
<body>
<table id="header" cellpadding="0" cellspacing="0" width="100%" border="0">
  <tr>
    <td id="top-panel" colspan="2">
      <table cellpadding="0" cellspacing="0" width="100%" border="0">
        <tr><td style="font-weight:bold; font-size: 2em;">
          <a href="${rootURL}/"><img src="${rootURL}/images/title.png" alt="title" /></a>
        </td><td>
          <%--form action="search">
            <div id="searchform">
              <img width="24" height="24" src="${rootURL}/images/24x24/find.gif"/>
              <b>Search:</b>
              <input name="search" size="12" value=""/>
              <input type="submit" value="Go"/> &nbsp;
            </div>
          </form--%>
          <c:if test="${app.useSecurity}">
            <rq:isUserInRole role="admin" value="false">
              <a href="${rootURL}/loginEntry"><b>login</b></a>
            </rq:isUserInRole>
            <rq:isUserInRole role="admin">
              <a href="${rootURL}/logout"><b>logout</b></a>
            </rq:isUserInRole>
          </c:if>
        </td></tr>
      </table>
    </td>
  </tr>

  <tr id="top-nav">
    <td id="left-top-nav">
      <c:forEach var="anc" items="${pageContext.request.ancestors}">
        <c:if test="${anc.prev!=null}">
          &gt;
        </c:if>
        <a href="${anc.url}">
          ${anc.object.displayName}
        </a>
      </c:forEach>
    </td>
    <td id="right-top-nav">
      <span class="smallfont">
        <c:choose>
          <c:when test="${param.auto_refresh}">
            <a href="?auto_refresh=false">DISABLE AUTO REFRESH</a>
          </c:when>
          <c:otherwise>
            <a href="?auto_refresh=true">ENABLE AUTO REFRESH</a>
          </c:otherwise>
        </c:choose>
      </span>
    </td>
  </tr>
</table>
