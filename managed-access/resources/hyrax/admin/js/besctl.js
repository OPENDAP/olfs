/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */


function startBes(prefix,besName,besctlUrl) {

    startBes_worker(prefix,besName,besctlUrl,preformattedStatusUpdate);

}

function startBes_worker(prefix, besName, besctlUrl, stateChangeHandler) {

    document.getElementById("status").innerHTML = "<pre> Starting BES '"+besName+"' in BesGroup '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"besName="+besName+"&"+"cmd=Start";
    var startRequest = createRequest();
    // alert("Start: \n stateChangeHandler: "+stateChangeHandler+"\n url: "+url);

    startRequest.open("GET", url, true);
    startRequest.onreadystatechange = function() { stateChangeHandler(startRequest); };
    startRequest.send(null);

}



function stopBesNicely(prefix,besName,besctlUrl) {


    stopBesNicely_worker(prefix,besName,besctlUrl,true,preformattedStatusUpdate);

}

function stopBesNicely_worker(prefix, besName, besctlUrl, isAsync, stateChangeHandler) {


    var status = document.getElementById("status");
    status.innerHTML = "<pre> Gently stopping BES '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"besName="+besName+"&"+"cmd=StopNice";
    var stopRequest = createRequest();
    // alert("StopNice: \n stateChangeHandler: "+stateChangeHandler+"\n url: "+url);

    if(isAsync) {
        status.innerHTML = "Is Async";
        //alert(status.innerHTML.valueOf());
        stopRequest.open("GET", url, true);
        stopRequest.onreadystatechange = function() { stateChangeHandler(stopRequest); };
        stopRequest.send(null);
    }
    else {
        status.innerHTML = "Is Sync";
        //alert(status.innerHTML.valueOf());
        stopRequest.open("GET", url, false);
        stopRequest.send(null);
        status.innerHTML = "<pre> "+stopRequest.responseText+"</pre>";
        //alert(status.innerHTML.valueOf());
        //stateChangeHandler(request);
    }
}


function stopBesNow(prefix,besName, besctlUrl) {
    document.getElementById("status").innerHTML = "<pre> Stopping BES '"+prefix+"' NOW.</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"besName="+besName+"&"+"cmd=StopNow";
    var request = createRequest();
    // alert("StopNow: \n stateChangeHandler: "+stateChangeHandler+"\n url: "+url);

    request.open("GET", url, true);
    request.onreadystatechange = function() { preformattedStatusUpdate(request); };
    request.send(null);

}

function getBesConfig(module,prefix, besName,besctlUrl) {
    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"besName="+besName+"&"+"cmd=getConfig";
    var request = createRequest();

    request.open("GET", url, true);
    request.onreadystatechange = function() { updateBesConfig(request); };
    request.send(null);
}

function setBesConfig(prefix,besName,module,besctlUrl) {

    document.getElementById("status").innerHTML = "<pre> Setting configuration for "+module+"</pre>";

    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"besName="+besName+"&"+"cmd=setConfig";

    var configElement =   document.getElementById("besConfiguration");


    var config = configElement.value;
    var configParam = "CONFIGURATION="+encodeURIComponent(config);

    var request = createRequest();
    request.open("POST", url, false);
    request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    request.onreadystatechange = function() { updateBesConfig(request); };
    request.send(configParam);

}



function showBes() {
    document.getElementById("status").innerHTML="New BES TIME...";
}





function updateBesConfig(request) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            alert(request.responseText);
            document.getElementById("status").innerHTML = "<pre> "+request.responseText+"</pre>";

        } else
            alert("updateConfig(): Error! Hyrax returned an HTTP status of " + request.status+" Along with the following content: "+request.responseText);
    }
}


//#########################################################
//#########################################################
//#########################################################



//var besLogUrl;
//var logBesPrefix;
//var logBesName;

var stopUpdatingBesLogView;
var besLogTailTimer;
var besLoggingStarted = false;


function startBesLogTailing(besApiUrl,besPrefix,besName) {

    if(!besLoggingStarted){
        // Make sure that log viewing is enabled.
        stopUpdatingBesLogView = false;
        besLoggingStarted = true;

        // Go get the log and start the log tailing cycle.
        getBesLog(besApiUrl,besPrefix,besName);
    }
}



function stopBesLogTailing() {
    besLoggingStarted = false;
    stopUpdatingBesLogView = true;
    clearTimeout(besLogTailTimer);
    var d = new Date();
    document.getElementById("status").innerHTML =  d.toTimeString() + " "+
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";



}

/**
 *
 * @param besLogUrl
 * @param besPrefix
 */
function getBesLog(besApiUrl, besPrefix, besName) {


    var logLines = document.getElementById("logLines").value;

    if(logLines=="all")
        logLines=0;

    var url = constructBesLogTailUrl(besApiUrl, besPrefix, besName, logLines);

    //alert(url);

    var d = new Date();

    document.getElementById("status").innerHTML = d.toTimeString() + " Polling log: <a href='"+url+"'>"+url+"</a>";



    // When the request is sent a state change will call updateLogContent. Once updateLogContent gets the response,
    // it will call logTail_worker() to initiate a new acquisition of the log tail (which is done by call this function,
    // getBesLog() )
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() { updateBesLogContent(request, besApiUrl, besPrefix, besName); };
    request.send(null);
}

function constructBesLogTailUrl(besApiUrl, besPrefix, besName, lineCount){
    return besApiUrl+"?cmd=getLog&prefix="+besPrefix+"&besName="+besName+"&lines="+lineCount;
}



