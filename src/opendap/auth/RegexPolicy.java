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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by ndp on 9/26/14.
 */
public class RegexPolicy implements Policy {

    private Logger _log;

    public static final String MATCH_ALL = ".*$";

    private Pattern _rolePattern;
    private Pattern _resourcePattern;
    private Pattern _queryStringPattern;

    private HashSet<HTTP_METHOD> _allowedActions;

    public RegexPolicy(){
        _log = LoggerFactory.getLogger(this.getClass().getName());
        _rolePattern = null;
        _resourcePattern = null;
        _queryStringPattern = null;
        _allowedActions = new HashSet<HTTP_METHOD>();


    }




    @Override
    public void init(Element config) throws ConfigurationException {

        Element e = config.getChild("role");
        if(e==null)
            throw new ConfigurationException("You must supply a regex string to match the subject Id.");
        _rolePattern = Pattern.compile(e.getTextTrim());


        e = config.getChild("resource");
        if(e==null)
            throw new ConfigurationException("You must supply a regex string to match the resource Id.");
        _resourcePattern = Pattern.compile(e.getTextTrim());



        e = config.getChild("query");
        if(e!=null){
            _queryStringPattern = Pattern.compile(e.getTextTrim());
        }
        else {
            /**
             * By default, if no query regex is offered, we match any query.
             */
            _queryStringPattern = Pattern.compile(MATCH_ALL);

        }




        List allowedActions = config.getChildren("allowedAction");
        if(allowedActions.isEmpty())
            throw new ConfigurationException("You must define at least one allowable action for the policy.");

        for(Object o:  allowedActions){
            Element allowedAction = (Element)o;
            _allowedActions.add(HTTP_METHOD.valueOf(allowedAction.getTextTrim()));
        }


    }


    @Override
    public boolean evaluate(String roleId, String resourceId, String queryString, String httpMethod) {

        if(roleId ==null || resourceId==null || queryString == null || httpMethod == null) {

            _log.error("evaluate() - Passing null values is not allowed. RETURNING FALSE");
            return false;
        }

        if(_rolePattern.matcher(roleId).matches()){
            if(_resourcePattern.matcher(resourceId).matches()){
                if(_queryStringPattern.matcher(queryString).matches()) {
                    if (_allowedActions.contains(HTTP_METHOD.valueOf(httpMethod))) {
                        return true;
                    }
                }
            }
        }

        return false;

    }
}
