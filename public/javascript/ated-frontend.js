


function showElementOnClick(clickableId, elementToShowId) {
    document.getElementById(clickableId).onclick = function() {
        document.getElementById(elementToShowId).style.display = 'inline-block';
    }
}

function hideElementOnClick(clickableId, elementToHideId) {
    document.getElementById(clickableId).onclick = function() {
        document.getElementById(elementToHideId).style.display = 'none';
        }
}

function autoHideElement(elementToHideId) {
    document.getElementById(elementToHideId).style.display='none';
}

function showHideElements(btn_Yes, btn_No, element_No, element_Yes) {

    var btnYes = document.getElementById(btn_Yes);
    var btnNo = document.getElementById(btn_No);
    var elemYes = document.getElementById(element_No);
    var elemNo = document.getElementById(element_Yes);

    btnYes.onclick = function() {
        elemYes.style.display = 'inline-block';
        elemNo.style.display = 'none'
     }

    btnNo.onclick = function() {
        elemYes.style.display = 'none';
        elemNo.style.display = 'inline-block'
    }

}

function toggleOnClick(rd_btn, element)
{
    var rdBtn = document.getElementById(rd_btn);
    var el = document.getElementById(element);

    rdBtn.onchange = function()
    {
        if (rdBtn.checked == true)
        {
            el.style.display = 'inline-block';
        }
        else if (rdBtn.checked == false)
        {
            el.style.display = 'none';
        }
    }
}

function toggleDate(btn, elementId) {

    $('#' + btn).click(function() {
    	$('#' + elementId).toggleClass('js-hidden');
    	return false;
    });

}