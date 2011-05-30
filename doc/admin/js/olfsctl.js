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

var logUrl;

function getOlfsLog(olfsCtlApi) {

    logUrl = olfsCtlApi;

    var logLines = document.getElementById("logLines").value;

    if(logLines=="all")
        logLines=0;

    var url = logUrl+"?cmd=getLog&lines="+logLines;

    //alert(url);

    var d = new Date();
    var status = d.toTimeString() + " Polling log: <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    request1.open("GET", url, true);
    request1.onreadystatechange = updateLoggerPage;
    request1.send(null);
    stopUpdatingLogView = false;
}



function startTailing(tailURL) {
    if(!stopUpdatingLogView)
        t = setTimeout("getOlfsLog('"+tailURL+"')", 1000);
}

function stopTailing() {
    stopUpdatingLogView = true;
    clearTimeout(t);
    var d = new Date();
    var status =  d.toTimeString() + " "+
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";


    document.getElementById("status").innerHTML = status;

}


function clearLogWindow() {
    document.getElementById("log").innerHTML="";
}



function updateLoggerPage() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            logDiv = document.getElementById("log");

            logDiv.innerHTML = "<pre>"+request1.responseText+"</pre>";


            startTailing(logUrl);



        } else
            alert("Error! Request status is " + request1.status);
    }
}



function setLogLevel(olfsCtlApi){


    var loggerName = document.getElementById("loggerName").value;
    var logLevel = document.getElementById("logLevel").value;

    var url = olfsCtlApi+"?cmd=setLogLevel&logger="+loggerName+"&level="+logLevel;

    //alert(url);

    var d = new Date();
    var status = d.toTimeString() + " Setting "+loggerName+" log level to "+logLevel+".   <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    request1.open("GET", url, true);
    request1.onreadystatechange = updateStatus;
    request1.send(null);

}

function updateLevelSelection(olfsCtlApi) {


    var loggerName = document.getElementById("loggerName").value;

    var url = olfsCtlApi+"?cmd=getLogLevel&logger="+loggerName;


    var d = new Date();
    var status = d.toTimeString() + " Getting "+loggerName+" log level.   <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    request1.open("GET", url, true);
    request1.onreadystatechange = updateLoggerLevel;
    request1.send(null);
}



function updateLoggerLevel() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            var logLevel = document.getElementById("logLevel");

            logLevel.value = request1.responseText;

        } else
            alert("Error! Request status is " + request1.status);
    }
}

function updateStatus() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            logDiv = document.getElementById("status");

            logDiv.innerHTML = "<pre>"+request1.responseText+"</pre>";

        } else
            alert("Error! Request status is " + request1.status);
    }
}



