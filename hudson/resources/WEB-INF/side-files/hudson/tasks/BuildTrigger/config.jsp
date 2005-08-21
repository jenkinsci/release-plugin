<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Projects to build"
  description="Multiple projects can be specified like 'abc, def'">
  <input class="setting-input" name="childProjects"
    type="text" value="${publisher.childProjects}">
</s:entry>
