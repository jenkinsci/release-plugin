<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Config page
--%>
<%-- SCM config pane --%>
<s:section title="Source Code Management">
  <c:set var="scms" value="<%= hudson.scm.SCMManager.getSupportedSCMs() %>" />
  <c:forEach var="idx" begin="0" end="${f:length(scms)-1}">
    <c:set var="scmd" value="${scms[idx]}" />
    <s:radioBlock name="scm" value="${idx}" title="${scmd.displayName}" checked="${it.scm.descriptor==scmd}">
      <c:set var="scm" value="${it.scm.descriptor==scmd?it.scm:null}" scope="request" />
      <jsp:include page="${scmd.configPage}"/>
    </s:radioBlock>
  </c:forEach>
</s:section>


<%-- build config pane --%>
<s:section title="Build">
  <c:set var="builds" value="<%= hudson.tasks.BuildStep.BUILDERS %>" />
  <c:forEach var="idx" begin="0" end="${f:length(builds)-1}">
    <c:set var="bd" value="${builds[idx]}" />
    <s:optionalBlock name="builder${idx}"
      title="${bd.displayName}" checked="${it.builders[bd]!=null}">

      <c:set var="descriptor" value="${bd}" scope="request"  />
      <c:set var="builder" value="${it.builders[bd]}" scope="request"  />
      <jsp:include page="${bd.configPage}"/>
    </s:optionalBlock>
  </c:forEach>
</s:section>


<%-- publisher config pane --%>
<s:section title="Post-build Actions">
  <c:set var="publishers" value="<%= hudson.tasks.BuildStep.PUBLISHERS %>" />
  <c:forEach var="idx" begin="0" end="${f:length(publishers)-1}">
    <c:set var="pd" value="${publishers[idx]}" />
    <s:optionalBlock name="publisher${idx}" title="${pd.displayName}" checked="${it.publishers[pd]!=null}">
      <c:set var="publisher" value="${it.publishers[pd]}" scope="request"  />
      <jsp:include page="${pd.configPage}"/>
    </s:optionalBlock>
  </c:forEach>
</s:section>
