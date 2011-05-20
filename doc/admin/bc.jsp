<%@ page import="opendap.coreServlet.ServletUtil" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
    <%

            String contextPath = request.getContextPath();

    %>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>BES Control</title>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/contents.css' type='text/css'/>
    <link rel='stylesheet' href='<%= contextPath%>/docs/css/tabs.css' type='text/css'/>
</head>
<body><table width='95%'><tr><td><img alt="OPeNDAP Logo" src='<%= contextPath%>/docs/images/logo.gif'/></td><td><div style='v-align:center;font-size:large;'>Hyrax Admin Interface</div></td></tr></table><h1>BES Control</h1><ol id="toc">
    <li class="current"><a href="?prefix=/">/</a></li>
</ol><div class="content">
<strong>BES</strong><br />prefix: <strong>/</strong><br />
hostname: <strong>localhost:10002</strong><br />
max client connections: <strong>5</strong><br />
current client connections: <strong>0</strong><br />
<br />


<div class='small'><ol id="toc">
</ol></div><div class='content'><div class='medium'>
<strong>Select a client to inspect.</strong></div></div><br />
<a href="?prefix=/&clientId=null&cmd=Start"><img src='<%= contextPath%>/docs/images/startButton.png' border='0' height='40px' /></a>
    <a href="?prefix=/&clientId=null&cmd=StopNice"><img src='<%= contextPath%>/docs/images/stopNiceButton.png' border='0'  height='40px' /></a>
    <a href="?prefix=/&clientId=null&cmd=StopNow"><img src='<%= contextPath%>/docs/images/stopNowButton.png'  border='0' height='40px' /></a>
    <div class='status'>Waiting for you to do something...</div><hr /><div class='medium'>Version Document</div><div class='small'><pre>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
&lt;BES xmlns=&quot;http://xml.opendap.org/ns/bes/1.0#&quot; prefix=&quot;/&quot;&gt;
  &lt;Administrator&gt;ndp@opendap.org&lt;/Administrator&gt;
  &lt;library name=&quot;bes&quot;&gt;3.9.0&lt;/library&gt;
  &lt;module name=&quot;dap-server/ascii&quot;&gt;4.1.0&lt;/module&gt;
  &lt;module name=&quot;csv_handler&quot;&gt;1.0.1&lt;/module&gt;
  &lt;library name=&quot;libdap&quot;&gt;3.11.0&lt;/library&gt;
  &lt;serviceVersion name=&quot;dap&quot;&gt;
    &lt;version&gt;2.0&lt;/version&gt;
    &lt;version&gt;3.0&lt;/version&gt;
    &lt;version&gt;3.2&lt;/version&gt;
  &lt;/serviceVersion&gt;
  &lt;module name=&quot;freeform_handler&quot;&gt;3.8.2&lt;/module&gt;
  &lt;module name=&quot;fits_handler&quot;&gt;1.0.5&lt;/module&gt;
  &lt;module name=&quot;fileout_netcdf&quot;&gt;1.1.0&lt;/module&gt;
  &lt;module name=&quot;gateway_module&quot;&gt;0.0.3&lt;/module&gt;
  &lt;module name=&quot;hdf4_handler&quot;&gt;3.9.1&lt;/module&gt;
  &lt;module name=&quot;hdf5_handler&quot;&gt;1.4.3.patch&lt;/module&gt;
  &lt;module name=&quot;netcdf_handler&quot;&gt;3.9.2&lt;/module&gt;
  &lt;module name=&quot;ncml_module&quot;&gt;1.1.0&lt;/module&gt;
  &lt;module name=&quot;dap-server/usage&quot;&gt;4.1.0&lt;/module&gt;
  &lt;module name=&quot;dap-server/www&quot;&gt;4.1.0&lt;/module&gt;
&lt;/BES&gt;

</pre></div></div><table width="100%" border="0">
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
</table><h3>OPeNDAP Hyrax

    <br/>
    <a href='<%= contextPath%>/docs/'>Documentation</a>
</h3>
</body>
</html>