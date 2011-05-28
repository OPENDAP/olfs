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

var request1 = createRequest();

function start(prefix,besctlUrl) {

    document.getElementById("status").innerHTML = "<pre> Starting BES '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=Start";
    request1.open("GET", url, true);
    request1.onreadystatechange = updatePage;
    request1.send(null);
}



function stopNice(prefix,besctlUrl) {
    document.getElementById("status").innerHTML = "<pre> Gently stopping BES '"+prefix+"'...</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=StopNice";
    request1.open("GET", url, true);
    request1.onreadystatechange = updatePage;
    request1.send(null);
}

function stopNow(prefix,besctlUrl) {
    document.getElementById("status").innerHTML = "<pre> Stopping BES '"+prefix+"' NOW.</pre>";
    var url = besctlUrl+"?prefix="+prefix+"&"+"cmd=StopNow";
    request1.open("GET", url, true);
    request1.onreadystatechange = updatePage;
    request1.send(null);

}

function getConfig(module,prefix,besctlUrl) {
    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"cmd=getConfig";
    request1.open("GET", url, true);
    request1.onreadystatechange = updateConfig;
    request1.send(null);





}

function setConfig(module,prefix,besctlUrl) {

    document.getElementById("status").innerHTML = "<pre> Setting configuration for "+module+"</pre>";

    var url = besctlUrl+"?module="+module+"&"+"prefix="+prefix+"&"+"cmd=setConfig";

    var configElement =   document.getElementById("CONFIGURATION");


    var config = configElement.value;
    var configParam = "CONFIGURATION="+encodeURIComponent(config);

    request1.open("POST", url, false);
    request1.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    request1.onreadystatechange = updateConfig;
    request1.send(configParam);

}



function showBes() {
    document.getElementById("status").innerHTML="New BES TIME...";
}



function updatePage() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            document.getElementById("status").innerHTML = "<pre> "+request1.responseText+"</pre>";
            //document.getElementById("besDetail").innerHTML = "<h1>Select BES to view...</h1>";


        } else
            alert("Error! Request status is " + request1.status);
    }
}


function updateConfig() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            //document.getElementById("CONFIGURATION").innerHTML = request1.responseText;
            //document.getElementById("besDetail").innerHTML = "<h1>Select BES to view...</h1>";

            alert(request1.responseText);
            document.getElementById("status").innerHTML = "<pre> "+request1.responseText+"</pre>";

        } else
            alert("Error! Request status is " + request1.status);
    }
}


var logUrl;
var logBesPrefix;
var logLines;


function getBesLog(besLogUrl, besPrefix, lines) {

    logUrl = besLogUrl;
    logBesPrefix = besPrefix;
    logLines=lines;

    var url = logUrl+"?cmd=getLog&prefix="+besPrefix+"&lines="+lines;

    var d = new Date();
    var status = d.toDateString() + "  "+ d.toTimeString() + " Polling log: "+url;

    document.getElementById("status").innerHTML = status;
    request1.open("GET", url, true);
    request1.onreadystatechange = updateLoggerPage;
    request1.send(null);
}



function startTailing(tailURL,besPrefix, lines) {
    t = setTimeout("getBesLog('"+tailURL+"','"+besPrefix+"','"+lines+"')", 1000);
}


function stopTailing() {
    clearTimeout(t);
    document.getElementById("message").innerHTML =
            "The log viewer has been paused. " +
            "To begin viewing again, click the Start button.";

}

function clearLogWindow() {
    document.getElementById("log").innerHTML="";
}



function updateLoggerPage() {
    if (request1.readyState == 4) {
        if (request1.status == 200) {

            logDiv = document.getElementById("log");

            logDiv.innerHTML = "<pre>"+request1.responseText+"</pre>" ;

            startTailing(logUrl, logBesPrefix, logLines);


        } else
            alert("Error! Request status is " + request1.status);
    }
}