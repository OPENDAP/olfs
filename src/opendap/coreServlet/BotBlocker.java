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
package opendap.coreServlet;

import org.jdom.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import java.util.Vector;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Oct 6, 2008
 * Time: 1:28:05 PM
 */
public class BotBlocker implements DispatchHandler {



    private static  org.slf4j.Logger log = null;

    private boolean initialized;


    private HashSet<String> ipAddresses;
    private Vector<Pattern> ipMatchPatterns;



    BotBlocker() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;
        ipAddresses = new HashSet<String>();
        ipMatchPatterns = new Vector<Pattern>();
    }

    /**
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The IsoDispatchHandler that creates an instance of IsoDispatchHandler will
     * pass itself into it along with the XML element that declared the
     * IsoDispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not availableInChunk by
     * interogating the IsoDispatchHandler methods.
     *
     * @param servlet This should be the IsoDispatchHandler that creates the
     * instance of DispatychHandler that is being intialized.
     * @param config A JDOM Element objct containing the XML Element that
     * announced which implementation of IsoDispatchHandler to use. It may (or
     * may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see DispatchServlet
     */
    public void init(HttpServlet servlet, Element config) throws Exception
    {
        if(initialized) return;

        configure(config);

        log.info("Initialized.");

        initialized = true;


    }

    private void configure(Element config){


        for (Object o : config.getChildren("IpAddress")) {
            String ipAddr = ((Element) o).getTextTrim();
            ipAddresses.add(ipAddr);
        }

        for (Object o : config.getChildren("IpMatch")) {
            String ipMatch = ((Element) o).getTextTrim();
            Pattern ipP = Pattern.compile(ipMatch);
            ipMatchPatterns.add(ipP);
        }




    }





    /**
     *
     * @param request The request to be handled.
     * @return True if the IsoDispatchHandler can service the request, false
     * otherwise.
     * @throws Exception When the bad things happen.
     */
    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        String remoteAddr = request.getRemoteAddr();

        if(ipAddresses.contains(remoteAddr)){
            log.info("The ip address: "+request.getRemoteAddr()+" is " +
                    "on the list of blocked adresses");
            return true;
        }

        for(Pattern p: ipMatchPatterns){
            if(p.matcher(remoteAddr).matches()){
                log.info("The ip address: "+request.getRemoteAddr()+" matches " +
                        "the the pattern: \""+p.pattern());

                return true;
            }
        }

        return false;


    }

    /**
     *
     * @param request The request to be handled.
     * @param response The response object into which the response information
     * will be placed.
     * @throws Exception When the bad things happen.
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {


        //response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // For some reason using sendError() causes the HTTP heieaders to say
        // return status is 200 OK and not 403 Forbidden... Weird.
        //response.sendError(HttpServletResponse.SC_FORBIDDEN);

        log.info("Denied access to "+request.getRemoteAddr()+" because it is " +
                "either on the list, or matches a blocking pattern.");



    }






    /**
     *
     *
     * @param req The request for which we need to get a last modified date.
     * @return The last modified date of the URI referenced in th request.
     * @see javax.servlet.http.HttpServlet
     */
    public long getLastModified(HttpServletRequest req) {
        return -1;

    }


    /**
     * Called when the servlet is shutdown. Here is where to clean up open
     * connections etc.
     */
    public void destroy() {

    }


}
