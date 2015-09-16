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
package opendap.hai;

import opendap.coreServlet.Scrub;
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
        if(params != null){

            for(Map.Entry<String,String[]> e: params.entrySet()){
                String name = e.getKey();
                String values[] = e.getValue();
                if(values.length>1){
                    log.warn("Multiple values found for besctl parameter '{}'. Will use the last one found.", Scrub.urlContent(name));
                }
                for(String value: values){
                    sb.append("'").append(value).append("' ");
                    kvp.put(name,value);
                }
                sb.append("\n");
            }

            log.debug("Parameters:\n{}",sb);
        }


        return kvp;


    }


}
