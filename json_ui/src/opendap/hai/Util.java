/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Hyrax" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.hai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/28/11
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {


    public static HashMap<String, String> processQuery(HttpServletRequest request){

        Logger log = LoggerFactory.getLogger("opendap.bes.BesControlApi");
        HashMap<String, String> kvp = new HashMap<String, String>();

        StringBuilder sb = new StringBuilder();
        Map<String,String[]> params = request.getParameterMap();
        for(String name: params.keySet()){
            sb.append(name).append(" = ");
            String[] values = params.get(name);
            if(values.length>1){
                log.warn("Multiple values found for besctl parameter '{}'. Will use the last one found.", name);
            }
            for(String value: values){
                sb.append("'").append(value).append("' ");
                kvp.put(name,value);
            }
            sb.append("\n");
        }

        log.debug("Parameters:\n{}",sb);



        return kvp;


    }


}
