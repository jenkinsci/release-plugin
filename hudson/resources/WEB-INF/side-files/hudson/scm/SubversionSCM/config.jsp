<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="modules"
  description="
    URL of SVN module. Multiple URLs can be specified.
  ">
  <input class="setting-input" name="svn_modules"
    type="text" value="${scm.modules}">
</s:entry>
<s:entry title="Use update"
  description="
    If checked, Hudson will use 'svn update' whenever possible, making the build faster.
    But this causes the artifacts from the previous build to remain when a new build starts.">
  <input name="svn_use_update"
    type="checkbox"
    <c:if test="${scm.useUpdate}">checked</c:if>>
</s:entry>
