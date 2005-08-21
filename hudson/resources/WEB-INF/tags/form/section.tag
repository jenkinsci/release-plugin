<%--
  Section header
--%>
<%@attribute name="title" required="true" %>
<%@ taglib prefix="f" tagdir="/WEB-INF/tags/form" %>

<f:block>
  <div style="font-weight:bold; border-bottom: 1px solid black; margin-bottom:0.2em; margin-top:0.4em">
    ${title}
  </div>
</f:block>
<jsp:doBody />
