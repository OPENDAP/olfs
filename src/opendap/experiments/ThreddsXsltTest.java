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

import net.sf.saxon.s9api.SaxonApiException;
import opendap.xml.Transformer;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 14, 2010
 * Time: 4:31:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreddsXsltTest {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(ThreddsXsltTest.class);


    public static void main (String[] args){


        try {

        Transformer thredds2DatasetUrl = new Transformer("xsl/thredds2datasetAccess.xsl");


            for(String arg: args){
                String targetCatalog = arg;
                log.debug("Target THREDDS catalog: "+targetCatalog);

                String catalogServer = getServerUrlString(targetCatalog);

                log.debug("Catalog Server: "+catalogServer);

                // Pass the catalogServer parameter to the transform
                thredds2DatasetUrl.setParameter("catalogServer", catalogServer);

                thredds2DatasetUrl.transform(new StreamSource(targetCatalog),System.out);



            }

        } catch (SaxonApiException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


    }

    private static String  getServerUrlString(URL url) {

        String baseURL = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("file")) {
            log.debug("Protocol is FILE.");

        } else if (protocol.equalsIgnoreCase("http")) {
            log.debug("Protcol is HTTP.");

            String host = url.getHost();
            /* String path = url.getPath(); */
            int port = url.getPort();

            baseURL = protocol + "://" + host;

            if (port != -1)
                baseURL += ":" + port;
        }

        log.debug("ServerURL: " + baseURL);

        return baseURL;

    }

    private static String getServerUrlString(String url) throws MalformedURLException {

        URL u = new URL(url);

        return getServerUrlString(u);

    }





}
