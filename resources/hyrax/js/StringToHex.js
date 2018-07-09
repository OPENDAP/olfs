function convertToHex(sourceStr) {
    var hex = '';
    for (i = 0; i < sourceStr.length; i++) {
        if (sourceStr.charCodeAt(i).toString(16).toUpperCase().length < 2) {
            hex += "0" + sourceStr.charCodeAt(i).toString(16).toUpperCase();
        } else {
            hex += sourceStr.charCodeAt(i).toString(16).toUpperCase();
        }
    }
    return hex;
}

function convertHexToASCII() {
    if (document.XSS.hexhtml.value != '') {
        var hexText = document.XSS.hexhtml.value;
        var testText = hexText.substring(3, hexText.length).split("&#x");
        var resultString = '';
        var sub = '';
        for (i = 0; i < testText.length; i++) {
            sub = testText[i].substring(testText[i].length - 3, testText[i].length - 1)
            if (sub.length < 2) {
                resultString += "%0" + sub;
                alert(sub, " - ", resultString);
            } else {
                resultString += "%" + sub;
            }
            document.XSS.ascii.value = unescape(resultString);
        }
    }
}
