<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  Config page
--%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="configSubmit">
    <s:entry title="Home directory">
      ${it.rootDir}
    </s:entry>
    <s:entry title="# of executors">
      <input type="text" name="numExecutors" class="setting-input"
        value="${it.numExecutors}"/>
    </s:entry>
    <s:entry title="Enable security"
      description="If enabled, you have to log in before changing the configuration or running a new build.">
      <input type="checkbox" name="use_security"
        <c:if test="${it.useSecurity}">checked</c:if>>
    </s:entry>

    <%-- build config pane --%>
    <c:set var="builds" value="<%= hudson.tasks.BuildStep.BUILDERS %>" />
    <c:forEach var="idx" begin="0" end="${f:length(builds)-1}">
      <c:set var="descriptor" value="${builds[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <%-- SCM config pane --%>
    <c:set var="scms" value="<%= hudson.scm.SCMManager.getSupportedSCMs() %>" />
    <c:forEach var="idx" begin="0" end="${f:length(scms)-1}">
      <c:set var="descriptor" value="${scms[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <%-- build config pane --%>
    <c:set var="pubs" value="<%= hudson.tasks.BuildStep.PUBLISHERS %>" />
    <c:forEach var="idx" begin="0" end="${f:length(pubs)-1}">
      <c:set var="descriptor" value="${pubs[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
