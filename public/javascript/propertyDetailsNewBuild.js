
function showHideIsNewBuild() {

    var newBuildFields = [
        $('#newBuildDate-day'),
        $('#newBuildDate-month'),
        $('#newBuildDate-year'),
        $('#localAuthRegDate-day'),
        $('#localAuthRegDate-month'),
        $('#localAuthRegDate-year'),
        $('#newBuildValue')
    ];


    var notNewBuildFields = [
        $('#notNewBuildDate-day'),
        $('#notNewBuildDate-month'),
        $('#notNewBuildDate-year'),
        $('#notNewBuildValue')
    ]

    $('input[type=radio][name=isNewBuild]').change(function(){
        if(this.value == 'true') {
            clearInputFields(notNewBuildFields);
        } else {
            clearInputFields(newBuildFields);
        }
    });
}

function clearInputFields(fieldsArray) {
    for (var i = 0; i < fieldsArray.length; i++) {
        fieldsArray[i].val("");
    }
}

$(document).ready(function(){
    showHideIsNewBuild();
});
