/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
 * // Author: Uday Kari  <ukari@opendap.org>
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

package opendap.dap4;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
/**
 * Concocted per netdawg in 
 * https://stackoverflow.com/questions/277502/jaxb-how-to-ignore-namespace-during-unmarshalling-xml-document
 * @author ukari
 *
 */
public class XMLReaderWithNamespaceInMyPackageDotInfo extends StreamReaderDelegate {
    public XMLReaderWithNamespaceInMyPackageDotInfo(XMLStreamReader reader) {
        super(reader);
      }
    
      @Override
      public String getAttributeNamespace(int arg0) {
        return "";
      }
      @Override
      public String getNamespaceURI() {
        //return "";
    	//return "http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#";
    	return "http://xml.opendap.org/ns/DAP/4.0#";
      }
  }

