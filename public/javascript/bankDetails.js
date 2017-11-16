$(document).ready(function(){
    showHideBankDetails();
});

function showHideBankDetails() {

    $('input[type=radio][name=hasUKBankAccount]').change(function(){
        if(this.value == 'true') {
            $('#iban').val("");
            $('#bicSwiftCode').val("");
        } else {
            $('#sortCode_firstElement').val("");
            $('#sortCode_secondElement').val("");
            $('#sortCode_thirdElement').val("");
            $('#accountNumber').val("");
        }
    });

}
