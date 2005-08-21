<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="Shell">
  <s:entry title="Shell executable">
    <input class="setting-input" name="shell"
      type="text" value="${descriptor.shell}">
  </s:entry>
</s:section>
