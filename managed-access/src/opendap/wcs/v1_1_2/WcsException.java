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
package opendap.wcs.v1_1_2;

import org.jdom.Element;

/**
 * WCS exceptions 
 * <table>
 * <tr>
 * <td align="center"><b>ExceptionCode</b></td>
 * <td align="center"><b>Meaning of code</b></td>
 * <td align="center"><b>locator value</b></td>
 * </tr>
 * <tr>
 * <td> OperationNotSupported</td>
 * <td> Request is for an operation that is not supported by this server</td>
 * <td> Name of operation not supported.</td>
 * </tr>
 * <tr>
 * <td>MissingParameterValue</td>
 * <td>Operation request does not include a parameter value, and this server
 *     did not declare a default value for that parameter </td>
 * <td> Name of missing parameter </td>
 * </tr>
 * <tr>
 * <td>InvalidParameterValue</td>
 * <td>Operation request contains an invalid parameter value.</td>
 * <td>Name of parameter with invalid value</td>
 * </tr>
 * <tr>
 * <td>VersionNegotiationFailed</td>
 * <td>List of versions in &quot;AcceptVersions&quot; parameter value in GetCapabilities
 *     operation request did not include any version supported by this server.</td>
 * <td>None, omit &quot;locator&quot; parameter</td>
 * </tr>
 * <tr>
 * <td>InvalidUpdateSequence</td>
 * <td>Value of (optional) updateSequence parameter in GetCapabilities operation
 *     request is greater than current value of service metadata updateSequence
 *     number </td>
 * <td>None, omit &quot;locator&quot; parameter</td>
 * </tr>
 * <tr>
 * <td>OptionNotSupported</td>
 * <td>Request is for an option that is not supported by this server.</td>
 * <td>Identifier of option not supported</td>
 * </tr>
 * <tr>
 * <td>NoApplicableCode</td>
 * <td>No other exceptionCode specified by this service and server applies to
 *     this exception.</td>
 * <td>None, omit locator parameter </td>
 * </tr>
 * </table>
 *
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:18:20 PM
 */
public class WcsException extends Exception {



    /**
     * OperationNotSupported: Request is for an operation that is not supported
     * by this server. (locator = Name of operation not supported)
     */
    public static final int OPERATION_NOT_SUPPORTED     = 0;


    /**
     * MissingParameterValue: Operation request does not include a parameter
     * value, and this server did not declare a default value for that
     * parameter. (locator = Name of missing parameter)
     */
    public static final int MISSING_PARAMETER_VALUE     = 1;


    /**
     * InvalidParameterValue: Operation request contains an invalid parameter
     * value. (locator =  Name of parameter with invalid value)
     */
    public static final int INVALID_PARAMETER_VALUE     = 2;


    /**
     * VersionNegotiationFailed: List of versions in AcceptVersions parameter
     * value in GetCapabilities operation request did not include any version
     * supported by this server. (locator = None, omit locator parameter)
     */
    public static final int VERSION_NEGOTIATION_FAILED  = 3;


    /**
     * InvalidUpdateSequence: Value of (optional) updateSequence parameter in
     * GetCapabilities operation request is greater than current value of
     * service metadata updateSequence number. (locator = None, omit locator
     * parameter)
     */
    public static final int INVALID_UPDATE_SEQUENCE     = 4;


    /**
     * NoApplicableCode: No other exceptionCode specified by this service
     * and server applies to this exception. (locator = None, omit locator
     * parameter)
     */
    public static final int NO_APPLICABLE_CODE          = 5;

    private static final String[] _exceptionCodeName =
            { "OperationNotSupported",
              "MissingParameterValue",
              "InvalidParameterValue",
              "VersionNegotiationFailed",
              "InvalidUpdateSequence",
              "NoApplicableCode" };


    private final int _exceptionCode;
    private final String _locator;

    public WcsException(String s, int exceptionCode, String locator) {
        super(s);
        _exceptionCode = exceptionCode;
        _locator = locator;
    }

    public WcsException(String s, int exceptionCode) {
        super(s);
        _exceptionCode = exceptionCode;
        _locator = null;
    }


    public Element getExceptionElement(){

        Element exp = new Element("Exception", WCS.OWS_NS);

        exp.setAttribute("exceptionCode",_exceptionCodeName[_exceptionCode]);
        if(_locator != null)
            exp.setAttribute("locator",_locator);

        String msg = getMessage();
        if(msg != null){
            Element expText = new Element("ExceptionText", WCS.OWS_NS);
            expText.setText(msg);
            exp.addContent(expText);
        }

        return exp;
    }


    public String getMessage(){
        String msg = super.getMessage();

        if(_locator!=null)
            msg = _locator + ": "+msg;

        msg += " (code: "+ _exceptionCodeName[_exceptionCode]+")";

        return msg;

    }


}
