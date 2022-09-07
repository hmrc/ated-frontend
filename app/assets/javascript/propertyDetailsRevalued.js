
function showHideRevalued() {
    var continueButton = $('#submit');
    var backToAtedButton = $('#back-to-ated');

     backToAtedButton.hide();

    $('input[type=radio][name=isPropertyRevalued]').change(function(){
        if(this.value == 'true') {
            continueButton.show();
            backToAtedButton.hide();
        } else {
            continueButton.hide();
            backToAtedButton.show();
        }
    });
}
$(document).ready(function(){
    showHideRevalued();
});
