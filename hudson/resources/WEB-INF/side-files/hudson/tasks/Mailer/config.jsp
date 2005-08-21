<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Recipients"
  description="Whitespace-separated list of recipient addresses. E-mail will be sent when a build fails.">
  <input class="setting-input" name="mailer_recipients"
    type="text" value="${publisher.recipients}">
</s:entry>
