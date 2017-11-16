$(document).ready(function(){
    showHideBankDetails();
});

function showHideBankDetails() {

    $('input[type=radio][name=hasUKBankAccount]').change(function(){
        if(this.value == 'true') {
            $('#non-uk-accountName').val("");
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
