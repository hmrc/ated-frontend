function showHideOwnedBefore2012() {
    var displayTrueDiv = $('#isOwnedBefore2012-true-hidden');
    var displayFalseDiv = $('#isOwnedBefore2012-false-hidden');
    var yesSelected = $('#isOwnedBefore2012-true');
    var noSelected = $('#isOwnedBefore2012-false');
    var ownedBefore2012Value = $('#ownedBefore2012Value');

    var isNewBuildFalse = $('#isNewBuild-false');
    var isNewBuildTrue = $('#isNewBuild-true');

    var newBuildDateDay = $('#newBuildDate-day');
    var newBuildDateMonth = $('#newBuildDate-month');
    var newBuildDateYear = $('#newBuildDate-year');

    var notNewBuildDateDay = $('#notNewBuildDate-day');
    var notNewBuildDateMonth = $('#notNewBuildDate-month');
    var notNewBuildDateYear = $('#notNewBuildDate-year');

    var newBuildValue = $('#newBuildValue');
    var notNewBuildValue = $('#notNewBuildValue');

    var displayIsNewBuildFalseDiv = $('#isNewBuild-false-hidden');
    var displayIsNewBuildTrueDiv = $('#isNewBuild-true-hidden');

    displayTrueDiv.hide();
    displayFalseDiv.hide();

    if(yesSelected.is(':checked')) {
        displayTrueDiv.show();
    }
    if(noSelected.is(':checked')) {
        displayFalseDiv.show();
    }
    $('input[type=radio][name=isOwnedBefore2012]').change(function(){
        if(this.value == 'true') {
            displayTrueDiv.show();
            displayFalseDiv.hide();
        } else {
            displayTrueDiv.hide();
            displayFalseDiv.show();
            ownedBefore2012Value.val("");
        }
    });
}


$(document).ready(function(){
    showHideOwnedBefore2012();
});
