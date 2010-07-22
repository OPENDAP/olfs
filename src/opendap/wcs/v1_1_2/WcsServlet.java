/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2;

import opendap.coreServlet.*;
import opendap.logging.LogUtil;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 * Stand alone servlet for WCS Services
 * User: ndp
 */
public class WcsServlet extends HttpServlet {


    private Logger log;
    private DispatchHandler httpService = null;

    private FormHandler formService = null;
    private PostHandler postService = null;
    private SoapHandler soapService = null;

    //private Document configDoc;


    public void init() throws ServletException {
        super.init();
        LogUtil.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());



        // Build Handler Objects
        httpService = new DispatchHandler();
        formService = new FormHandler();
        postService = new PostHandler();
        soapService = new SoapHandler();

        // Build configuration elements
        Element config  = new Element("config");
        Element prefix  = new Element("prefix");

//        System.out.println(ServletUtil.probeServlet(this));

        // ServletContext sc = this.getServletContext();
        // prefix.setText(sc.getContextPath());
        config.addContent(prefix);

        try {
            prefix.setText("/");
            httpService.init(this,config);
            prefix.setText("/form");
            formService.init(this,config);
            prefix.setText("/post");
            postService.init(this,config);
            prefix.setText("/soap");
            soapService.init(this,config);
           
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }



    public void doGet(HttpServletRequest req, HttpServletResponse resp){
        try {
            httpService.handleRequest(req, resp);
        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally{
            this.destroy();
        }
    }


    public void doPost(HttpServletRequest req, HttpServletResponse resp){
        try {

            if(postService.requestCanBeHandled(req)){
                postService.handleRequest(req,resp);
            }
            else if(soapService.requestCanBeHandled(req)){
                soapService.handleRequest(req,resp);
            }
            else if(formService.requestCanBeHandled(req)){
                formService.handleRequest(req,resp);
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally{
            this.destroy();
        }
    }



}
