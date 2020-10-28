/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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

package opendap.auth;

import opendap.io.HyraxStringEncoding;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by ndp on 9/24/14.
 */
public class Util {


    /**
     * Utility method used to submit an HTTP request.
     *
     * This method will submit a GET request unless 'data' is non-null,
     * in which case it will be considered a POST request.
     *
     */
    public static String submitHttpRequest( String url, Map<String, String> headers, String data )
        throws IOException
    {
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = null;

        try
        {
            // Create a connection and build the request
            connection = (HttpURLConnection) (new URL(url)).openConnection();

            connection.setUseCaches(false);
            connection.setDoInput(true);

            for(Map.Entry<String, String> header: headers.entrySet()){
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // If data is provided, then convert it to a POST request.
            if( data != null )
            {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(data);
                out.flush();
                out.close();
            }


            int http_status = connection.getResponseCode();

            // Extract the body of the response so we can return it.
            // We want this even if it's an error.
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(),HyraxStringEncoding.getCharset()));

            String line;
            while( (line = in.readLine()) != null ) result.append(line);
            in.close();

            // Check the response to the request. We consider anything other than
            // 200 (OK) as an error, though it may be useful to be able to return
            // this value to the caller and let the caller decide.
            if( http_status != 200 )
            {
                StringBuilder msg = new StringBuilder();
                msg.append("HTTP request failed. status: ").append(http_status);
                msg.append(" url: ").append(url);
                msg.append(" message: ").append(result);
                throw new IOException(msg.toString());
            }



        }
        finally
        {
            if( connection != null ) connection.disconnect();
        }

        return result.toString();
    }

}
