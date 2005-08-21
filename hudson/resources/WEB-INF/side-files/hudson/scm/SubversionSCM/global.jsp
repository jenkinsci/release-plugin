<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="Subversion">
  <s:entry title="svn executable"
    description="subversion executable">
    <input class="setting-input" name="svn_exe"
      type="text" value="${descriptor.svnExe}">
  </s:entry>
</s:section>