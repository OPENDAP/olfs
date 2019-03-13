/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.PathBuilder;
import opendap.bes.caching.BesNodeCache;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:07 AM
 */
public class BESManager {

    private static final Logger LOG = LoggerFactory.getLogger(BESManager.class);
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final Vector<BesGroup> BES_COLLECTION = new Vector<>();
    private static final AtomicBoolean CONFIGURED = new AtomicBoolean(false);
    private static final ReentrantLock LOCK = new ReentrantLock() ;

    public static final String BES_MANAGER_CONFIG_ELEMENT = "BESManager";


    private static Element config = null;
    private static BesGroup rootGroup;

    /**
     * This is a singleton class so we make the default constructor private. All of the methods are static and so
     * nobody ever needs an instance
     */
    private BESManager(){}

    public static void init(ServletContext servletContext, Element config) throws BadConfigurationException {

        LOCK.lock();
        try {
            if (INITIALIZED.get()) return;

            BESManager.config = (Element) config.clone();
            configure(servletContext, config);
            LOG.info("Initialized.");
            INITIALIZED.set(true);
        }
        finally {
            LOCK.unlock();
        }
    }

    public static boolean isInitialized(){
        return INITIALIZED.get();
    }




    public void destroy(){
        shutdown();
        LOG.info("Destroy complete.");
    }


    public static Element getConfig(){
        return (Element) config.clone();
    }


    private static void configure(ServletContext servletContext, Element besConfiguration) throws BadConfigurationException {

        if(CONFIGURED.get()) return;

        List besList = besConfiguration.getChildren("BES");

        if (besList.isEmpty())
            throw new BadConfigurationException("OLFS Configuration must " +
                    "contain at LEAST one BES configuration element. And " +
                    "the value of it's prefix element  must be \"/\".");


        boolean foundRootBES = false;
        BES bes;
        BESConfig besConfig;
        Element besConfigElement;
        for (Object o : besList) {
            besConfigElement = (Element) o;
            besConfig = new BESConfig(besConfigElement);
            bes = new BES(besConfig);

            BesGroup groupForThisPrefix = getBesGroup(bes.getPrefix());

            // Since the BES with prefix '/' will always match we have to check to make sure
            // that if a non null group is returned that it's prefix does in fact match the one
            // for the new BES - if it doesn't match the returned group prefix then we need to make a new group.
            if (groupForThisPrefix == null || !groupForThisPrefix.getGroupPrefix().equals(bes.getPrefix())) {
                groupForThisPrefix = new BesGroup(bes.getPrefix());
                BES_COLLECTION.add(groupForThisPrefix);
            }


            groupForThisPrefix.add(bes);


            if (groupForThisPrefix.getGroupPrefix().equals("/")) {
                rootGroup = groupForThisPrefix;
                foundRootBES = true;
            }
        }

        if (!foundRootBES)
            throw new BadConfigurationException("OLFS Configuration must " +
                    "contain at LEAST one BES configuration element. Whose " +
                    "prefix is \"/\". (Why? Think about it...)");


        Element nodeCache = besConfiguration.getChild(BesNodeCache.NODE_CACHE_ELEMENT_NAME);
        // If nodeCache is null no action needs to be taken because the NodeCache
        // will be disabled, and that's fine.
        if(nodeCache!=null){
            BesNodeCache.init(nodeCache);
        }

        Element siteMapCache = besConfiguration.getChild(BESSiteMap.SITE_MAP_CACHE_ELEMENT_NAME);
        // The SiteMap cache is required, so if it's not in the configuration
        // then we need to gin one up.
        if(siteMapCache==null) {
            siteMapCache = new Element(BESSiteMap.SITE_MAP_CACHE_ELEMENT_NAME);
        }
        String cacheFile = siteMapCache.getAttributeValue(BESSiteMap.CACHE_FILE_ATTRIBUTE_NAME);
        if(cacheFile==null) {
            // Critically, we need to tell the BESSiteMap where to cache so if
            // the cache file is missing from the configuration we sort that
            // out using our configuration stack.
            ServletUtil.getConfigPath(servletContext);
            String defaultSiteMapCacheFile = PathBuilder.pathConcat(ServletUtil.getConfigPath(servletContext),"cache");
            defaultSiteMapCacheFile = PathBuilder.pathConcat(defaultSiteMapCacheFile,"SiteMap.cache");
            siteMapCache.setAttribute(BESSiteMap.CACHE_FILE_ATTRIBUTE_NAME, defaultSiteMapCacheFile);
        }
        // We don't need a refresh interval because if it's missing the
        // default BESSiteMap.DEFAULT_CACHE_REFRESH_INTERVAL will be used
        BESSiteMap.init(siteMapCache);
        CONFIGURED.set(true);

    }


