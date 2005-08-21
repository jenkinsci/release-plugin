<%-- report a login error --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <div style="margin: 2em;">
    <%-- login form --%>
    <form action="j_security_check" method="post" style="text-size:smaller">
      <table>
        <tr>
          <td>User:</td>
          <td><input type="text" name="j_username" /></td>
        </tr>
        <tr>
          <td>Password</td>
          <td><input type="password" name="j_password" /></td>
        </tr>
      </table>
      <input type="submit" value="login" />
    </form>
  </div>
</l:main-panel>
<l:footer/>
