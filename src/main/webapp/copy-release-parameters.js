document.querySelectorAll('.release-copy-parameter').forEach(element => element.addEventListener('click', useReleaseParameters))

function useReleaseParameters(event) {
  const element = event.target
  clearFields();
  const releaseParametersArray = element.closest('td').getElementsBySelector('div.jenkins-form-item');
  for (let i = 0; i < releaseParametersArray.length; i++) {
    if (typeof releaseParametersArray[i].getElementsBySelector('.setting-main')[0] != "undefined") {
      let fieldElement = releaseParametersArray[i].getElementsBySelector('.setting-main')[0];
      const valueElement = fieldElement.getElementsBySelector("input")[0];
      let nameElement = releaseParametersArray[i].getElementsBySelector('.jenkins-form-label')[0];
      if (typeof nameElement == "undefined") {
        if (typeof releaseParametersArray[i].getElementsBySelector('.jenkins-checkbox')[0] != "undefined") {
          nameElement = releaseParametersArray[i].getElementsBySelector('.jenkins-checkbox')[0].getElementsBySelector('label')[0];
        }
      }
      if (typeof nameElement != "undefined") {
        const fieldName = nameElement.innerHTML;
        setFieldValue(fieldName, valueElement);
      }
    }
  }
}

function setFieldValue(pName, pValueElement) {
  let inputElement = $$('form[action="submit"] input[value="' + pName + '"]')[0];
  if (inputElement) {
    if (pValueElement.getAttribute('type') === "text") {
      //Text field
      const fieldValue = pValueElement.getAttribute('value');
      if (inputElement.next().tagName === "SELECT") {
        clearChoice(pName);
        inputElement.next('select').getElementsBySelector('option[value="' + fieldValue + '"]')[0].selected = "selected";
      } else {
        inputElement.next('input').value = fieldValue;
      }
    } else if (pValueElement.getAttribute('type') === "checkbox") {
      //Boolean parameter
      let fieldValue = pValueElement.getAttribute('checked');
      if (fieldValue == null) {
        fieldValue = false;
      }
      inputElement.next('.jenkins-checkbox').getElementsBySelector('input')[0].checked = fieldValue;
    }
  }
}

function clearChoice(pName) {
  const optionsArray = $$('form[action="submit"] input[value="' + pName + '"]')[0].next('select').getElementsBySelector('option');
  optionsArray.each(function (item) {
    item.removeAttribute("selected");
  });
}

function clearFields() {
  const inputFields = $$('form[action="submit"] .jenkins-input');
  inputFields.each(function (item) {
    item.clear();
  });
}