    public static void addBes(BES bes) throws BadConfigurationException {

        Iterator<BesGroup> i = BESManager.getBesGroups();

        BesGroup bg, myGroup = null;
        while(i.hasNext()){
            bg = i.next();
            if(bg.getGroupPrefix().equals(bes.getPrefix())){
                myGroup = bg;
            }
        }
        if(myGroup != null){
            myGroup.add(bes.getNickName(),bes);
        }
        else {
            myGroup = new BesGroup(bes.getPrefix());
            myGroup.add(bes);
            BES_COLLECTION.add(myGroup);
        }
    }


    public static boolean isConfigured(){
        return CONFIGURED.get();
    }






    public static BES getBES(String path) throws BadConfigurationException {

        if(path==null)
            path = "/";

        if(path.indexOf("/")!=0){
            LOG.debug("Pre-pending / to path: "+ path);
            path = "/"+path;
        }

        BesGroup besGroupToServicePath = null;
        String prefix;
        for(BesGroup besGroup : BES_COLLECTION){
            prefix = besGroup.getGroupPrefix();

            if(path.indexOf(prefix) == 0){
                if(besGroupToServicePath == null){
                    besGroupToServicePath = besGroup;
                }
                else {
                    if(prefix.length() > besGroupToServicePath.getGroupPrefix().length())
                        besGroupToServicePath = besGroup;
                }

            }
        }

        BES bes = null;

        if(besGroupToServicePath!=null)
            bes =  besGroupToServicePath.getNext();



        if (bes == null) {
            String msg = "There is no BES to handle the requested data source: " + Scrub.urlContent(path);
            LOG.error(msg);
            throw new BadConfigurationException(msg);
        }

        return bes;


    }





    public static BesGroup getBesGroup(String path){

        if(path==null)
            path = "/";

        if(path.indexOf("/")!=0){
            LOG.debug("Pre-pending / to path: "+ path);
            path = "/"+path;
        }

        BesGroup result = null;
        String prefix;
        for(BesGroup besGroup : BES_COLLECTION){
            prefix = besGroup.getGroupPrefix();
            if(path.indexOf(prefix) == 0){
                if(result == null){
                    result = besGroup;
                }
                else {
                    if(prefix.length() > result.getGroupPrefix().length())
                        result = besGroup;
                }
            }
        }
        if(result == null)
            result = rootGroup;

        return result;
    }


    public static Iterator<BesGroup> getBesGroups(){

        return BES_COLLECTION.listIterator();

    }


    public static void shutdown(){
        for(BesGroup besGroup : BES_COLLECTION){
            LOG.debug("Shutting down BesGroup for prefix '" + besGroup.getGroupPrefix()+"'");
            besGroup.destroy();
        }
        LOG.debug("All BesGroup's have been shut down.");

    }


    public static Document getCombinedVersionDocument() throws JDOMException, IOException, PPTException, BadConfigurationException, BESError {



        Document doc = new Document();
        doc.setRootElement(new Element("HyraxCombinedVersion"));

        // Add a version element for Hyrax (which is a combination of many things)
        doc.getRootElement().addContent(opendap.bes.Version.getHyraxVersionElement());
        // Add a version element for this, the OLFS server
        doc.getRootElement().addContent(opendap.bes.Version.getOLFSVersionElement());


        //doc.getRootElement().addNamespaceDeclaration();  // Maybe someday?
        Element besVer;
        Document tmp;

        for(BesGroup besGroup : BES_COLLECTION){

            Document besGroupVerDoc = besGroup.getGroupVersion();
            doc.getRootElement().addContent(besGroupVerDoc.detachRootElement());

        }



        return doc;
    }




    public static Document getGroupVersionDocument(String path) throws Exception {

        return getBesGroup(path).getGroupVersion();


    }

    public static Document getVersionDocument(String path, String besName) throws Exception {

        BesGroup besGroup = getBesGroup(path);
        BES bes =  besGroup.get(besName);

        return bes.getVersionDocument();


    }

}
