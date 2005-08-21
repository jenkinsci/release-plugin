<%--
  History of runs.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<l:pane width="2" title="Build history">
  <c:forEach var="build" items="${it.builds}">
    <c:set var="link" value="${rootURL}/${it.url}build/${build.id}" />
    <tr class="build-row">
      <td nowrap="nowrap">
        <img width="16" height="16" src="${rootURL}/${build.buildStatusUrl}">&nbsp;
        #${build.number}
      </td>
      <td nowrap="nowrap">
        <span class="highlighted-build">
          <a class="tip" href="${link}">
            <i:formatDate value="${build.timestamp.time}" type="both" dateStyle="medium" timeStyle="medium"/>
            <!--span><%-- TODO: log --%></span-->
          </a>
        </span>
      </td>
    </tr>
    <c:if test="${build.building}">
      <c:set var="pos" value="${build.executor.progress}" />
      <c:if test="${pos>0}">
        <tr><td></td><td>
          <t:progressBar pos="${pos}"/>
        </td></tr>
      </c:if>
    </c:if>
  </c:forEach>
  <tr class=build-row>
    <td colspan=2 align=right>
      <a href="rssAll"><img src="${rootURL}/images/atom.png" border=0> for all runs</a>
    </td>
  </tr>
  <tr class=build-row>
    <td colspan=2 align=right>
      <a href="rssFailed"><img src="${rootURL}/images/atom.png" border=0> for failures</a>
    </td>
  </tr>
</l:pane>
