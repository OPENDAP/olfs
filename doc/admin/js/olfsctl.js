/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Hyrax" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2011 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.

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



