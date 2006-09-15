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

package opendap.olfs;

import opendap.coreServlet.HttpDate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


/**
 * Handler to process the logic for HTTP/1.1 conditional GET requests.
 *
 * User: ndp
 * Date: Aug 22, 2006
 * Time: 1:55:29 PM
 */
public class ConditionalGetHandler {


    private static final int   IF_MODIFIED_SINCE = 1;
    private static final int IF_UNMODIFIED_SINCE = 2;



    private boolean _isValidConditionalGet;
    private boolean _conditionMet;
    private int     _typeOfGetCondition;
    private Date    _requestDateReference;
    private int     _returnStatus;

    private Date    _targetLastModifiedDate;

    // Conditional GET request related header values.
    private String _If_Modified_Since_header;
    private String _If_Unmodified_Since_header;



    // Turn these on later if we implement them!
    //private String _If_Match_header;
    //private String _If_None_Match_header;
    //private String _Range_header;


    /**
     *
     * @param request The clients request
     * @param targetLastModified The Last-Modified time of the clients requested entity.
     */
    public ConditionalGetHandler(HttpServletRequest request, Date targetLastModified) {

        this();

        _targetLastModifiedDate = targetLastModified;


        // Go get those headers associated with conditional GET activities
        _If_Modified_Since_header    = request.getHeader(  "If-Modified-Since");
        _If_Unmodified_Since_header  = request.getHeader("If-Unmodified-Since");

        // Turn these on later if we implement them!
        //_If_Match_header             = request.getHeader(           "If-Match");
        //_If_None_Match_header        = request.getHeader(      "If-None-Match");
        //_Range_header                = request.getHeader(              "Range");

        processRequest();


    }

    private ConditionalGetHandler(){

        _isValidConditionalGet = false;
        _conditionMet          = false;
        _typeOfGetCondition    = -1;
        _requestDateReference  = null;
        _returnStatus       = HttpServletResponse.SC_OK;


        _If_Modified_Since_header = null;
        _If_Unmodified_Since_header = null;


    }






    /**
     * Figure out if the headers make sense and get the Date and other data squared away.
     * @return True if the headers look good, false if they turn out to be invalid.
     */
    private boolean evaluateRequestHeaders(){



        if(_If_Modified_Since_header !=null && _If_Unmodified_Since_header !=null){
            _isValidConditionalGet = false;
        }
        else if(_If_Modified_Since_header !=null){

            _requestDateReference = HttpDate.getHttpDate(_If_Modified_Since_header);

            if(_requestDateReference != null){
                _typeOfGetCondition = IF_MODIFIED_SINCE;
                _isValidConditionalGet = true;
            }
            else
                _isValidConditionalGet = false;


        }
        else if(_If_Unmodified_Since_header !=null){

            _requestDateReference = HttpDate.getHttpDate(_If_Unmodified_Since_header);
            if(_requestDateReference != null){
                _typeOfGetCondition = IF_UNMODIFIED_SINCE;
                _isValidConditionalGet = true;
            }
            else
                _isValidConditionalGet = false;

        }
        else {
            _isValidConditionalGet = false;
        }


        return _isValidConditionalGet;


    }



    private void processRequest() {

            if(evaluateRequestHeaders()){

                switch(_typeOfGetCondition){

                    case   IF_MODIFIED_SINCE:
                        _conditionMet = _targetLastModifiedDate.after(_requestDateReference);
                        if(!conditionIsMet())
                            _returnStatus      = HttpServletResponse.SC_NOT_MODIFIED;
                        else
                            _returnStatus      = HttpServletResponse.SC_OK;
                        break;


                    case IF_UNMODIFIED_SINCE:
                        _conditionMet = _targetLastModifiedDate.before(_requestDateReference);
                        if(!conditionIsMet())
                            _returnStatus      = HttpServletResponse.SC_PRECONDITION_FAILED;
                        else
                            _returnStatus      = HttpServletResponse.SC_OK;
                        break;

                    default:
                        _conditionMet = false;
                        break;

                }

            }




    }


