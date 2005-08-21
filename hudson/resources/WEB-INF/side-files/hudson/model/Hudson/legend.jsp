<%-- show the icon legend --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <table cellpadding=5>
    <tr><td>
      <img src="images/blue.gif">
      Last build was successful.
    </td></tr>
    <tr><td>
      <img src="images/blue_anim.gif">
      Last build was successful. A new build is in progress.
    </td></tr>
    <tr><td>
      <img src="images/red.gif">
      Last build failed.
    </td></tr>
    <tr><td>
      <img src="images/red_anim.gif">
      Last build failed. A new build is in progress.
    </td></tr>
  </table>

</l:main-panel>
<l:footer/>
