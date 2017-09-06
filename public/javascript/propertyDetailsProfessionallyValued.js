
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
        yesSelected.attr('aria-expanded', 'true')
        displayTrueDiv.show();
    }

    $('input[type=radio][name=isValuedByAgent]').change(function () {
        if (this.value == 'true') {
            yesSelected.attr('aria-expanded', 'true')
            displayTrueDiv.show();
        } else {
            yesSelected.attr('aria-expanded', 'false')
            displayTrueDiv.hide();
            clearInputFields(fieldsArray)
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
