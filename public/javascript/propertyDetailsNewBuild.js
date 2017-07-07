
function showHideIsNewBuild() {
    var displayTrueDiv = $('#isNewBuild-true-hidden');
    var displayFalseDiv = $('#isNewBuild-false-hidden');
    var yesSelected = $('#isNewBuild-true');
    var noSelected = $('#isNewBuild-false');

    var newBuildFields = [
        $('#newBuildDate-day'),
        $('#newBuildDate-month'),
        $('#newBuildDate-year'),
        $('#newBuildValue')
    ];

    var notNewBuildFields = [
        $('#notNewBuildDate-day'),
        $('#notNewBuildDate-month'),
        $('#notNewBuildDate-year'),
        $('#notNewBuildValue')
    ]

    displayTrueDiv.hide();
    displayFalseDiv.hide();

    if(yesSelected.is(':checked')) {
        displayTrueDiv.show();
    }
    if(noSelected.is(':checked')) {
        displayFalseDiv.show();
    }
    $('input[type=radio][name=isNewBuild]').change(function(){
        if(this.value == 'true') {
            displayTrueDiv.show();
            displayFalseDiv.hide();
            clearInputFields(notNewBuildFields)
        } else {
            displayTrueDiv.hide();
            displayFalseDiv.show();
            clearInputFields(newBuildFields)
        }
    });
}

$(document).ready(function(){
    showHideIsNewBuild();
});
