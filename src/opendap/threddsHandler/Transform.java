/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
package opendap.threddsHandler;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;
import java.io.OutputStream;
import java.io.File;
import java.util.Date;

/**
 * User: ndp
 * Date: Dec 29, 2008
 * Time: 8:38:21 AM
 */
public class Transform {

    private Processor proc;
    private Serializer out;
    private XsltTransformer transform;
    private Date cacheTime;
    private String xsltDoc;


    public Transform(String xsltDocument) throws SaxonApiException {


        init(xsltDocument);

    }

    private void init(String xsltDocument) throws SaxonApiException {

        xsltDoc = xsltDocument;

        // Get an XSLT processor and serializer
        proc = new Processor(false);
        out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");

        loadTransform();



    }

    private void reloadTransformIfRequired() throws SaxonApiException {
        File f = new File(xsltDoc);
        if(f.lastModified()>cacheTime.getTime()){
            loadTransform();
        }

    }

    private void loadTransform() throws SaxonApiException{
        // Get an XSLT compiler with our transform in it.
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(xsltDoc));
        transform = exp.load(); // loads the transform file.
        cacheTime = new Date();

    }

    public Processor getProcessor(){
        return proc;
    }

    public void transform(XdmNode doc, OutputStream os) throws SaxonApiException {
        reloadTransformIfRequired();
        out.setOutputStream(os);
        transform.setInitialContextNode(doc);
        transform.setDestination(out);
        transform.transform();
    }







}
