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

package opendap.experiments;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/9/11
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class XslPipeline {


    public static void main(String[] args){


        try {
            StreamSource sourceDocument = new StreamSource("path/to/source/document");

            Serializer stdout = new Serializer();
            stdout.setOutputProperty(Serializer.Property.METHOD, "xml");
            stdout.setOutputProperty(Serializer.Property.INDENT, "yes");
            stdout.setOutputStream(System.out);


            StreamSource xlst_1 = new StreamSource("path/to/transform/filename1");
            Processor proc = new Processor(false);
            XsltCompiler comp = proc.newXsltCompiler();
            XsltExecutable exp = comp.compile(xlst_1);
            XsltTransformer transform_1 = exp.load();

            StreamSource xlst_2 = new StreamSource("path/to/transform/filename2");
            comp = proc.newXsltCompiler();
            exp = comp.compile(xlst_2);
            XsltTransformer transform_2 = exp.load();


            transform_1.setSource(sourceDocument);
            transform_1.setDestination(transform_2);
            transform_2.setDestination(stdout);

            transform_1.transform();


        } catch (SaxonApiException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }




    }
}
