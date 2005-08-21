<%-- report a login error --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <div style="margin: 2em; text-align:center; color:red; font-weight:bold">
    Invalid login information. Please try again.
    <br>
    <a href="loginEntry">Try again</a>
  </div>
</l:main-panel>
<l:footer/>
