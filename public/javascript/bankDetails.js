$(document).ready(function(){
    showHideBankDetails();
    clearBankDetails();
});

function clearBankDetails() {

    $('input[type=radio][name=hasUKBankAccount]').change(function(){
        if(this.value == 'true') {
            $('#accountName').val("");
            $('#iban').val("");
            $('#bicSwiftCode').val("");
        } else {
            $('#accountName').val("");
            $('#sortCode_firstElement').val("");
            $('#sortCode_secondElement').val("");
            $('#sortCode_thirdElement').val("");
            $('#accountNumber').val("");
        }
    });

}

function showHideBankDetails() {
  var bankDetailsUK = $('#hidden-bank-details-uk');
  var bankDetailsNonUK = $('#hidden-bank-details-non-uk');
  var ele = $('input[type=radio]:checked')[0]["id"];

  switch(ele) {
    case "hasUKBankAccount-true":
      bankDetailsUK.show();
      bankDetailsNonUK.hide();
      break;
    case "hasUKBankAccount-false":
      bankDetailsUK.hide();
      bankDetailsNonUK.show();
      break;
  }
}

$('#hasUKBankAccount-id').on('click', showHideBankDetails)


