<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%--
  Config page. derived class specific entries should go to configure-entries.jsp
--%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <s:form method="post" action="configSubmit">
    <s:entry title="Project name">
      ${it.name}
    </s:entry>
    <s:entry title="Description"
      description="You can use any HTML tag.">
      <textarea class="setting-input" name="description"
        rows="5" style="width:100%">${it.description}</textarea>
    </s:entry>
    <s:entry title="Next builder number">
      <input class="setting-input" name="buildNumber"
        type="text"  value="${it.nextBuildNumber}" />
    </s:entry>

    <%-- log rotator --%>
    <s:optionalBlock name="logrotate"
      title="Log Rotation" checked="${it.logRotator!=null}">
      <jsp:include page="../../tasks/LogRotator/config.jsp"/>
    </s:optionalBlock>

    <%-- additional entries from derived classes --%>
    <st:include page="configure-entries.jsp" />

    <s:block>
      <input type="submit" name="Submit" value="Save" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>

