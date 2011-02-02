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
package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.util.Vector;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 5:34:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Keywords {

    private Vector<LanguageString> keywords;
    private Vector<CodeStringAttribute>     types;

    public Keywords(){
        keywords = null;
        types= null;

    }

    public Keywords(LanguageString[] keywords, CodeStringAttribute[] types){
        this.keywords = new Vector<LanguageString>();
        for(LanguageString keyword: keywords)
            this.keywords.add(keyword);

        this.types = new Vector<CodeStringAttribute>();
        for(CodeStringAttribute type: types)
            this.types.add(type);

    }


    public Keywords(Vector<LanguageString> keywords, Vector<CodeStringAttribute> types){
        this.keywords = keywords;
        this.types = types;
    }


    public void addKeyword(LanguageString keyword){
        keywords.add(keyword);
    }


    public LanguageString[] getKeywordsArray(){

        LanguageString[] array = new  LanguageString[keywords.size()];

        return keywords.toArray(array);

    }

    public Iterator<LanguageString> getKeywordsIterator(){
        return keywords.iterator();
    }


    public void addType(CodeStringAttribute type){
        types.add(type);
    }

    public CodeStringAttribute[] getTypesArray(){

        CodeStringAttribute[] array = new CodeStringAttribute[types.size()];

        return types.toArray(array);

    }

    public Iterator<CodeStringAttribute> getTypesIterator(){
        return types.iterator();
    }



    public Element getElement(){
        Element e;
        Element keywordsElement = new Element("Keyword",WCS.OWCS_NS);

        for(LanguageString keyword: keywords){
            e = new Element("Keyword",WCS.OWS_NS);
            e.setText(keyword.getValue());
            e.setAttribute("xml:lang",keyword.getLang());
            keywordsElement.addContent(e);
        }

        for(CodeStringAttribute type: types){
            e = new Element("Type",WCS.OWS_NS);
            e.setText(type.getValue());
            e.setAttribute("codeSpace",type.getValue());
            keywordsElement.addContent(e);
        }
        return keywordsElement;

    }



}
