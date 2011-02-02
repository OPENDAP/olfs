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
package opendap.wcs.gatewayClient;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collection;

/**
 * User: ndp
 * Date: Mar 12, 2008
 * Time: 4:09:27 PM
 */
public class WcsManager {

    private static Logger log;
    private static String confFilename;
    private static Document config;
    private static HashMap<String,WcsService> services;
    private static HashMap<String, Project> projects;

    private static boolean isIntialized=false;


    public static void init(String fname) throws Exception {


        log = org.slf4j.LoggerFactory.getLogger(WcsManager.class);

        log.debug("Configuring...");

        if(isIntialized){
            log.error(" Configuration has already been done. isInitialized(): "+isIntialized);
            return;
        }

        services = new HashMap<String,WcsService>();
        projects = new HashMap<String, Project>();

        String msg;

        File configFile = new File(fname);

        if(!configFile.exists()){
            msg = "Cannot find file: "+fname;
            log.error(msg);
            throw new IOException(msg);
        }

        if(!configFile.canRead()){
            msg = "Cannot read file: "+fname;
            log.error(msg);
            throw new IOException(msg);
        }

        log.debug("Parsing XML file: "+fname);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        FileInputStream fis = new FileInputStream(fname);
        try {
            config  = sb.build(fis);
        }
        finally {

            try{
                fis.close();
            }
            catch(IOException e){
                log.error("Failed to close file "+fname+" Error MEssage: "+e.getMessage());
            }
            
        }

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //xmlo.output(config, System.out);

        configure();

        confFilename = fname;

        isIntialized = true;


    }

    public static void destroy(){

        Collection<WcsService> servCol = services.values();

        for (WcsService service : servCol) {
            service.destroy();
        }
        log.debug("Destroyed");

    }




    public static String getConfigFilename(){
        return confFilename;
    }

    private static void configure( ) throws Exception {

        log.debug("Processing configuration document...");

        Element wcsConfig = config.getRootElement();

        if(!wcsConfig.getName().equals("WCSConfig")){
            String msg = "Cannot build a "+WcsManager.class.getName()+" using " +
                    "<"+wcsConfig.getName()+"> element.";
            log.error(msg);
            throw new Exception(msg);
        }



        List servers = wcsConfig.getChildren("WCSService");
        if(servers.isEmpty()){
            String msg = "WCS Configurations MUST " +
                "contain one or more (1..*) <WCSService> elements.";
            log.error(msg);
            throw new Exception(msg);
        }
        WcsService service;

        Iterator i = servers.iterator();
        while(i.hasNext()){
            Element e = (Element)i.next();
            service = new WcsService(e);
            services.put(service.getName(),service);
        }




        List projList = wcsConfig.getChildren("Project");
        if(projList.isEmpty()) {
            String msg = "WCS Configurations MUST " +
                "contain one or more (1..*) <Project> elements.";
            log.error(msg);
            throw new Exception(msg);
        }

        Project project;

        i = projList.iterator();
        while(i.hasNext()){
            Element e = (Element)i.next();
            project = new Project(e);
            log.debug("Adding project: "+project.getName());
            projects.put(project.getName(),project);
        }


    }

    public static  Project getProject(String name){
        return projects.get(name);
    }

    public static int getProjectCount(){
        return projects.size();
    }

    public static String[] getProjectNames(){

        Collection<Project> pc = projects.values();

        String[] names = new String[projects.size()];

        int j=0;
        for (Project proj : pc) {
            names[j++] = proj.getName();
        }

        return names;
    }
    public static Collection<Project> getProjects(){
        return projects.values();
    }


    public static int getWcsServiceCount(){
        return services.size();
    }

    public static WcsService getWcsService(String name){
        return services.get(name);
    }


    public static String[] getWcsServiceNames(){

        Collection<WcsService> pc = services.values();

        String[] names = new String[services.size()];

        int j=0;
        for (WcsService serv : pc) {
            names[j++] = serv.getName();
        }

        return names;
    }

    public static Collection<WcsService> getWcsServices(){
        return services.values();
    }


}
