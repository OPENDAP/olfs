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
package opendap.experiments;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Mar 11, 2010
 * Time: 12:00:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomIndexTest {




    public static void main(String args[]){

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        SAXBuilder sb = new SAXBuilder();
        Document doc;
        Iterator i;
        Element e, docRoot;

        try {
            for(String s:args){
                doc = sb.build(s);
                docRoot =doc.getRootElement();
                i = docRoot.getDescendants(new ContentFilter(ContentFilter.ELEMENT));
                while(i.hasNext()){
                    e = (Element) i.next();
                    System.out.println("Element: "+e.getName()+"   index(Parent is "+e.getParentElement().getName() +"): "+e.getParentElement().indexOf(e));
                }




            }
        } catch (JDOMException er) {
            er.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException er) {
            er.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }



}
