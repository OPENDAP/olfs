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

package opendap.bes;

import org.jdom.Document;
import org.jdom.Element;

import java.util.Iterator;
import java.util.Vector;
import java.util.List;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.DispatchServlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:07 AM
 */
public class BESManager implements DispatchHandler {


    private static Vector<BES> _besCollection;

    private static boolean _isConfigured = false;

    private static  org.slf4j.Logger log = null;

    private static boolean initialized;





    public BESManager(){
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }

    public void init(HttpServlet servlet, Element config) throws Exception{

        if(initialized) return;

        configure(config);

        log.info("Initialized.");

        initialized = true;

    }





    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception{

        return false;

    }

    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception{

        throw new Exception("handleRequest(): ERROR! THis method should never " +
                "be called because requestCanBeHandled() always returns false.");

    }

    public long getLastModified(HttpServletRequest request){
        return -1;
    }

    public void destroy(){
        shutdown();
        log.info("Destroy complete.");

    }



    public static void configure(Element besConfiguration) throws Exception {

        if(_isConfigured) return;

        _besCollection  = new Vector<BES>();

        List besList = besConfiguration.getChildren("BES");

        if(besList.isEmpty())
            throw new BadConfigurationException("OLFS Configuration must " +
                    "contain at LEAST one BES configuration element. And " +
                    "the value of it's prefix element  must be \"/\".");


        boolean foundRootBES = false;
        BES bes;
        BESConfig besConfig;
        Element besConfigElement;
        for (Object o : besList) {
            besConfigElement = (Element) o;
            besConfig   = new BESConfig(besConfigElement);
            bes = new BES(besConfig);

            _besCollection.add(bes);

            if(bes.getPrefix().equals("/"))
                foundRootBES = true;
        }

        if(!foundRootBES)
            throw new BadConfigurationException("OLFS Configuration must " +
                    "contain at LEAST one BES configuration element. Whose " +
                    "prefix is \"/\". (Why? Think about it...)");

        _isConfigured = true;


    }


    //public static void configure(OLFSConfig olfsConfig) throws Exception {

    //    configure(olfsConfig.getBESConfig());

    //}


    public static boolean isConfigured(){
        return _isConfigured;
    }




    public static BES getBES(String path){

        if(path==null)
            path = "/";

        if(path.indexOf("/")!=0){
            log.debug("Pre-pending / to path: "+ path);
            path = "/"+path;
        }

        BES result = null;
        String prefix;
        for(BES bes : _besCollection){
            prefix = bes.getPrefix();

            if(path.indexOf(prefix) == 0){
                if(result == null){
                    result = bes;
                }
                else {
                    if(prefix.length() > result.getPrefix().length())
                        result = bes;
                }

            }
        }

        return result;

    }

    public static Iterator<BES> getBES(){

        return _besCollection.listIterator();

    }


    public static void shutdown(){
        for(BES bes : _besCollection){
            log.debug("Shutting down BesPool " + bes);
            bes.destroy();
        }
        log.debug("All BesPools have been shut down.");

    }


    public static Document getCombinedVersionDocument() throws Exception {



        Document doc = new Document();
        doc.setRootElement(new Element("OPeNDAP-Version"));
        Element besVer;
        Document tmp;
        for (BES bes : _besCollection) {
            tmp = bes.getVersionDocument();
            besVer = tmp.getRootElement();
            besVer.detach();
            doc.getRootElement().addContent(besVer);
        }

        // Add a version element for this, the OLFS server
        doc.getRootElement().addContent(opendap.bes.Version.getOLFSVersionElement());

        // Add a version element for this, the OLFS server
        doc.getRootElement().addContent(opendap.bes.Version.getHyraxVersionElement());

        return doc;
    }




    public static Document getVersionDocument(String path) throws Exception {

        return getBES(path).getVersionDocument();
    }







//    public static void removeThreadCache(Thread t){
//        threadCache.remove(t);
//        log.info("Removing ThreadCache for thread "+t.getName());
//    }








}
