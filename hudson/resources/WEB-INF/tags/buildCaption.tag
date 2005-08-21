<%--
    displays a caption for build/externalRun.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<h1>
  <c:if test="${it.building}">
    <c:set var="pos" value="${it.executor.progress}" />
    <c:if test="${pos>0}">
      <div style="float:right">
        <table><tr>
          <td>
            Progress:
          </td><td>
            <t:progressBar pos="${pos}"/>
          </td>
        </tr></table>
      </div>
    </c:if>
  </c:if>
  
  <img src="buildStatus" width=32 height=32>
  <jsp:doBody />
</h1>
