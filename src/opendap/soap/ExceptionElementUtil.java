/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.soap;

import org.jdom.Element;
import org.jdom.Namespace;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 *
 * Provides XML representaions of Java exceptions.
 *
 *
 * User: ndp
 * Date: May 2, 2006
 * Time: 11:07:16 AM
 */
public class ExceptionElementUtil  {


    /**
     * Builds a JDOM ELement called OPeNDAPException that holds information about an exception
     * thrown, well, somewhere...
     * @param type The type of exception
     * @param msg The message associated with the exception.
     * @param location The location (in the code) of the exeption.
     * @return A JDOM Element containing the all the Exception information.
     */
    public static Element makeExceptionElement(String type, String msg, String location){

        Namespace ns = XMLNamespaces.getOpendapSoapNamespace();

        Element exception = new Element("OPeNDAPException",ns);


        exception.addContent( new Element("Type",ns).setText(type));
        exception.addContent( new Element("Message",ns).setText(msg));
        exception.addContent( new Element("Location",ns).setText(location));

        return exception;


    }


    /**
     * Builds a JDOM ELement called OPeNDAPException that holds information about an exception
     * thrown, well, somewhere...
     * @param t The bad thing that happened.
     * @return A JDOM Element containing the all the Exception information.
     */
    public static Element makeExceptionElement(Throwable t){

        Namespace ns = XMLNamespaces.getOpendapSoapNamespace();

        Element exception = new Element("OPeNDAPException",ns);


        exception.addContent( new Element("Type",ns).setText(t.getClass().getName()));
        exception.addContent( new Element("Message",ns).setText(t.getMessage()));
        exception.addContent( new Element("Location",ns).setText(t.getStackTrace()[0].getFileName() +
                " - line " + t.getStackTrace()[0].getLineNumber()));

        return exception;


    }






}
