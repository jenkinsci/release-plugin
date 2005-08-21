<%--
  region of HTML with a title
--%>
<%@attribute name="title" required="true" %>
<%@attribute name="width" required="true" %>
<table class="pane">
  <tr><td class="pane-header" colspan="${width}">
    ${title}
  </td></tr>
  <jsp:doBody />
</table>
