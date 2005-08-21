<%--
  One entry
--%>
<%@attribute name="title" required="true" %>
<%@attribute name="description" required="false" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<tr>
  <td class="setting-name">
    ${title}
  </td>
  <td>
    <jsp:doBody />
  </td>
</tr>
<c:if test="${description!=null}">
  <tr>
    <td>&nbsp;</td>
    <td class="setting-description">
      ${description}
    </td>
  </tr>
</c:if>