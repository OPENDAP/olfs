<%@ page import="opendap.coreServlet.ServletUtil" %>
<%@ page import="opendap.bes.BES" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.jdom.output.XMLOutputter" %>
<%@ page import="org.jdom.output.Format" %>
<%@ page import="opendap.ppt.OPeNDAPClient" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="opendap.coreServlet.Scrub" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<%

    XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

    String contextPath = request.getContextPath();

    HashMap<String, String> kvp = new HashMap<String, String>();

    String query = request.getQueryString();

    if (query != null) {
        for (String kvPair : query.split("&")) {
            String[] kv = kvPair.split("=");

            String key = null, value = null;


            switch (kv.length) {
                case 2:
                    key = kv[0];
                    value = kv[1];
                    break;
                case 1:
                    key = kv[0];
                    break;
                default:
                    break;

            }

            if (key != null)
                kvp.put(key, value);
        }
    }


    String besCmd = kvp.get("cmd");
    String currentPrefix = kvp.get("prefix");
    String currentClientId = kvp.get("clientId");

    if (currentPrefix == null)
        currentPrefix = "/";


    BES bes = BESManager.getBES(currentPrefix);


    StringBuffer status = new StringBuffer();
    if (currentPrefix != null && besCmd != null) {

        if (besCmd.equals("Start")) {
            status.append(bes.start());
        } else if (besCmd.equals("StopNice")) {
            status.append(bes.stopNice(3000));
        } else if (besCmd.equals("StopNow")) {
            status.append(bes.stopNow());

        } else {
            status.append("Unrecognized BES command: ").append(Scrub.simpleString(besCmd));
        }
    } else {
        status.append("Waiting for you to do something...");
    }


%>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>BES Control</title>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/tabs.css' type='text/css'/>
</head>
<body>
<table width='95%'>
    <tr>
        <td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td>
        <td>
            <div style='v-align:center;font-size:large;'>Hyrax Admin Interface</div>
        </td>
    </tr>
</table>
<h1>BES Control</h1>
<ol id="toc">

    <%

        Iterator<BES> i = BESManager.getBES();

        while (i.hasNext()) {
            BES b = i.next();
            String prefix = b.getPrefix();

            out.append("    <li ");
            if (prefix.equals(currentPrefix))
                out.append("class=\"current\"");
            out.append(">");

            out.append("<a href=\"?prefix=")
                    .append(prefix)
                    .append("\">")
                    .append(prefix)
                    .append("</a></li>\n");
        }
        out.append("</ol>");


    %>
</ol>
<div class="content">
    <strong>BES</strong><br/>
    prefix: <strong><%=currentPrefix%>
</strong><br/>
    hostname: <strong><%=bes.getHost()%>:<%=bes.getPort()%>
</strong><br/>


    max client connections: <strong><%=bes.getMaxClients()%></strong><br/>
    current client connections: <strong><%=bes.getBesClientCount()%></strong><br/>

    <br/>

    <div class='small'>
        <ol id="toc">
            <%

                Enumeration<OPeNDAPClient> clients;
                OPeNDAPClient currentClient = null;
                clients = bes.getClients();
                while (clients.hasMoreElements()) {
                    OPeNDAPClient client = clients.nextElement();

                    out.append("    <li ");
                    if (client.getID().equals(currentClientId)) {
                        out.append("class=\"current\"");
                        currentClient = client;
                    }
                    out.append(">");


                    out.append("<a href=\"?prefix=")
                            .append(currentPrefix)
                            .append("&clientId=")
                            .append(client.getID())
                            .append("\">")
                            .append(client.getID())
                            .append("</a></li>\n");
                }
            %>

        </ol>

    </div>

    <div class='content'>
        <div class='medium'>

            <%
                if (currentClient != null) {

                    out.append("client id: <strong>")
                            .append(currentClient.getID())
                            .append("</strong><br />\n")
                            .append("commands executed: <strong>")
                            .append(Integer.toString(currentClient.getCommandCount()))
                            .append("</strong><br />\n")
                            .append("is running: <strong>")
                            .append(currentClient.isRunning() ? "true" : "false")
                            .append("</strong><br />\n");

                } else if (bes.getBesClientCount() == 0) {

                    out.append("<div class='small'>No active connections to BES '").append(currentPrefix).append("'</div>");

                } else {
                    out.append("<strong>Select a client to inspect.</strong>");
                }


            %>

        </div>
    </div>
    <br/>


    <a href="?prefix=<%=currentPrefix%>&clientId=<%=currentClientId%>&cmd=Start">
    <img src='<%=contextPath%>/docs/images/startButton.png' border='0' height='40px'/>
    </a>

    <a href="?prefix=<%=currentPrefix%>&clientId=<%=currentClientId%>&cmd=StopNice">
    <img src='<%=contextPath%>/docs/images/stopNiceButton.png' border='0' height='40px'/>
    </a>


    <a href="?prefix=<%=currentPrefix%>&clientId=<%=currentClientId%>&cmd=StopNow">
    <img src='<%=contextPath%>/docs/images/stopNowButton.png' border='0' height='40px'/>
    </a>



    <div class='status'><%=StringEscapeUtils.escapeHtml(status.toString())%></div>

    <hr/>

    <!--
    <div class='medium'>Version Document</div>
    <div class='small'>
        <pre>
            <%
            /*
                try {
                    out.append(StringEscapeUtils.escapeHtml(xmlo.outputString(bes.getVersionDocument())));
                } catch (Exception e) {
                    out.append("<p><strong>Unable to produce BES Version document.</strong></p>")
                            .append("<p>Error Message:<p>")
                            .append(e.getMessage())
                            .append("</p></p>");
                }
                */

            %>
        </pre>
    </div>
    -->

</div>

<table width="100%" border="0">
    <tr>
        <td>
            <div class="small" align="left">
                Hyrax Administration Prototype
            </div>
        </td>
        <td>
            <div class="small" align="right">
                Hyrax development sponsored by
                <a href='http://www.nsf.gov/'>NSF</a>
                ,
                <a href='http://www.nasa.gov/'>NASA</a>
                , and
                <a href='http://www.noaa.gov/'>NOAA</a>
            </div>
        </td>
    </tr>
</table>
<h3>OPeNDAP Hyrax

    <br/>
    <a href='<%= contextPath%>/docs/'>Documentation</a>
</h3>
</body>
</html>