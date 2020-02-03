/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by ndp on 9/26/14.
 */
public class SimplePDP extends PolicyDecisionPoint {

    private Logger log;
    private Vector<Policy> policies;

    public SimplePDP(){
        log = LoggerFactory.getLogger(this.getClass());
        policies = new Vector<>();
    }

    private Policy policyFactory(Element policyDef) throws ConfigurationException {

        String msg;
        String policyImplementation = policyDef.getAttributeValue("class");
        if(policyImplementation == null){
            msg = "Policy definitions must contain a \"class\" attribute that defines the name of the Policy implementation class that is to be instantiated.";
            log.error("policyFactory(): {}",msg);
            throw new ConfigurationException(msg);
        }
        try {
            log.debug("Building Policy: " + policyImplementation);
            Class classDefinition = Class.forName(policyImplementation);
            Policy policy = (Policy) classDefinition.newInstance();
            policy.init(policyDef);
            return policy;

        } catch (Exception e) {
            msg = "Unable to manufacture a new "+policyImplementation+" instance.  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            log.error("policyFactory(): {}",msg);
            throw new ConfigurationException(msg, e);
        }
    }

    public void init(String configFileName) throws IOException, JDOMException, ConfigurationException {
        File configFile = new File(configFileName);
        Element config = opendap.xml.Util.getDocumentRoot(configFile);
        init(config);
    }





    @Override
    public void init(Element config) throws ConfigurationException {

        String msg;
        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            log.error("init() - {}",msg);
            throw new ConfigurationException(msg);
        }

        Iterator pItr = config.getChildren("Policy").iterator();
        while(pItr.hasNext()){
            Element policy = (Element) pItr.next();
            Policy p = policyFactory(policy);
            addPolicy(p);
        }
        Element memberships = config.getChild("Memberships");
        if(memberships==null)
            throw new ConfigurationException("Configuration is missing required element 'Memberships'");

        MembershipRulesManager.init(memberships);
   }

    @Override
    public boolean addPolicy(Policy policy) {

        log.debug("addPolicy() - Adding Policy {}",policy.toString());
        return policies.add(policy);
    }

    @Override
    public boolean removePolicy(Policy policy) {
        log.debug("removePolicy() - Removing Policy {}",policy.toString());
        return policies.remove(policy);
    }


    @Override
    public boolean evaluate(
            String userId,
            String authContext,
            String resourceId,
            String queryString,
            String httpMethod) {

        log.debug("evaluate() - { " +
                "userId: \""+userId+"\", " +
                "authContext: \""+authContext+"\" "+
                "resourceId: \""+resourceId +"\", " +
                "queryString: \""+queryString+"\", " +
                "httpMethod:\""+httpMethod+"\"}");

        HashSet<String> userRoles = MembershipRulesManager.getUserRoles(userId,authContext);

        if(userRoles.isEmpty()){
            userRoles.add("");
        }
        for(String userInRole: userRoles){
            for(Policy policy: policies){
                log.debug("evaluate() - Evaluating Policy {}",policy.toString());
                if(policy.evaluate(userInRole,resourceId,queryString, httpMethod)) {
                    log.debug("evaluate() - END <**MATCH**>");
                    return true;
                }
            }
        }
        log.debug("evaluate() - END [NO MATCH])");
        return false;
    }

}
