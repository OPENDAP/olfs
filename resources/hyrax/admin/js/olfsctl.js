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



var stopUpdatingOlfsLogView = true;
var olfsLogTailTimer;
var olfsLoggingStarted = false;



function startOlfsLogTailing(olfsLogTailUrl) {

    if(!olfsLoggingStarted){
        // Make sure that log viewing is enabled.
        stopUpdatingOlfsLogView = false;
        olfsLoggingStarted = true;

        // Go get the log and start the log tailing cycle.
        getOlfsLog(olfsLogTailUrl);
    }
}


function stopOlfsLogTailing() {
    olfsLoggingStarted = false;
    stopUpdatingOlfsLogView = true;
    clearTimeout(olfsLogTailTimer);
    var d = new Date();
    var status =  d.toTimeString() + " "+
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";


    document.getElementById("status").innerHTML = status;

}


function clearOlfsLogWindow() {
    document.getElementById("log").innerHTML="";
}


function getOlfsLog(olfsLogTailUrl) {


    var olfsLogLines = document.getElementById("olfsLogLines").value;

    if(olfsLogLines=="all")
        olfsLogLines=0;

    var url = olfsLogTailUrl+"?cmd=getLog&lines="+olfsLogLines;

    //alert(url);

    var d = new Date();
    var status = d.toTimeString() + " Polling log: <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function(){ updateOlfsLoggerPage(request, olfsLogTailUrl) };
    request.send(null);
}



function updateOlfsLoggerPage(request, olfsLogTailUrl) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            var olfsLogDisplay = document.getElementById("olfsLogDisplay");

            olfsLogDisplay.innerHTML = "<pre>"+request.responseText+"</pre>";


            olfsLogTail_worker(olfsLogTailUrl);



        } else
            alert("Error! Request status is " + request.status);
    }
}

function olfsLogTail_worker(olfsLogTailUrl) {

    // When the timeout expires getOlfsLog will be called...
    if(!stopUpdatingOlfsLogView) {
        olfsLogTailTimer = setTimeout("getOlfsLog('"+olfsLogTailUrl+"')", 1000);
    }
}




function setOlfsLogLevel(olfsCtlApi){


    var olfsLoggerName = document.getElementById("olfsLoggerName").value;
    var olfsLoggerLevel = document.getElementById("olfsLoggerLevel").value;

    var url = olfsCtlApi+"?cmd=setLogLevel&logger="+olfsLoggerName+"&level="+olfsLoggerLevel;

    //alert(url);

    var d = new Date();
    var status = d.toTimeString() + " Setting "+olfsLoggerName+" log level to "+olfsLoggerLevel+".   <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() { updateStatus(request) };
    request.send(null);

}

function updateOlfsLogLevelSelection(olfsCtlApi) {


    var olfsLoggerName = document.getElementById("olfsLoggerName").value;

    var url = olfsCtlApi+"?cmd=getLogLevel&logger="+olfsLoggerName;


    var d = new Date();
    var status = d.toTimeString() + " Getting "+olfsLoggerName+" log level.   <a href='"+url+"'>"+url+"</a>";

    document.getElementById("status").innerHTML = status;
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() { updateOlfsLoggerLevel(request) };
    request.send(null);
}



function updateOlfsLoggerLevel(request) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            var olfsLoggerLevel = document.getElementById("olfsLoggerLevel");

            olfsLoggerLevel.value = request.responseText;

        } else {
            alert("Error! Request status is " + request.status);
        }
    }
}

function updateStatus(request) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            logDiv = document.getElementById("status");

            logDiv.innerHTML = "<pre>"+request.responseText+"</pre>";

        } else
            alert("Error! Request status is " + request.status);
    }
}



