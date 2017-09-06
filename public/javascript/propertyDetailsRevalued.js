
function showHideRevalued() {
    var displayTrueDiv = $('#isPropertyRevalued-true-hidden');
    var displayFalseDiv = $('#isPropertyRevalued-false-hidden');
    var yesSelected = $('#isPropertyRevalued-true');
    var noSelected = $('#isPropertyRevalued-false');
    var continueButton = $('#submit');

    var revaluedDateDay = $('#revaluedDate-day');
    var revaluedDateMonth = $('#revaluedDate-month');
    var revaluedDateYear = $('#revaluedDate-year');
    var revaluedValue = $('#revaluedValue');

    displayTrueDiv.hide();
    displayFalseDiv.hide();

    if(yesSelected.is(':checked')) {
        yesSelected.attr('aria-expanded', 'true')
        displayTrueDiv.show();
    }
    if(noSelected.is(':checked')) {
        ynoSelected.attr('aria-expanded', 'true')
        displayFalseDiv.show();
        continueButton.hide();
    }
    $('input[type=radio][name=isPropertyRevalued]').change(function(){
        if(this.value == 'true') {
            displayTrueDiv.show();
            displayFalseDiv.hide();
            continueButton.show();
        } else {
            displayTrueDiv.hide();
            displayFalseDiv.show();
            revaluedDateDay.val("");
            revaluedDateMonth.val("");
            revaluedDateYear.val("");
            revaluedValue.val("");
            continueButton.hide();
        }
    });
}

$(document).ready(function(){
    showHideRevalued();
});
