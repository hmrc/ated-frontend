
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
        yesSelected.attr('aria-expanded', 'true')
    }
    if(noSelected.is(':checked')) {
        displayFalseDiv.show();
        noSelected.attr('aria-expanded', 'true')
    }
    $('input[type=radio][name=isNewBuild]').change(function(){
        if(this.value == 'true') {
            yesSelected.attr('aria-expanded', 'true')
            displayTrueDiv.show();
            displayFalseDiv.hide();
            clearInputFields(notNewBuildFields)
        } else {
            noSelected.attr('aria-expanded', 'true')
            displayTrueDiv.hide();
            displayFalseDiv.show();
            clearInputFields(newBuildFields)
            noSelected.attr('aria-expanded', 'true')
        }
    });
}

$(document).ready(function(){
    showHideIsNewBuild();
});
