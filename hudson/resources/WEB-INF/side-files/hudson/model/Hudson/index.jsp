<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <div class="dashboard">
    <table id="projectstatus" class="pane">
      <tr>
        <th>&nbsp;</th>
        <th>Job</th>
        <th>Last Success</th>
        <th>Last Failure</th>
        <th>Last Duration</th>
        <th>&nbsp;</th>
        <l:isAdmin><%-- build icon --%>
          <th width=1>&nbsp;</th>
        </l:isAdmin>
      </tr>

    <c:forEach var="job" items="${app.jobs}">
      <c:set var="lsBuild" value="${job.lastSuccessfulBuild}" />
      <c:set var="lfBuild" value="${job.lastFailedBuild}" />
      <tr>
        <td>
          <img width="32" height="32" src="${rootURL}/${job.buildStatusUrl}" />
        </td>
        <td>
          <a href="job/${job.name}">
            ${job.name}
          </a>
        </td>
        <td>
          ${lsBuild!=null ? lsBuild.timestampString : "N/A"}
          <c:if test="${lsBuild!=null}">
            (<a href="${job.url}lastSuccessfulBuild">#${lsBuild.number}</a>)
          </c:if>
        </td>
        <td>
          ${lfBuild!=null ? lfBuild.timestampString : "N/A"}
          <c:if test="${lfBuild!=null}">
            (<a href="${job.url}lastFailedBuild">#${lfBuild.number}</a>)
          </c:if>
        </td>
        <td>
          ${lsBuild!=null ? lsBuild.durationString : "N/A"}
        </td>
        <td>
          &nbsp;
        </td>
        <l:isAdmin>
          <td>
            <a href="job/${job.name}/build"><img src="images/24x24/gears_run.gif" title="Schedule a build" border=0></a>
          </td>
        </l:isAdmin>
      </tr>
    </c:forEach>
  </table>
  <div align=right style="margin:1em">
      <a href="legend">Legend</a>
      &nbsp;&nbsp;&nbsp;&nbsp;
      <a href="rssAll"><img src="${rootURL}/images/atom.png" border=0> for all</a>
      &nbsp;&nbsp;&nbsp;&nbsp;
      <a href="rssFailed"><img src="${rootURL}/images/atom.png" border=0> for failures</a>
  </div>
</l:main-panel>
<l:footer/>
