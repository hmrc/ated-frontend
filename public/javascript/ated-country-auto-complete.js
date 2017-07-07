function trimCountry(countryVal) {
    var trimmedCountry = countryVal
    var position = countryVal.search(":");
    if (position > 0){
      trimmedCountry = countryVal.substring(0, position).trim();
    }
    return trimmedCountry
}

function createLabelForHiddenSelect(divId, fieldId, labelText) {
    var selectDiv = $('#' + divId)
    var newLabel = $("<label>").attr('for', fieldId).text(labelText).attr('class', "visuallyhidden");
    selectDiv.append(newLabel);
}


function countryCodeAutoComplete(selectId) {

////////////////////////////////////////////
(function( $ ) {
    $.widget( "custom.combobox", {
      _create: function() {
        this.wrapper = $( "<span>" )
//          .addClass( "custom-combobox" )
          .insertAfter( this.element );

        this.element.hide();
        this._createAutocomplete();
        this.element.attr("id", this.element.attr("id")+"_");
      },


      _createAutocomplete: function() {
        var selected = this.element.children( ":selected" ),
          value = selected.val() ? trimCountry(selected.text()): "";

        this.input = $( "<input>" )
          .appendTo( this.wrapper )
          .val( value )
          .attr( "title", "" )
          .attr( "id", this.element.attr("id") )
          .addClass( "custom-combobox-input ui-widget ui-widget-content ui-state-default ui-corner-left form-control" )
          .autocomplete({
            delay: 0,
            minLength: 2,
            source: $.proxy( this, "_source" )
          });

        this._on( this.input, {
          autocompleteselect: function( event, ui ) {
            ui.item.option.selected = true;
            this._trigger( "select", event, {
              item: ui.item.option
            });
          },

          autocompletechange: "_removeIfInvalid"
        });
      },

      _source: function( request, response ) {
        var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
        response( this.element.children( "option" ).map(function() {
          var text = $( this ).text();
          var trimmedCountry = trimCountry(text);
          if ( this.value && ( !request.term || matcher.test(text) ) )
            return {
              label: trimmedCountry,
              value: trimmedCountry,
              option: this
            };
        }) );
      },

      _removeIfInvalid: function( event, ui ) {

        // Selected an item, nothing to do
        if ( ui.item ) {
          return;
        }

        // Search for a match (case-insensitive)
        var value = this.input.val(),
          valueLowerCase = value.toLowerCase(),
          valid = false;
        this.element.children( "option" ).each(function() {
          if ( $( this ).text().toLowerCase() === valueLowerCase ) {
            this.selected = valid = true;
            return false;
          }
        });

        // Found a match, nothing to do
        if ( valid ) {
          return;
        }

        // Remove invalid value
        this.input
          .val( "" )
          .attr( "title", value + " didn't match any item" );
        this.element.val( "" );
        this.input.autocomplete( "instance" ).term = "";
      },

      _destroy: function() {
        this.wrapper.remove();
        this.element.show();
      }
    });
  })( jQuery );
///////////////////////////////////////////
$(function() {
    $(selectId).combobox();
});

}
