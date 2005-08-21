<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Command">
  <textarea class="setting-input" name="shell"
    rows="5" style="width:100%">${builder.command}</textarea>
</s:entry>
