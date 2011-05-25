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

function start(prefix) {

    document.getElementById("status").innerHTML = "Polling OLFS log...";
    var url = logURL;
    request1.open("GET", url, true);
    request1.onreadystatechange = updatePage;
    request1.send(null);
    startTail(url);
}



function stopNice(prefix) {
    t = setTimeout("getLog('"+tailURL+"')", 1000);
}

function stopNow(prefix) {
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