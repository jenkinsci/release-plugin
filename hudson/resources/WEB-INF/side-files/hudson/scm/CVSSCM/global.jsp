<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="CVS">
  <s:entry title=".cvspass file"
    description=".cvspass file to load passwords from. Leave it empty to read from $HOME/.cvspass">
    <input class="setting-input" name="cvs_cvspass"
      type="text" value="${descriptor.cvspassFile}">
  </s:entry>
</s:section>