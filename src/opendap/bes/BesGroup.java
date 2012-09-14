/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
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


import opendap.coreServlet.RequestCache;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;


/**
 *
 *
 **/
public class BesGroup extends CyclicGroup<BES> {


    Logger log;
    private String prefix;



    public BesGroup(String prefix) throws BadConfigurationException {
        log = LoggerFactory.getLogger(getClass());
        this.prefix = prefix;
        

    }


    /*
    public void addBes(BESConfig config) throws Exception {

        if(!config.getPrefix().equals(prefix))
            throw new BadConfigurationException("Members of a BesGroup must all have the same prefix. " +
                    "This ring has prefix '"+prefix+"' the BESConfig  has a prefix of '"+config.getPrefix()+"'.");

        BES bes = new BES(config);
        add(bes);


    }
    */

    public boolean add(BES bes) {
        if(!bes.getPrefix().equals(prefix)){
            log.error("Members of a BesGroup must all have the same prefix. " +
                    "This ring has prefix '" + prefix + "' the BES you tried to add has a prefix of '" + bes.getPrefix() + "'.");
         return false;
        }

        String name = bes.getNickName();

        if(name==null){
            name = prefix+"-"+size();
            bes.setNickName(name);
        }

        return super.add(bes.getNickName(),bes);

    }






    public String getGroupPrefix(){
        return prefix;
    }


    public void destroy(){

        Object[] members = drain();
        
        for(Object o : members){
            BES bes = (BES)o;
            bes.destroy();
        }

    }

    @Override
    public BES getNext(){


        BES bes;

        String responseCacheKey = this.getClass().getName()+".getNext()";

        Object o  = RequestCache.get(responseCacheKey);

        if(o == null){
            bes = super.getNext();

            RequestCache.put(responseCacheKey,bes);

        }
        else {
            bes = (BES)o;
        }


        return bes;

    }



    public Document getGroupVersion() throws JDOMException, BESError, IOException, BadConfigurationException, PPTException {
        Element besGroupElement = new Element("BesGroup");
        besGroupElement.setAttribute("prefix", getGroupPrefix());

        for(int i=0; i<size() ;i++){
            BES bes = get(i);

            Document besVerDoc = bes.getVersionDocument();
            Element verElement = besVerDoc.detachRootElement();
            besGroupElement.addContent(verElement);
        }

        return new Document(besGroupElement);


    }

    public TreeSet<String> getCommonDapVersions()  {


        TreeSet<String> commonDapVersions  = new TreeSet<String>();
        Document groupVersionDoc = null;


        try {
            groupVersionDoc = getGroupVersion();
        } catch (Exception e) {
            log.error("Failed to retrieve BesGroup version document!");
        }


        if (groupVersionDoc != null) {

            Element besGroupElem = groupVersionDoc.getRootElement();

            List besList  = besGroupElem.getChildren("BES", opendap.namespaces.BES.BES_NS);

            boolean isFirst = true;
            for(Object o_beselem : besList){
                Element besElement = (Element) o_beselem;


                List serviceVersionList = besElement.getChildren("serviceVersion",opendap.namespaces.BES.BES_NS);

                if(serviceVersionList.isEmpty())
                    log.error("The BES with prefix='"+besElement.getAttributeValue("prefix")+"' has no defined services! " +
                            "(bes:serviceRef elements are missing");

                Vector<String> dapVersions = getDapVersions(serviceVersionList);
                if(isFirst) {
                    commonDapVersions.addAll(dapVersions);
                    isFirst = false;
                }
                else {
                    Vector<String> dropList = new Vector<String>();
                    for(String version: commonDapVersions){
                        if(!dapVersions.contains(version)){
                            // no matching version in new one? Drop it.
                            dropList.add(version);
                        }
                    }
                    commonDapVersions.removeAll(dropList);
                }
            }

        }

        if(commonDapVersions.isEmpty()){
            commonDapVersions.add("DAP_VERSION_UNKNOWN");
        }


        return commonDapVersions;

    }

    public Vector<String> getDapVersions(List serviceVersionList ){

        Vector<String> dapVersions = new Vector<String>();

        for(Object o_serviceVersion : serviceVersionList){
            Element serviceVersion = (Element) o_serviceVersion;
            if(serviceVersion.getAttributeValue("name").equalsIgnoreCase("dap")){
                for (Object o : serviceVersion.getChildren("version",opendap.namespaces.BES.BES_NS)) {
                    Element dapVersionElement = (Element) o;
                    String dapVersionString = dapVersionElement.getTextTrim();
                    dapVersions.add(dapVersionString);
                }
            }
        }

        return dapVersions;

    }



    public TreeSet<String> getGroupComponentVersions() throws Exception {

        Document groupVersionDoc = getGroupVersion();
        TreeSet<String> components = new TreeSet<String>();


        if (groupVersionDoc != null) {

            Element besGroupElem = groupVersionDoc.getRootElement();

            List besGroupList  = besGroupElem.getChildren("BES", opendap.namespaces.BES.BES_NS);


            for(Object o_beselem : besGroupList){
                Element besElement = (Element) o_beselem;


                List libraries = besElement.getChildren("library", opendap.namespaces.BES.BES_NS);
                List modules = besElement.getChildren("module", opendap.namespaces.BES.BES_NS);

                if (libraries.isEmpty())
                    log.error("The BES with prefix='" + besElement.getAttributeValue("prefix") + "' is running without code libraries! " +
                            "(bes:library elements are missing");

                if (modules.isEmpty())
                    log.error("The BES with prefix='" + besElement.getAttributeValue("prefix") + "' is running without modules! " +
                            "(bes:module elements are missing");



                for(Object o : libraries){
                    Element lib = (Element) o;
                    String libName = lib.getAttributeValue("name") + "/" + lib.getTextTrim();
                    components.add(libName);
                }


                for(Object o : modules){
                    Element module = (Element) o;
                    String moduleName = module.getAttributeValue("name") + "/" + module.getTextTrim();
                    components.add(moduleName);
                }
            }

        }

        return components;

    }


    @Override
    public BES[] toArray(){
        BES[] myBesSet = new BES[size()];
        return super.toArray(myBesSet);        // Return it to the requester.
    }




}
