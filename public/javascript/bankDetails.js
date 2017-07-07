$(document).ready(function(){
    showHideBankDetails();
});

function showHideBankDetails() {
    var bankDetails = $('#hidden-bank-details');
    var bankDetailsUK = $('#hidden-bank-details-uk');
    var bankDetailsNonUK = $('#hidden-bank-details-non-uk');

    bankDetails.hide();
    bankDetailsUK.hide();
    bankDetailsNonUK.hide();

    $('input[type=radio][name=hasUKBankAccount]').change(function(){
        if(this.value == 'true') {
            bankDetails.show();
            bankDetailsUK.show();
            bankDetailsNonUK.hide();
            $('#iban').val("");
            $('#bicSwiftCode').val("");
        } else {
            bankDetails.show();
            bankDetailsUK.hide();
            bankDetailsNonUK.show();
            $('#sortCode_firstElement').val("");
            $('#sortCode_secondElement').val("");
            $('#sortCode_thirdElement').val("");
            $('#accountNumber').val("");
        }
    });

    if($('#hasUKBankAccount-true').attr('checked')) {
        bankDetails.show();
        bankDetailsUK.show();
        bankDetailsNonUK.hide();
    } else if($('#hasUKBankAccount-false').attr('checked')) {
        bankDetails.show();
        bankDetailsUK.hide();
        bankDetailsNonUK.show();
    }
}
