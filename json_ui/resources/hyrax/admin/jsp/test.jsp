<!--
~ /////////////////////////////////////////////////////////////////////////////
~ // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
~ //
~ //
~ // Copyright (c) $year OPeNDAP, Inc.
~ // Author: Nathan David Potter <ndp@opendap.org>
~ //
~ // This library is free software; you can redistribute it and/or
~ // modify it under the terms of the GNU Lesser General Public
~ // License as published by the Free Software Foundation; either
~ // version 2.1 of the License, or (at your option) any later version.
~ //
~ // This library is distributed in the hope that it will be useful,
~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
~ // Lesser General Public License for more details.
~ //
~ // You should have received a copy of the GNU Lesser General Public
~ // License along with this library; if not, write to the Free Software
~ // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
~ //
~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
~ /////////////////////////////////////////////////////////////////////////////
-->
<%@ page import="java.util.Enumeration" %>
<HTML>
<BODY>
Hello!  The time is now <%= new java.util.Date() %>

<%
    out.println( "<BR>Your machine's address is " );
    out.println( request.getRemoteHost());

        String className = this.getClass().getName();

    String servletInfo = getServletInfo();
    ServletConfig sConfig = this.getServletConfig();

    ServletContext sContext = sConfig.getServletContext();


%>




<p>BESManager.isConfigured(): <strong><%= opendap.bes.BESManager.isConfigured() %></strong></p>


className: <strong><%= className%></strong> <br/>
servletInfo: <strong><%= getServletInfo() %></strong> <br/>


