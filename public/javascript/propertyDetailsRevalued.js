
function showHideRevalued() {
    var revaluedDateDay = $('#revaluedDate-day');
    var revaluedDateMonth = $('#revaluedDate-month');
    var revaluedDateYear = $('#revaluedDate-year');
    var partAcqDispDateDay = $('#partAcqDispDate-day');
    var partAcqDispDateMonth = $('#partAcqDispDate-month');
    var partAcqDispDateYear = $('#partAcqDispDate-year');
    var revaluedValue = $('#revaluedValue');
    var continueButton = $('#submit');
    var backToAtedButton = $('#back-to-ated');

     backToAtedButton.hide();

    $('input[type=radio][name=isPropertyRevalued]').change(function(){
        if(this.value == 'true') {
            continueButton.show();
            backToAtedButton.hide();
        } else {
            revaluedDateDay.val("");
            revaluedDateMonth.val("");
            revaluedDateYear.val("");
            revaluedValue.val("");
            partAcqDispDateDay.val("");
            partAcqDispDateMonth.val("");
            partAcqDispDateYear.val("");
            continueButton.hide();
            backToAtedButton.show();
        }
    });
}
$(document).ready(function(){
    showHideRevalued();
});
