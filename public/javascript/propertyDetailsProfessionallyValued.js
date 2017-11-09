
function showHideValuedByAgent() {
    var displayTrueDiv = $('#isValuedByAgent-true-hidden');
    var yesSelected = $('#isValuedByAgent-true');

    var fieldsArray = [
        $('#valuationDate-day'),
        $('#valuationDate-month'),
        $('#valuationDate-year')
    ];

    displayTrueDiv.hide();

    if (yesSelected.is(':checked')) {
        displayTrueDiv.show();
    }

    $('input[type=radio][name=isValuedByAgent]').change(function () {
        if (this.value == 'true') {
            displayTrueDiv.show();
        } else {
            displayTrueDiv.hide();
            clearInputFields(fieldsArray);
        }
    });
}

function clearInputFields(fieldsArray) {
    for (var i = 0; i < fieldsArray.length; i++) {
        fieldsArray[i].val("");
    }
}

$(document).ready(function(){
    showHideValuedByAgent();
});
