function showHideEmailFunc() {

  var checkBoxValue = $('#' + "emailConsent-true" + ':checked').val();

  var editEmail = $("#contact-address-form");

  var email = $('#emailAddress');

  editEmail.hide();

  if(checkBoxValue == 'true') {
    editEmail.show();
  }

  $('input[type=radio][name=emailConsent]').change(function(){
    if(this.value == 'true') {
      editEmail.show();
    } else {
      editEmail.hide();
      email.val("");
    }
  });
}

$(document).ready(function() {
  showHideEmailFunc();
});