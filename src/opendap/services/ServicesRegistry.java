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
package opendap.services;

import opendap.webstart.JwsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 5, 2010
 * Time: 5:06:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServicesRegistry {

    private static Logger log = LoggerFactory.getLogger(ServicesRegistry.class);

    private static ConcurrentHashMap<String, Service> services;
    private static ConcurrentHashMap<String, WebServiceHandler> _webServiceHandlers;
    private static ConcurrentHashMap<String, JwsHandler> _jwsHandlers;
    static {
        services = new ConcurrentHashMap<String, Service>();
        _webServiceHandlers = new ConcurrentHashMap<String, WebServiceHandler>();
        _jwsHandlers = new ConcurrentHashMap<String, JwsHandler>();
    }


    public static int addService(Service service){
        if(service instanceof WebServiceHandler){
            _webServiceHandlers.put(service.getServiceId(), (WebServiceHandler) service);

        }
        else if(service instanceof JwsHandler){
            _jwsHandlers.put(service.getServiceId(), (JwsHandler) service);

        }
        services.put(service.getServiceId(),service);
        return services.size();

    }

    public static int addServices(Collection<Service> services){

        for(Service s : services)
            addService(s);

        return services.size();
    }




    public static Map<String, JwsHandler> getJavaWebStartHandlers() {
        return Collections.unmodifiableMap(_jwsHandlers);
    }

    public static Map<String, WebServiceHandler> getWebServiceHandlers() {
        return Collections.unmodifiableMap(_webServiceHandlers);
    }


    public static Service getServiceById(String id){
        return services.get(id);
    }

    public static JwsHandler getJwsHandlerById(String id){
        return _jwsHandlers.get(id);
    }

    public static WebServiceHandler getWebServiceById(String id){
        return _webServiceHandlers.get(id);
    }



}
