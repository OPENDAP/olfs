/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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

package opendap.experiments;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 */
public class TestBadXML {

    public static void main(String args[]) {

        String testDoc =
                "<showCatalog>\n"+
                "    <response>\n"+
                "        <BESException>\n"+
                "            <Message>Test Exception</Message>\n"+
                "        </BESException>\n"+
                "    </response>\n"+
                "</showCatalog>\n";

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc;
        try {
            doc = sb.build(new ByteArrayInputStream(testDoc.getBytes()));
            ElementFilter exceptionFilter = new ElementFilter("BESException");
            Iterator i = doc.getDescendants(exceptionFilter);
            if(i.hasNext()){
                Element exception = (Element) i.next();
                System.out.print("Found BESException!\nMessage: ");
                System.out.println(exception.getChild("Message").getTextTrim());
            }

        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
