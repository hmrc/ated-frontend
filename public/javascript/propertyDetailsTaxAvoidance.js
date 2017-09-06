
function showHideAvoidanceScheme() {
    var displayTrueDiv = $('#isTaxAvoidance-true-hidden');
    var yesSelected = $('#isTaxAvoidance-true');
    var noSelected = $('#isTaxAvoidance-false');
    var avoidanceRefNumber = $('#taxAvoidanceScheme');
    var promoterRefNumber = $('#taxAvoidancePromoterReference');
    displayTrueDiv.hide();

    if(yesSelected.is(':checked')) {
        displayTrueDiv.show();
        yesSelected.attr('aria-expanded', 'true')
    }
    $('input[type=radio][name=isTaxAvoidance]').change(function(){
        if(this.value == 'true') {
        yesSelected.attr('aria-expanded', 'true')
            displayTrueDiv.show();

        } else {
        noSelected.attr('aria-expanded', 'false')
            displayTrueDiv.hide();
            avoidanceRefNumber.val("");
            promoterRefNumber.val("");
        }
    });
}


$(document).ready(function(){
   showHideAvoidanceScheme();
});
