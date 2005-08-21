<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="E-mail Notification">
  <s:entry title="SMTP server"
    description="Name of the mail server. Leave it empty to use the default server.">
    <input class="setting-input" name="mailer_smtp_server"
      type="text" value="${descriptor.smtpServer}">
  </s:entry>
  <s:block>
    <small>
      Additional settings can be given as system properties to the container.
      See <a href="http://java.sun.com/products/javamail/javadocs/overview-summary.html#overview_description">
      this document</a> for possible values and effects.
    </small>
  </s:block>
  <s:entry title="System Admin E-mail Address"
    description="Notification e-mails will be sent with this address in the from header">
    <input class="setting-input" name="mailer_admin_address"
      type="text" value="${descriptor.adminAddress}">
  </s:entry>
</s:section>
