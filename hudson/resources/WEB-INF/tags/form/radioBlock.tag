<%--
  Radiobox with foldable details
--%>
<%@attribute name="name" required="true" %>
<%@attribute name="value" required="true" %>
<%@attribute name="title" required="true" %>
<%@attribute name="checked" required="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" tagdir="/WEB-INF/tags/form" %>
<c:set var="rb_id" value="${rb_id+1}" scope="request" />

<tr id="rb_s${rb_id}"><%-- this ID marks the beginning --%>
  <td colspan="2">
    <script>
        <%-- this is way too clumsy. there must be a better way to do it. --%>
        function showRb${rb_id}(show) {
          var tbl = document.getElementById('rb_s${rb_id}').parentNode;
          var i = false;
          var o = false;

          for( j=0; tbl.rows[j]; j++ ) {
            n = tbl.rows[j];

            if(n.id=="rb_e${rb_id}")
              o = true;

            if( i && !o ) {
              if( show )
                n.style.display = "";
              else
                n.style.display = "none";
            }

            if(n.id=="rb_s${rb_id}")
              i = true;
          }
        }

        function updateRb${rb_id}() {
          // update other radios
          var x = document.getElementById('Rb${rb_id}');
          col = x.form.${name};
          for(c=0;c<col.length;c++) {
            var item = col.item(c);
            eval("show"+item.id+"("+(item==x)+")");
          }
        }
    </script>
    <input type="radio" name="${name}" value="${value}" onclick="javascript:updateRb${rb_id}()" id="Rb${rb_id}"
      <c:if test="${checked=='true'}">checked</c:if>>
    ${title}
  </td>
</tr>
<jsp:doBody />
<%-- end marker --%>
<tr id="rb_e${rb_id}" style="display:none">
  <c:if test="${checked=='false'}">
    <script>
      showRb${rb_id}(false);
    </script>
  </c:if>
</tr>