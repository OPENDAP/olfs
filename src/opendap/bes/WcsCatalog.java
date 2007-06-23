/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

import thredds.servlet.DataRootHandler;
import thredds.catalog.*;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

import opendap.coreServlet.ReqInfo;

/**
 * User: ndp
 * Date: Jun 18, 2007
 * Time: 11:26:40 AM
 */
public class WcsCatalog implements DataRootHandler.ConfigListener {

    private static Logger log;
    private static ReentrantReadWriteLock configLock;

    private static HashMap<String,InvDataset> wcsDatasets;

    public WcsCatalog(){

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        configLock = new ReentrantReadWriteLock();
        wcsDatasets = null;
    }

    /**
     * Recieve notification that configuration has started.
     */
    public void configStart(){

        log.debug("Configuration START. Locking WCS Catalog.");
        configLock.writeLock().lock();

        wcsDatasets = new HashMap<String,InvDataset>();

    }

    /**
     * Recieve notification that configuration has completed.
     */
    public void configEnd(){

        configLock.writeLock().unlock();
        log.debug("Configuration END. Unlocked WCS Catalog.");
    }

    /**
     * Recieve notification on the inclusion of a configuration catalog.
     * @param catalog the catalog being included in configuration.
     */
    public void configCatalog( InvCatalog catalog ){
        log.debug("configCatalog()");

    }

    /**
     * Recieve notification that configuration has found a dataset.
     * @param dataset the dataset found during configuration.
     */
    public void configDataset( InvDataset dataset ){



        if(isWcsDataset(dataset)){
            String msg = "WCS Dataset: \n";
            msg += "    Name: " + dataset.getName() + "\n";

            InvAccess access = dataset.getAccess(ServiceType.OPENDAP);

            if(access!=null){

                String urlPath = access.getUrlPath();
                msg += "      InvAccess.getUrlPath:             " + urlPath + "\n";

                if(wcsDatasets.containsKey(urlPath)){
                    log.error("configDataset() - Access for WCS dataset is " +
                            "not unique! Ignoring THREDDS catalog entry. " +
                            "InvDataset.getName(): " + dataset.getName() + "  " +
                            "Access.getUrlPath(): " + urlPath);
                }
                else {
                    wcsDatasets.put(urlPath,dataset);
                }



            }
            else
              msg += "No Access!";


            log.debug("configDataset()\n"+msg);

        }






    }


    public static boolean isWcsDataset(HttpServletRequest req){

        String reqUrlPath = ReqInfo.getUrlPath(req);

        configLock.readLock().lock();

        boolean ret =  wcsDatasets.containsKey(reqUrlPath);

        configLock.readLock().unlock();

        return ret;

    }

    public static boolean isWcsDataset(String dataset){

        String reqUrlPath = dataset.startsWith("/")?dataset.substring(1,dataset.length()):dataset;

        configLock.readLock().lock();

        boolean ret =  wcsDatasets.containsKey(reqUrlPath);

        configLock.readLock().unlock();

        return ret;

    }



    public static String getWcsRequestURL(HttpServletRequest req){
        String reqUrlPath = ReqInfo.getUrlPath(req);
        String wcsURL = null;


        configLock.readLock().lock();

        InvDataset id = wcsDatasets.get(reqUrlPath);

        if(id!=null){
            wcsURL = getPropertyValue(id,"wcs-request");
        }

        configLock.readLock().unlock();

        return wcsURL;

    }


    public static String getWcsRequestURL(String dataset){
        String reqUrlPath = dataset.startsWith("/")?dataset.substring(1,dataset.length()):dataset;
        String wcsURL = null;


        configLock.readLock().lock();

        InvDataset id = wcsDatasets.get(reqUrlPath);

        if(id!=null){
            wcsURL = getPropertyValue(id,"wcs-request");
        }

        configLock.readLock().unlock();

        return wcsURL;

    }


    public static String getDatasetPropertyValue(HttpServletRequest req,
                                          String propertyName){
        String reqUrlPath = ReqInfo.getUrlPath(req);
        String value = null;


        configLock.readLock().lock();

        InvDataset id = wcsDatasets.get(reqUrlPath);

        if(id!=null){
            value = getPropertyValue(id,propertyName);
        }

        configLock.readLock().unlock();

        return value;

    }


    public static boolean isWcsDataset(InvDataset id){

        Iterator ici = id.getProperties().iterator();
        InvProperty prop;


        while(ici.hasNext()){
            prop = (InvProperty) ici.next();

            if(prop.getName().equalsIgnoreCase("wcs-request")){
                return true;
            }
        }

        return false;

    }



    public static  String getPropertyValue(InvDataset id, String propertyName){
        Iterator ici = id.getProperties().iterator();
        InvProperty prop;


        while(ici.hasNext()){
            prop = (InvProperty) ici.next();

            if(prop.getName().equalsIgnoreCase(propertyName)){
                return prop.getValue();
            }

        }

        return null;
    }




}
