$(document).ready(function(){
        var arn = $('#arn-hidden');
        var atedRefNo = $('#atedRefNo-hidden');
        arn.hide();
        if($('#agent').prop('checked')) {
            arn.show();
            atedRefNo.hide();
        }
        $('#agent').change(function(){
            if($('#agent').prop('checked')) {
                $('#arn').val("");
                arn.show();
                $('#atedRefNo').val("")
                atedRefNo.hide();
            } else {
                $('#arn').val("");
                arn.hide();
                atedRefNo.show();
                $('#atedRefNo').val("")
            }
        });
});
