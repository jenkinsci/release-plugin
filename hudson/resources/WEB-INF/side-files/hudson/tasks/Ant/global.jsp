<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="Ant">
  <s:entry title="Ant installation"
    description="List of Ant installations on this system">
    <table width="100%">
      <c:set var="idx" value="0" />
      <c:forEach var="inst" items="${descriptor.installations}">
        <s:entry title="name">
          <input class="setting-input" name="ant_name${idx}"
            type="text" value="${inst.name}">
        </s:entry>
        <c:if test="${!inst.exists && inst.name!=''}">
          <c:set var="status" value="<span class=error>No such installation exists</span>" />
        </c:if>
        <s:entry title="ANT_HOME"
          description="${status}">
          <input class="setting-input" name="ant_home${idx}"
            type="text" value="${inst.antHome}">
        </s:entry>
        <c:set var="idx" value="${idx+1}" />
      </c:forEach>
    </table>
    <input type="submit" name="ant_add" value="Add new entry " />
    <c:if test="${idx!=0}">
      <input type="submit" name="ant_delete" value="Delete last entry" />
    </c:if>
  </s:entry>
</s:section>