    /**
     *
     * @return True if the clients conditional GET request was valid (consistent with HTTP/1.1 secifications)
     */
    public boolean isValidConditionalGet(){

        return _isValidConditionalGet;

    }


    /**
     *
     * @return True if the conditions set by the client are met (or are invalid). False otherwise.
     */
    public boolean conditionIsMet(){

        return _conditionMet;

    }

    /**
     * Provides the correct HTTP/1.1 return status for the conditional GET request. This is really
     * most useful for requests whose conditions are not met, so that the server responds with a simple status
     * and nothing else. In the event that the condition is met OR the conditional environment was invalid, then the
     * return status will always be "OK" (200) to indicate that the requested document should be sent normally,
     * assuming that the document requested exists, etc.
     *
     * @return The correct HTTP/1.1 return status for request.
     */
    public int getReturnStatus(){
        return _returnStatus;
    }

    /**
     * Provides a rudimentary unit test of this class.
     * @param args Is ignored.
     */
    public static void main(String args[]){

        ConditionalGetHandler cgh = new ConditionalGetHandler();

        cgh._targetLastModifiedDate = HttpDate.getHttpDate("Mon, 21 Aug 2006 17:00:30 GMT");


        cgh._If_Modified_Since_header    = "Mon, 21 Aug 2006 17:00:45 GMT";
        cgh._If_Unmodified_Since_header  = "Mon, 21 Aug 2006 17:00:15 GMT";
        cgh.processRequest();
        System.out.println(cgh);

        cgh._If_Modified_Since_header    = "Mon, 21 Aug 2006 17:00:45 GMT";
        cgh._If_Unmodified_Since_header  = null;
        cgh.processRequest();
        System.out.println(cgh);

        cgh._If_Modified_Since_header    = "Mon, 21 Aug 2006 17:00:15 GMT";
        cgh._If_Unmodified_Since_header  = null;
        cgh.processRequest();
        System.out.println(cgh);

        cgh._If_Modified_Since_header    = "Mon, 21 Aug 2006 17:00:30 GMT";
        cgh._If_Unmodified_Since_header  = null;
        cgh.processRequest();
        System.out.println(cgh);


        cgh._If_Modified_Since_header    = null;
        cgh._If_Unmodified_Since_header  = "Mon, 21 Aug 2006 17:00:45 GMT";
        cgh.processRequest();
        System.out.println(cgh);

        cgh._If_Modified_Since_header    = null;
        cgh._If_Unmodified_Since_header  = "Mon, 21 Aug 2006 17:00:15 GMT";
        cgh.processRequest();
        System.out.println(cgh);

        cgh._If_Modified_Since_header    = null;
        cgh._If_Unmodified_Since_header  = "Mon, 21 Aug 2006 17:00:30 GMT";
        cgh.processRequest();
        System.out.println(cgh);



    }


    public String toString(){
        String s = "";

        s += "\n";
        switch(_typeOfGetCondition){

            case   IF_MODIFIED_SINCE:
                s += "If-Modified-Since:" + "\n";
                break;

            case IF_UNMODIFIED_SINCE:
                s+= "If-Unmodified-Since:" + "\n";
                break;

            default:
                s += "INVALID CONDITIONAL GET:" + "\n";
                break;

        }
        s += "   Target Last-Modified Date:  " + HttpDate.getHttpDateString(_targetLastModifiedDate) + "\n";
        s += "   If-Modified-Since header:   " + _If_Modified_Since_header   + "\n";
        s += "   If-Unmodified-Since header: " + _If_Unmodified_Since_header + "\n";
        s += "   Client Date Reference:      " + HttpDate.getHttpDateString(_requestDateReference)   + "\n";
        //s += "\n";
        s += "   isValidConditionalGet():    " + isValidConditionalGet()     + "\n";
        s += "   conditionIsMet():           " + conditionIsMet()            + "\n";
        s += "   getReturnStatus:            " + getReturnStatus()           + "\n";

        return s;

    }



}
