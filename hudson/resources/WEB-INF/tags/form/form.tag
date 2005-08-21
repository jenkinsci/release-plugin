<%--
  Outer-most tag that forms a form tag.
--%>
<%@attribute name="action" required="true" %>
<%@attribute name="method" required="true" %>
<%@attribute name="enctype" required="false" %>
<%@attribute name="name" required="false" %>
<%@attribute name="target" required="false" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<form action="${action}" method="${method}"
  <c:if test="${enctype!=null}">enctype="${enctype}"</c:if>
  <c:if test="${name!=null}">enctype="${name}"</c:if>
  <c:if test="${target!=null}">enctype="${target}"</c:if>
>
  <table width="100%">
    <jsp:doBody/>
  </table>
</form>