<dl>
    <dt><strong>ServletContext</strong></dt>
    <dd>class name:               <strong><%= sContext.getClass().getName() %>     </strong></dd>
    <dd>getContextPath():         <strong><%= sContext.getContextPath() %>        </strong></dd>
    <dd>getServletContextName():  <strong><%= sContext.getServletContextName() %> </strong></dd>
    <dd>getRealPath("docs"):      <strong><%= sContext.getRealPath("docs") %>     </strong></dd>
    <dd> <dl>
    <dt><strong>Attributes</strong></dt>
    <%
        StringBuffer sb = new StringBuffer();
        Enumeration<String> attrNames = sContext.getAttributeNames();
        while(attrNames.hasMoreElements()){
            String name = attrNames.nextElement();
            Object value = sContext.getAttribute(name);

            sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
        }
        out.println(sb.toString());
    %>
    </dl></dd>
    <dd> <dl>
    <dt><strong>InitParams</strong></dt>
    <%
        sb = new StringBuffer();
        Enumeration<String> paramNames = sContext.getInitParameterNames();
        while(paramNames.hasMoreElements()){
            String name = paramNames.nextElement();
            String value = sContext.getInitParameter(name);

            sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
        }
        out.println(sb.toString());
    %>
    </dl></dd>


    <dt><strong>ServletConfig</strong></dt>
    <dd>class name:     <strong><%= sConfig.getClass().getName() %> </strong></dd>
    <dd>servletName():  <strong><%= sConfig.getServletName() %>     </strong></dd>

    <dd> getInitParamsNames():
        <dl>
        <dt><strong>InitParams</strong></dt>
        <%
            sb = new StringBuffer();
            paramNames = sConfig.getInitParameterNames();
            while(paramNames.hasMoreElements()){
                String name = paramNames.nextElement();
                String value = sConfig.getInitParameter(name);

                sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
            }
            out.println(sb.toString());
        %>

    </dl>
    </dd>



    <dt><strong>request</strong></dt>
        <dd>class name:     <strong><%= request.getClass().getName() %> </strong></dd>
        <dd>getPathInfo():     <strong><%= request.getPathInfo() %> </strong></dd>
        <dd>getContextPath():     <strong><%= request.getContextPath() %> </strong></dd>
        <dd>getProtocol():     <strong><%= request.getProtocol() %> </strong></dd>
        <dd> getAttributes():
            <dl>
                <dt><strong>Attributes</strong></dt>
                <%
                    sb = new StringBuffer();
                    attrNames = request.getAttributeNames();
                    while(attrNames.hasMoreElements()){
                        String name = attrNames.nextElement();
                        Object value = request.getAttribute(name);

                        sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong> (class: ").append(value.getClass().getName()).append(")</dd>\n");
                    }
                    out.println(sb.toString());
                %>
            </dl>
        </dd>
        <dd>

        <dd>getAuthType():     <strong><%= request.getAuthType() %> </strong></dd>
        <dd>getCharacterEncoding():     <strong><%= request.getCharacterEncoding() %> </strong></dd>
        <dd>getContentLength():     <strong><%= request.getContentLength() %> </strong></dd>
        <dd>getContentType():     <strong><%= request.getContentType() %> </strong></dd>
        <dd>getContextPath():     <strong><%= request.getContextPath() %> </strong></dd>
        <dd> getCookies():
            <dl>
                <%
                    sb = new StringBuffer();
                    Cookie[] cookies = request.getCookies();
                    for(Cookie c : cookies){

                        sb.append("<dd>").append(c.getName()).append(": <strong>").append(c.getValue()).append("</strong> (class: ").append(c.getClass().getName()).append(")</dd>\n");
                    }
                    out.println(sb.toString());
                %>
            </dl>
        </dd>
        <dd>

        <dd> Headers():
            <dl>
                <%
                    sb = new StringBuffer();
                    Enumeration<String> headers = request.getHeaderNames();
                    while(headers.hasMoreElements()){
                        String name = headers.nextElement();
                        String value = request.getHeader(name);

                        sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong> </dd>\n");
                    }
                    out.println(sb.toString());
                %>
            </dl>
        </dd>



        <dd>getLocalAddr():     <strong><%= request.getLocalAddr() %> </strong></dd>
        <dd>getLocale():     <strong><%= request.getLocale() %> </strong></dd>
        <dd>getLocalName():     <strong><%= request.getLocalName() %> </strong></dd>
        <dd>getLocalPort():     <strong><%= request.getLocalPort() %> </strong></dd>
        <dd>getMethod():     <strong><%= request.getMethod() %> </strong></dd>

        <dd> Parameters():
            <dl>
                <%
                    sb = new StringBuffer();
                    Enumeration<String> params = request.getParameterNames();
                    while(params.hasMoreElements()){
                        String name = params.nextElement();
                        String value = request.getParameter(name);

                        sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong> </dd>\n");
                    }
                    out.println(sb.toString());
                %>
            </dl>
        </dd>





        <dd>getPathInfo():     <strong><%= request.getPathInfo() %> </strong></dd>
        <dd>getPathTranslated():     <strong><%= request.getPathTranslated() %> </strong></dd>
        <dd>getProtocol():     <strong><%= request.getProtocol() %> </strong></dd>
        <dd>getQueryString():     <strong><%= request.getQueryString() %> </strong></dd>
        <dd>getRemoteAddr():     <strong><%= request.getRemoteAddr() %> </strong></dd>
        <dd>getRemotePort():     <strong><%= request.getRemotePort() %> </strong></dd>
        <dd>getRemoteUser():     <strong><%= request.getRemoteUser() %> </strong></dd>
        <dd>getRequestedSessionId():     <strong><%= request.getRequestedSessionId() %> </strong></dd>
        <dd>getRemoteHost():     <strong><%= request.getRemoteHost() %> </strong></dd>
        <dd>getRequestURI():     <strong><%= request.getRequestURI() %> </strong></dd>
        <dd>getRequestURL():     <strong><%= request.getRequestURL() %> </strong></dd>
        <dd>getScheme():     <strong><%= request.getScheme() %> </strong></dd>
        <dd>getServerName():     <strong><%= request.getServerName() %> </strong></dd>
        <dd>getServerPort():     <strong><%= request.getServerPort() %> </strong></dd>
        <dd>getServletPath():     <strong><%= request.getServletPath() %> </strong></dd>
        <dd>isSecure():     <strong><%= request.isSecure() %> </strong></dd>


    <dt><strong>response</strong></dt>
    <dd>class name:     <strong><%= response.getClass().getName() %> </strong></dd>


    <dt><strong>out</strong></dt>
    <dd>class name:     <strong><%= out.getClass().getName() %> </strong></dd>


    <dt><strong>session</strong></dt>
    <dd>class name:               <strong><%= session.getClass().getName() %>     </strong></dd>
    <dd>getId():                  <strong><%= session.getId() %>                  </strong></dd>
    <dd>getCreationTime():        <strong><%= session.getCreationTime() %>        </strong></dd>
    <dd>getLastAccessedTime():    <strong><%= session.getLastAccessedTime() %>    </strong></dd>
    <dd>getMaxInactiveInterval(): <strong><%= session.getMaxInactiveInterval() %> </strong></dd>

    <dd>isNew():                  <strong><%= session.isNew() %>                  </strong></dd>
    <dd>
         <dl>
        <dt><strong>Attributes</strong></dt>
        <%
            sb = new StringBuffer();
            attrNames = session.getAttributeNames();
            while(attrNames.hasMoreElements()){
                String name = attrNames.nextElement();
                String value = sConfig.getInitParameter(name);

                sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
            }
            out.println(sb.toString());
        %>
        </dl>
    </dd>


    <dt><strong>application</strong></dt>
    <dd>class name:     <strong><%= application.getClass().getName() %> </strong></dd>
    <dd>getServletContextName():     <strong><%= application.getServletContextName() %> </strong></dd>
        <dd>class name:               <strong><%= application.getClass().getName() %>     </strong></dd>
        <dd>getContextPath():         <strong><%= application.getContextPath() %>        </strong></dd>
        <dd>getServletContextName():  <strong><%= application.getServletContextName() %> </strong></dd>
        <dd>getRealPath("docs"):      <strong><%= application.getRealPath("docs") %>     </strong></dd>
        <dd> <dl>
        <dt><strong>Attributes</strong></dt>
        <%
            sb = new StringBuffer();
            attrNames = application.getAttributeNames();
            while(attrNames.hasMoreElements()){
                String name = attrNames.nextElement();
                Object value = application.getAttribute(name);

                sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
            }
            out.println(sb.toString());
        %>
        </dl></dd>
        <dd> <dl>
        <dt><strong>InitParams</strong></dt>
        <%
            sb = new StringBuffer();
            paramNames = application.getInitParameterNames();
            while(paramNames.hasMoreElements()){
                String name = paramNames.nextElement();
                String value = application.getInitParameter(name);

                sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
            }
            out.println(sb.toString());
        %>
        </dl></dd>


    <dt><strong>config</strong></dt>
        <dd>class name:     <strong><%= config.getClass().getName() %> </strong></dd>
        <dd>servletName():  <strong><%= config.getServletName() %>     </strong></dd>

            <dd> <dl>
            <dt><strong>InitParams</strong></dt>
            <%
                sb = new StringBuffer();
                paramNames = config.getInitParameterNames();
                while(paramNames.hasMoreElements()){
                    String name = paramNames.nextElement();
                    String value = config.getInitParameter(name);

                    sb.append("<dd>").append(name).append(": <strong>").append(value).append("</strong></dd>\n");
                }
                out.println(sb.toString());
            %>
            </dl></dd>



    <dt><strong>pageContext</strong></dt>
            <dd>class name:     <strong><%= pageContext.getClass().getName() %> </strong></dd>

    <dt><strong>page</strong></dt>
    <dd>class name:     <strong><%= page.getClass().getName() %> </strong></dd>



</dl>




</BODY>
</HTML>
