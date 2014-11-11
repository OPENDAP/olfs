/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2014 OPeNDAP, Inc.
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

package opendap.auth;

import org.jdom.Element;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ndp on 9/26/14.
 */
public abstract class PolicyDecisionPoint {


    public abstract void init(JSONObject config);
    public abstract void init(Element config) throws ConfigurationException;


    public abstract boolean addPolicy(Policy policy);

    public abstract boolean removePolicy(Policy policy);

    public abstract boolean evaluate(String userId, String resourceId, String queryString, String actionId);

    public static PolicyDecisionPoint pdpFactory(Element config) throws ConfigurationException {

        Logger log = LoggerFactory.getLogger(PolicyDecisionPoint.class);
        String msg;


        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            log.error("pdpFactory():  {}",msg);
            throw new ConfigurationException(msg);
        }


        String pdpClassName = config.getAttributeValue("class");

        if(pdpClassName==null) {
            msg = "PolicyDecisionPoint definition must contain a \"class\" attribute whose value is the class name of the PolicyDecisionPoint implementation to be created.";
            log.error("pdpFactory(): {}",msg);
            throw new ConfigurationException(msg);
        }

        try {

            log.debug("pdpFactory(): Building PolicyDecisionPoint: " + pdpClassName);
            Class classDefinition = Class.forName(pdpClassName);
            PolicyDecisionPoint pdp = (PolicyDecisionPoint) classDefinition.newInstance();

            pdp.init(config);

            return pdp;


        } catch (Exception e) {
            msg = "Unable to manufacture an instance of "+pdpClassName+"  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            log.error("pdpFactory(): {}"+msg);
            throw new ConfigurationException(msg, e);

        }


    }


}
