function maxCharsInSupportingInfoBox() {
    var supportingInfoBox = $('#supportingInfo');
    var charRemainLoad = 200 - supportingInfoBox.val().length
    $('#supportingInfo_chars').text(charRemainLoad)

    supportingInfoBox.keyup(function(){
        var charRemain = 200 - supportingInfoBox.val().length
        $('#supportingInfo_chars').text(charRemain)
    });

}

$(document).ready(function(){
   maxCharsInSupportingInfoBox();
});
