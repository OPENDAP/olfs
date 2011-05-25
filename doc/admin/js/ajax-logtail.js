/* an ajax log file tailer / viewer
 copyright 2007 john minnihan.
 http://freepository.com
 Released under these terms
 1. This script, associated functions and HTML code ("the code") may be used by you ("the recipient") for any purpose.
 2. This code may be modified in any way deemed useful by the recipient.
 3. This code may be used in derivative works of any kind, anywhere, by the recipient.
 4. Your use of the code indicates your acceptance of these terms.
 5. This notice must be kept intact with any use of the code to provide attribution.
 */

var request1 = createRequest();

function getLog(logURL) {

    document.getElementById("message").innerHTML = "Polling OLFS log...";
    var url = logURL;
    request1.open("GET", url, true);
    request1.onreadystatechange = updatePage;
    request1.send(null);
    startTail(url);
}



function startTail(tailURL) {
    t = setTimeout("getLog('"+tailURL+"')", 1000);
}

function stopTail() {
    clearTimeout(t);
    document.getElementById("message").innerHTML =
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";

}


function clearLogWindow() {
    document.getElementById("log").innerHTML="";
}



function updatePage() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            logDiv = document.getElementById("log");

            logDiv.innerHTML = request1.responseText



        } else
            alert("Error! Request status is " + request1.status);
    }
}