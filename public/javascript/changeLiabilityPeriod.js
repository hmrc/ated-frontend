function showHideAvoidanceScheme() {
    var displayTrueDiv = $('#isTaxAvoidance-true-hidden');
    var yesSelected = $('#isTaxAvoidance-true');
    var avoidanceRefNumber = $('#taxAvoidanceScheme');

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
        }
    });
}

function maxCharsInSupportingInfoBox() {
    var supportingInfoBox = $('#supportingInfo');

    supportingInfoBox.keyup(function(){
        var cr = 200 - supportingInfoBox.val().length
        $('#supportingInfo_chars').text(cr)
    });

}

$(document).ready(function(){
   showHideAvoidanceScheme();
   maxCharsInSupportingInfoBox();
});
