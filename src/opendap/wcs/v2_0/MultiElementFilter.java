/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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

package opendap.wcs.v2_0;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.Filter;

import java.util.Vector;

/**
 *
 *
 */
public class MultiElementFilter implements Filter {

    private Vector<String> _targetNames;
    private Vector<Namespace> _targetNamespaces;


    public MultiElementFilter(String name, Namespace ns){
        _targetNames = new Vector<>();
        _targetNamespaces = new Vector<>();

        addTargetElement(name,ns);
    }

    public void addTargetElement(String name, Namespace ns){
        _targetNames.add(name);
        _targetNamespaces.add(ns);
    }

    public void addTargetElement(String name){
        addTargetElement(name, null);
    }


    public void addTargetNamespace(Namespace ns){
        addTargetElement(null, ns);
    }



    public boolean matches(Object obj){

        if(obj instanceof Element){

            Element candidate = (Element) obj;
            Namespace cNS = candidate.getNamespace();
            String cName  = candidate.getName();

            String targetName;
            Namespace targetNamespace;
            for(int i = 0; i< _targetNames.size() ; i++){
                targetName = _targetNames.get(i);
                targetNamespace = _targetNamespaces.get(i);
                if(cName!=null){
                    if(targetName == null){
                        if(targetNamespace==null)
                            return true;
                        if(cNS==null || cNS.equals(targetNamespace))
                            return true;
                    }
                    else {
                        if(cName.equals(targetName)){
                            if(targetNamespace==null)
                                return true;
                            if(cNS.equals(targetNamespace))
                                return true;
                        }
                    }
                }
                else {
                    if(targetNamespace==null)
                        return true;
                    if(cNS!=null && cNS.equals(targetNamespace))
                        return true;
                }
            }
        }
        return false;
    }
}
