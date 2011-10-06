/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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
/////////////////////////////////////////////////////////////////////////////


function start(prefix,besctlUrl) {

    start_worker(prefix,besctlUrl,preformattedStatusUpdate);

}

function start_worker(prefix,besctlUrl, stateChangeHandler) {

    document.getElementById("status").innerHTML = "<pre> Starting BES '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=Start";
    var request = createRequest();

    request.open("GET", url, true);
    request.onreadystatechange = function() { stateChangeHandler(request); };
    request.send(null);

}



function stopNice(prefix,besctlUrl) {


    stopNice_worker(prefix,besctlUrl,true,preformattedStatusUpdate);

}

function stopNice_worker(prefix,besctlUrl,isAsync, stateChangeHandler) {


    var status = document.getElementById("status");
    status.innerHTML = "<pre> Gently stopping BES '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=StopNice";
    var request = createRequest();
    //alert("StopNice: \n stateChangeHandler: "+stateChangeHandler+"\n url: "+url);

    if(isAsync) {
        status.innerHTML = "Is Async";
        //alert(status.innerHTML.valueOf());
        request.open("GET", url, true);
        request.onreadystatechange = function() { stateChangeHandler(request); };
        request.send(null);
    }
    else {
        status.innerHTML = "Is Sync";
        //alert(status.innerHTML.valueOf());
        request.open("GET", url, false);
        request.send(null);
        status.innerHTML = "<pre> "+request.responseText+"</pre>";
        //alert(status.innerHTML.valueOf());
        //stateChangeHandler(request);
    }
}


function stopNow(prefix,besctlUrl) {
    document.getElementById("status").innerHTML = "<pre> Stopping BES '"+prefix+"' NOW.</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=StopNow";
    var request = createRequest();

    request.open("GET", url, true);
    request.onreadystatechange = function() { preformattedStatusUpdate(request); };
    request.send(null);

}

function getConfig(module,prefix,besctlUrl) {
    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"cmd=getConfig";
    var request = createRequest();

    request.open("GET", url, true);
    request.onreadystatechange = function() { updateConfig(request); };
    request.send(null);
}

function setConfig(module,prefix,besctlUrl) {

    document.getElementById("status").innerHTML = "<pre> Setting configuration for "+module+"</pre>";

    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"cmd=setConfig";

    var configElement =   document.getElementById("CONFIGURATION");


    var config = configElement.value;
    var configParam = "CONFIGURATION="+encodeURIComponent(config);

    var request = createRequest();
    request.open("POST", url, false);
    request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    request.onreadystatechange = function() { updateConfig(request); };
    request.send(configParam);

}



function showBes() {
    document.getElementById("status").innerHTML="New BES TIME...";
}





function updateConfig(request) {
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



var logUrl;
var logBesPrefix;




/**
 *
 * @param besLogUrl
 * @param besPrefix
 */
function getBesLog(besLogUrl, besPrefix) {

    logUrl = besLogUrl;
    logBesPrefix = besPrefix;

    var logLines = document.getElementById("logLines").value;

    if(logLines=="all")
        logLines=0;

    var url = logUrl+"?cmd=getLog&prefix="+besPrefix+"&lines="+logLines;

    //alert(url);

    var d = new Date();

    document.getElementById("status").innerHTML = d.toTimeString() + " Polling log: <a href='"+url+"'>"+url+"</a>";

    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() { updateLogContent(request); };
    request.send(null);
}


var stopUpdatingLogView;
var timeout;

function startTailing(tailURL,besPrefix) {
    stopUpdatingLogView = false;
    if(!stopUpdatingLogView)
        timeout = setTimeout("getBesLog('"+tailURL+"','"+besPrefix+"')", 1000);
}


function stopTailing() {
    stopUpdatingLogView = true;
    clearTimeout(timeout);

    var d = new Date();


    document.getElementById("status").innerHTML =  d.toTimeString() + " "+
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";



}

function clearLogWindow() {
    document.getElementById("log").innerHTML="";
}



function updateLogContent(request) {
    if (request.readyState == 4) {
        if (request.status == 200) {

            var logDiv = document.getElementById("log");

            logDiv.innerHTML = "<pre>"+request.responseText+"</pre>" ;

            startTailing(logUrl, logBesPrefix);


        } else
            alert("updateLoggerPage(): Error! BES log request returned HTTP status of " + request.status);
    }
}


function commitLoggingChanges(besCtlApi, besPrefix, loggerSelect){

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

    var url = besCtlApi+"?prefix="+besPrefix+"&cmd=setLoggerStates&enable="+enabled+"&disable="+disabled;



    var d = new Date();
    var status = "Enabling loggers: '"+enabled+"'\n Disabling loggers: '"+disabled+"'\n" +"   <a href='"+url+"'>"+url+"</a>";
    //alert(status);
    document.getElementById("status").innerHTML = status;

    var setLoggerStatesRequest = createRequest();


    setLoggerStatesRequest.open("GET", url, true);

    setLoggerStatesRequest.onreadystatechange = function() {confirmCommit(besPrefix,besCtlApi,setLoggerStatesRequest); };


    setLoggerStatesRequest.send(null);


}

function confirmCommit(besPrefix,besCtlApi,setLoggerStatesRequest) {

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
            stopNice_worker(
                besPrefix,
                besCtlApi,
                true,
                function(request){
                    if (request.readyState == 4) {
                        if (request.status == 200) {
                            var status = document.getElementById("status");
                            status.innerHTML = "<pre> "+request.responseText+"</pre>";
                            //alert("About to start BES...");
                            //
                            // I used another closure here because I want to close the window after I start the BES.
                            // To do this I need to ensure that the BES is started and all of the status updates are
                            // done before I close the window. Failing to do this in a sequential order will crash the
                            // browser (or at least Safari)
                            start_worker(
                                besPrefix,
                                besCtlApi,
                                function(request){
                                    if (request.readyState == 4) {

                                        if (request.status == 200) {

                                            var status = document.getElementById("status");
                                            status.innerHTML = "<pre> "+request.responseText+"</pre>";
                                            //document.getElementById("besDetail").innerHTML = "<h1>Select BES to view...</h1>";


                                        } else
                                            alert("preformattedStatusUpdate(): Error! Hyrax returned an HTTP status of " + request.status+" Along with the following content: "+request.responseText);
                                    }
                                    self.close();
                                }
                            );
                        }
                        else {
                            alert("confirmCommit(): Error! Hyrax returned an HTTP status of " + request.status+" Along with the following content: "+request.responseText);

                        }
                    }
                }
            );
            //alert("About to start BES...");
            //start(besPrefix,besCtlApi);
        }
        else {
            var d = new Date();
            document.getElementById("status").innerHTML = d.toTimeString() + " Logging Commit aborted!";
        }

    }
    else {
        alert("confirmCommit(): Error! Failed to set the BES logger state. Hyrax returned an Http Request status of " + setLoggerStatesRequest.status);
    }

    //self.close();
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
