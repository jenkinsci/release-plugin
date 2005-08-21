<%--
  Outer-most tag that forms a form tag.
--%>
<%@attribute name="name" required="true" %>
<%@attribute name="title" required="true" %>
<%@attribute name="checked" required="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" tagdir="/WEB-INF/tags/form" %>
<c:set var="oe_id" value="${oe_id+1}" scope="request" />

<tr id="oe_s${oe_id}"><%-- this ID marks the beginning --%>
  <td colspan="2">
    <script>
        function toggle${oe_id}() {
          var tbl = document.getElementById('oe_s${oe_id}').parentNode;
          var i = false;
          var o = false;

          for( j=0; tbl.rows[j]; j++ ) {
            n = tbl.rows[j];

            if(n.id=="oe_e${oe_id}")
              o = true;

            if( i && !o ) {
              if( n.style.display!="none" )
                n.style.display = "none";
              else
                n.style.display = "";
            }

            if(n.id=="oe_s${oe_id}")
              i = true;
          }
        }
    </script>
    <input type="checkbox" name="${name}" onclick="javascript:toggle${oe_id}()"
      <c:if test="${checked=='true'}">checked</c:if>>
    ${title}
  </td>
</tr>
<jsp:doBody />
<%-- end marker --%>
<tr id="oe_e${oe_id}" style="display:none">
  <c:if test="${checked=='false'}">
    <script>
      toggle${oe_id}();
    </script>
  </c:if>
</tr>