function updateBesLogContent(request, besApiUrl, besPrefix, besName) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            var logDiv = document.getElementById("log");

            logDiv.innerHTML = "<pre>"+request.responseText+"</pre>" ;

            // Now that we have the log and have updated the display, start the cycle again...
            besLogTail_worker(besApiUrl, besPrefix, besName);


        } else
            alert("updateBesLogContent(): Error! BES log request returned HTTP status of '" + request.status+"'  url: '"+request.url+"'");
    }
}


function besLogTail_worker(besApiUrl, besPrefix, besName) {

    // When the timeout expires getBesLog will be called...
    if(!stopUpdatingBesLogView) {
        besLogTailTimer = setTimeout(function(){getBesLog(besApiUrl,besPrefix,besName)},1000);
    }
}



function clearBesLogWindow() {
    document.getElementById("log").innerHTML="";
}






function launchBesLoggingConfigurationWindow(logConfigUrl, name, size){
    stopBesLogTailing();
    window.open(logConfigUrl, name, size);
}

function commitBesLoggingChanges(besCtlApi, besPrefix, besName, loggerSelect){


    var enabled = "";
    var disabled = "";


    for (i = 0; i < loggerSelect.length; i++){
        if(loggerSelect[i].checked){
            if(enabled!="")
                enabled +=",";
            enabled += loggerSelect[i].value;
        }
        else {
            if(disabled!="")
                disabled +=",";
            disabled += loggerSelect[i].value;
        }


    }

    var url = besCtlApi+"?prefix="+besPrefix+"&besName="+besName+"&cmd=setLoggerStates&enable="+enabled+"&disable="+disabled;



    var d = new Date();
    var status = "BesGroup: '"+besPrefix+"' Bes: '"+besName+"'-- Enabling loggers: '"+enabled+"'\n Disabling loggers: '"+disabled+"'\n" +"   <a href='"+url+"'>"+url+"</a>";
    //alert(status);
    document.getElementById("status").innerHTML = status;

    var setLoggerStatesRequest = createRequest();
    setLoggerStatesRequest.open("GET", url, true);
    setLoggerStatesRequest.onreadystatechange = function() {confirmBesLoggingConfigurationCommit(besPrefix,besName,besCtlApi,setLoggerStatesRequest); };
    setLoggerStatesRequest.send(null);


}

function confirmBesLoggingConfigurationCommit(besPrefix,besName,besCtlApi,setLoggerStatesRequest) {

    if (setLoggerStatesRequest.readyState != 4)
        return;
    if (setLoggerStatesRequest.status == 200) {
        var r=confirm("Committing these changes will require that the BES be stopped and restarted. " +
                "I will do this as gently as possible, but some connections may be dropped. " +
                "Do you wish to continue?");
        if(r){

            /* I worked this over to use a 'closure' function. This allows me to daisy chain the async AJAX calls
              and prevents commands from being sent in the wrong order. So StopNice is called and ONLY when it is
              completed the closure function defined below is called. The closure function updates the status block and
              then calls start. Sweet! Right?
             */


            stopBesNicely_worker(
                besPrefix,
                besName,
                besCtlApi,
                true,
                function(stopRequest) {
                    if (stopRequest.readyState == 4) {
                        if (stopRequest.status == 200) {
                            var status = document.getElementById("status");
                            status.innerHTML = "<pre> " + stopRequest.responseText + "</pre>";

                            //alert("About to start BES...");
                            //
                            // I used another closure here because I want to close the window after I start the BES.
                            // To do this I need to ensure that the BES is started and all of the status updates are
                            // done before I close the window. Failing to do this in a sequential order will crash the
                            // browser (or at least Safari)
                            startBes_worker(
                                besPrefix,
                                besName,
                                besCtlApi,
                                function(startRequest) {
                                    if (startRequest.readyState == 4) {

                                        if (startRequest.status == 200) {

                                            var status = document.getElementById("status");
                                            status.innerHTML = "<pre> " + startRequest.responseText + "</pre>";

                                            self.close();

                                        } else {
                                            alert("confirmCommit(stopBesNicely_worker(startBes_worker())): Error! Hyrax returned an HTTP status of " + startRequest.status + " Along with the following content: " + startRequest.responseText);

                                        }
                                    }
                                }
                            );
                        }
                        else {
                            alert("confirmCommit(stopBesNicely_worker()): Error! Hyrax returned an HTTP status of " + stopRequest.status + " Along with the following content: " + stopRequest.responseText);
                        }

                    }
                }
            );

        }
        else {
            var d = new Date();
            document.getElementById("status").innerHTML = d.toTimeString() + " Logging Commit aborted!";
        }

    }
    else {
        alert("confirmCommit(): Error! Failed to set the BES logger state. Hyrax returned an Http Request status of " + setLoggerStatesRequest.status);
    }

}




function updateStatus(request) {
    if (request.readyState == 4) {

        if (request.status == 200) {

            var status = document.getElementById("status");
            status.innerHTML = request.responseText;

        } else
            alert("updateStatus(): Error! Failed to get the BES logger state. Hyrax returned an Http Request status of " + request.status);
    }
}


function preformattedStatusUpdate(request) {
    if (request.readyState == 4) {

        if (request.status == 200) {

            var status = document.getElementById("status");
            status.innerHTML = "<pre> "+request.responseText+"</pre>";
            //document.getElementById("besDetail").innerHTML = "<h1>Select BES to view...</h1>";


        } else
            alert("preformattedStatusUpdate(): Error! Hyrax returned an HTTP status of " + request.status+" Along with the following content: "+request.responseText);
    }
}
