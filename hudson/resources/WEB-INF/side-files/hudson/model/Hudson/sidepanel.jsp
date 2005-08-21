<%--
  Side panel for the build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<l:header title="Hudson" />
<l:side-panel>
  <l:tasks>
    <l:isAdmin>
      <l:task icon="images/24x24/box_new.gif" href="newJob" title="New Job" />
      <l:task icon="images/24x24/wrench.gif" href="configure" title="Configure" />
      <l:task icon="images/24x24/gear_connection.gif" href="reload" title="Reload Config" />
    </l:isAdmin>
  </l:tasks>
  <t:queue />
  <t:executors />
</l:side-panel>
