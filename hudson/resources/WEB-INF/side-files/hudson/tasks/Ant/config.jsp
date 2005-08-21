<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Ant Version">
  <select class="setting-input" name="ant_version">
    <option>(Default)</option>
    <c:forEach var="inst" items="${descriptor.installations}">
      <option <c:if test="${inst.name==builder.ant.name}">selected</c:if>>${inst.name}</option>
    </c:forEach>
  </select>
</s:entry>
<s:entry title="Targets">
  <input class="setting-input" name="ant_targets"
    type="text" value="${builder.targets}">
</s:entry>
