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

import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/27/11
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class BesConfigurationModule {

    private String name;

    private Element _config;


    BesConfigurationModule(Element config){
        _config = (Element) config.clone();

        name =_config.getAttributeValue("module");

        if(name==null){
            name = "Unnamed";
        }


    }


    public String getName(){
        return name;

    }

    public String getShortName(){

        String sName = name;

        if(sName.endsWith(".conf")){
            sName = sName.substring(0,sName.lastIndexOf(".conf"));
        }

        return sName;

    }

    public void setName(String s){
        name = s;
    }


    public String getConfig(){
        return _config.getText();
    }

    public void setConfig(String config){
        _config.setText(config);
    }


    public Element getConfigElement(){
        return (Element) _config.clone();
    }


}
