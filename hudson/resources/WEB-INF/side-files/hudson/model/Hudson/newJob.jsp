<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  New Project page
--%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="createJob">
    <s:entry title="Job name">
      <input type="text" name="name" class="setting-input" />
    </s:entry>
    <s:entry title="Job type">
      <select name="type">
        <option value="hudson.model.Project">Building a software project</option>
        <option value="hudson.model.ExternalJob">Monitoring an external job</option>
      </select>
    </s:entry>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
