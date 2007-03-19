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

import org.jdom.Document;
import org.jdom.Element;

import java.util.Vector;
import java.util.List;
import java.util.Iterator;


/**
 * User: ndp
 * Date: Mar 19, 2007
 * Time: 11:39:07 AM
 */
public class BESManager {


    private static Vector<BES> _besCollection;

    private static boolean _isConfigured = false;


    public static void configure(Document olfsConfig) throws Exception {


        if(_isConfigured) return;

        _besCollection  = new Vector<BES>();

        List besList = olfsConfig.getRootElement().getChildren("BES");

        if(besList.isEmpty())
            throw new BadConfigurationException("OLFS Configuration must " +
                    "contain at LEAST one BES configuration element");

        Iterator i = besList.iterator();

        BES bes;
        BESConfig besConfig;
        Element besConfigElement;
        for (Object o : besList) {
            besConfigElement = (Element) o;
            besConfig   = new BESConfig(besConfigElement);
            bes = new BES(besConfig);

            _besCollection.add(bes);

        }

        _isConfigured = true;


    }


    public static BES getBES(String path){

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


    public static void shutdown(){
        for(BES bes : _besCollection){
            bes.shutdownBES();
        }

    }








}
