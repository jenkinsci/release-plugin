<%--
  Displays the toggle button to keep/unkeep the log file.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${it.parent.logRotator!=null}">
  <div style="float:right">
    <form method="get" action="toggleLogKeep">
      <c:if test="${it.keepLog}">
        <input type="submit" value="Don't keep this log forever"  />
      </c:if>
      <c:if test="${!it.keepLog}">
        <input type="submit" value="Keep this log forever"  />
      </c:if>
    </form>
  </div>
</c:if>
