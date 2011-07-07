<%@ page import="opendap.coreServlet.ServletUtil" %>
<%@ page import="opendap.bes.BES" %>
<%@ page import="opendap.bes.BESManager" %>
<%@ page import="org.jdom.output.XMLOutputter" %>
<%@ page import="org.jdom.output.Format" %>
<%@ page import="opendap.ppt.OPeNDAPClient" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="opendap.coreServlet.Scrub" %>
<%@ page import="java.net.URL" %>
<%@ page import="opendap.bes.BesConfigurationModule" %>
<%@ page import="opendap.hai.BesControlApi" %>
<%@ page import="opendap.hai.Util" %>
<%@ page import="java.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<%

    String contextPath = request.getContextPath();

    HashMap<String, String> kvp = Util.processQuery(request);


    String currentPrefix = kvp.get("prefix");
    if (currentPrefix == null)
        currentPrefix = "/";



    String currentBesTask = kvp.get("task");
    if (currentBesTask == null)
        currentBesTask = "";


    BES bes = BESManager.getBES(currentPrefix);

    currentPrefix = bes.getPrefix();

    String besCtlApi = contextPath + "/hai/besctl";


    StringBuffer status = new StringBuffer();
    status.append(" OK ");


%>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>BES Controller</title>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/tabs.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/besctl.css' type='text/css'/>
    <script type="text/javascript" src="js/XmlHttpRequest.js"></script>
    <script type="text/javascript" src="js/besctl.js"></script>

</head>

<body>
<h1>Bes Logging Configuration</h1>

<div id="besLoggingConfig" class="content">


            <div class="small">

                <div style=" margin-left: 40px; margin-right: 5px;">
                    <form name="loggerSelect" action="<%=besCtlApi%>?prefix=<%=currentPrefix%>&cmd=setLoggers"
                          method="get">

                    <%
                        TreeMap<String, BES.BesLogger> besLoggers = bes.getBesLoggers();
                        for (BES.BesLogger logger : besLoggers.values()) {
                            out.append("<input type='checkbox' name='lSelection' value='").append(logger.getName()).append("'");

                            if (logger.getIsEnabled()) {
                                out.append(" checked='checked' ");
                            }
                            out.append(" />").append(logger.getName()).append("<br/>\n");

                        }

                    %>

                    </form>
                </div>
            </div>

        <button onclick="commitLoggingChanges('<%=besCtlApi%>','<%=bes.getPrefix()%>',document.loggerSelect.lSelection);">Commit</button>

        <button onclick="self.close()">Cancel</button>


</div>


<br/>
<div class="tiny_black_fw"><strong>Status</strong>

    <div id='status' class='statusDisplay'>
        <pre><%=status%></pre>
    </div>

</div>


</body>
</html>