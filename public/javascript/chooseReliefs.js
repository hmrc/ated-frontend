function showHideCheckBoxDates(checkBoxName, dateName) {
     var displayDiv = $('#hidden-' + checkBoxName + '-date');
     var checkBoxValue = $('#' + checkBoxName + ':checked').val();
     var dateFieldDay = $('#' + dateName + '-day');
     var dateFieldMonth = $('#' + dateName + '-month');
     var dateFieldYear = $('#' + dateName + '-year');

     displayDiv.hide();

     if(checkBoxValue == 'true') {
         displayDiv.show();
     }
     $('input[type=checkBox][name=' + checkBoxName + ']').change(function(){
         if(this.checked) {
             displayDiv.show();
         } else {
             displayDiv.hide();
         }
     });
}

$(document).ready(function(){
   showHideCheckBoxDates('rentalBusiness', 'rentalBusinessDate');
   showHideCheckBoxDates('openToPublic', 'openToPublicDate');
   showHideCheckBoxDates('propertyDeveloper', 'propertyDeveloperDate');
   showHideCheckBoxDates('propertyTrading', 'propertyTradingDate');
   showHideCheckBoxDates('lending', 'lendingDate');
   showHideCheckBoxDates('employeeOccupation', 'employeeOccupationDate');
   showHideCheckBoxDates('farmHouses', 'farmHousesDate');
   showHideCheckBoxDates('socialHousing', 'socialHousingDate');
   showHideCheckBoxDates('equityRelease', 'equityReleaseDate');
});
