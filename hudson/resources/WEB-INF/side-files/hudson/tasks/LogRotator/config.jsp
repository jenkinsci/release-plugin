<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Days to keep records"
  description="if not -1, log/artifacts etc are only kept up to this number of days">
  <input class="setting-input" name="logrotate_days"
    type="text" value="${it.logRotator.daysToKeep}">
</s:entry>
<s:entry title="Max # of records to keep"
  description="if not -1, only up to this number of log/artifacts etc are kept">
  <input class="setting-input" name="logrotate_nums"
    type="text" value="${it.logRotator.numToKeep}">
</s:entry>
