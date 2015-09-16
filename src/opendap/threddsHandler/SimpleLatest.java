/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.TreeMap;

/**
 * Created by ndp on 4/19/15.
 */
public class SimpleLatest implements Proxy {

    String _name;
    boolean _top;
    String _serviceName;

    public SimpleLatest(Element e){

        _name = e.getAttributeValue(THREDDS.NAME);
        if(_name==null)
            _name="latest";

        _top = true;
        String s = e.getAttributeValue(THREDDS.TOP);
        if(s!=null){
            _top = Boolean.parseBoolean(s);
        }


        _serviceName = e.getAttributeValue(THREDDS.SERVICE_NAME);
        if(_serviceName==null)
            _serviceName = "latest";

    }


    @Override
    public Element getProxyDataset(TreeMap<String, Element> datasets) {


        Element lastDataset = datasets.get(datasets.lastKey());

        Element proxy = (Element) lastDataset.clone();

        proxy.setAttribute(THREDDS.NAME,_name);
        proxy.setAttribute(THREDDS.SERVICE_NAME,_serviceName);

        return proxy;
    }

    public boolean isTop() {
        return _top;
    }
}
