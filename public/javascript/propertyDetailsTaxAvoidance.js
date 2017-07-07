
function showHideAvoidanceScheme() {
    var displayTrueDiv = $('#isTaxAvoidance-true-hidden');
    var yesSelected = $('#isTaxAvoidance-true');
    var avoidanceRefNumber = $('#taxAvoidanceScheme');
    var promoterRefNumber = $('#taxAvoidancePromoterReference');
    displayTrueDiv.hide();

    if(yesSelected.is(':checked')) {
        displayTrueDiv.show();
    }
    $('input[type=radio][name=isTaxAvoidance]').change(function(){
        if(this.value == 'true') {
            displayTrueDiv.show();
        } else {
            displayTrueDiv.hide();
            avoidanceRefNumber.val("");
            promoterRefNumber.val("");
        }
    });
}


$(document).ready(function(){
   showHideAvoidanceScheme();
});
