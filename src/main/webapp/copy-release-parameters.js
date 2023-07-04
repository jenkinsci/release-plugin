document.querySelectorAll('.release-copy-parameter').forEach(element => element.addEventListener('click', useReleaseParameters))

function useReleaseParameters(event) {
  const element = event.target
  clearFields();
  const releaseParametersArray = element.closest('td').querySelectorAll('div.jenkins-form-item');
  for (let i = 0; i < releaseParametersArray.length; i++) {
    if (typeof releaseParametersArray[i].querySelector('.setting-main') != "undefined") {
      let fieldElement = releaseParametersArray[i].querySelector('.setting-main');
      const valueElement = fieldElement.querySelector("input");
      let nameElement = releaseParametersArray[i].querySelector('.jenkins-form-label');
      if (!nameElement) {
        if (typeof releaseParametersArray[i].querySelector('.jenkins-checkbox') != "undefined") {
          nameElement = releaseParametersArray[i].querySelector('.jenkins-checkbox').querySelector('label');
        }
      }
      if (nameElement) {
        const fieldName = nameElement.innerHTML;
        setFieldValue(fieldName, valueElement);
      }
    }
  }
}

function setFieldValue(pName, pValueElement) {
  let inputElement = document.querySelector('form[action="submit"] input[value="' + pName + '"]');
  if (inputElement) {
    if (pValueElement.getAttribute('type') === "text") {
      //Text field
      const fieldValue = pValueElement.getAttribute('value');
      if (inputElement.nextElementSibling.tagName === "SELECT") {
        clearChoice(pName);
        inputElement.nextElementSibling.querySelector('option[value="' + fieldValue + '"]').selected = "selected";
      } else {
        inputElement.nextElementSibling.value = fieldValue;
      }
    } else if (pValueElement.getAttribute('type') === "checkbox") {
      //Boolean parameter
      let fieldValue = pValueElement.getAttribute('checked');
      if (fieldValue == null) {
        fieldValue = false;
      }
      inputElement.nextElementSibling.querySelector('input').checked = fieldValue;
    }
  }
}

function clearChoice(pName) {
  const result = document.querySelector('form[action="submit"] input[value="' + pName + '"]')
  const optionsArray = result.nextElementSibling.querySelectorAll('option');
  optionsArray.forEach(function (item) {
    item.removeAttribute("selected");
  });
}

function clearFields() {
  const inputFields = document.querySelectorAll('form[action="submit"] .jenkins-input');
  inputFields.forEach(function (item) {
    item.clear();
  });
}
