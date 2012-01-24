

/**
 * Creates an XMLHttpRequest if possible.
 *
 * <ul>
 *     <li>1st Fall Back: ActiveXObject("Msxml2.XMLHTTP");  </li>
 *     <li>2nd Fall Back: ActiveXObject("Microsoft.XMLHTTP");  </li>
 *     <li> Other wise returns <code>null</code> </li>
 * </ul>
 *
 *
 * Copied from http://commavee.com/2007/04/13/ajax-logfile-tailer-viewer/ no clear attribution proivided.
 */
function createRequest() {
    var request = null;
    try {
        request = new XMLHttpRequest();
    } catch (trymicrosoft) {
        try {
            request = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (othermicrosoft) {
            try {
                request = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (failed) {
                request = null;
            }
        }
    }

    if (request == null) {
        alert("Error creating request object!");
    } else {
        return request;
    }
}

