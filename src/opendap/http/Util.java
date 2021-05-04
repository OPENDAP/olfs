/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class Util {

    public static final String HTTP_PROTOCOL = "http://";
    public static final String HTTPS_PROTOCOL = "https://";
    public static final String BES_PROTOCOL = "bes://";

    static private Logger _log;
    static {
        _log = LoggerFactory.getLogger(Util.class);
    }


    static public CredentialsProvider getNetRCCredentialsProvider() throws IOException {
        String default_file = ".netrc";
        String home = System.getProperty("user.home");

        if (home != null)
            default_file = home + "/" + default_file;

        return getNetRCCredentialsProvider(default_file, true);

    }

    static public CredentialsProvider getNetRCCredentialsProvider(String filename, boolean secure_transport) throws IOException {

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        NetRC netRC = new NetRC(filename);

        for (NetRC.NetRCEntry entry : netRC.getEntries()) {
            String userId = entry.getLogin();
            String pword = entry.getPassword();

            credsProvider.setCredentials(
                    new AuthScope(entry.getMachine(), secure_transport ? 443 : 80),
                    new UsernamePasswordCredentials(userId, pword));

        }
        return credsProvider;
    }


    /**
     * Retrieves the resource located at "url" and writes its content into "os". Authentication is
     * handled using the passed CredentialsProvider.
     * @param url Resource to retrieve.
     * @param _credsProvider Authenitcation credentials used to access "url"
     * @param os Writes the stuff to this sink.
     * @throws IOException
     */
    static public void writeRemoteContent(String url, CredentialsProvider _credsProvider, OutputStream os) throws IOException {
        _log.debug("writeRemoteContent() - URL: {}", url);

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(_credsProvider)
                .build();

        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse resp = httpclient.execute(httpGet);
        try {
            _log.debug("writeRemoteContent() - HTTP STATUS: {}", resp.getStatusLine());
            HttpEntity entity1 = resp.getEntity();
            entity1.writeTo(os);
            EntityUtils.consume(entity1);
        } finally {
            resp.close();
        }
    }


    /**
     * Copies the content of the remote resource into the passed HttpServletResponse instance. If the passed paramter
     * transferHttpHeaders is true then the response headers are copied to the OutputStream before the resource
     * content is transmitted.
     * @param url The remote resource to retrieve
     * @param credentialsProvider Credentials to be used if authentication is required
     * @param response The HttpServletResponse object which contains the destination OutputStream and the API for setting
     *                 response headers in the Tomcat/Servlet API ecosystem.
     * @param transferHttpHeaders If set to true the remote resource's HTTP response heders will be transfered. If false
     *                            then the remote resource response hedaers will be ignored.
     * @throws IOException When bad things happen.
\     */
    public static void forwardUrlContent(
            String url,
            CredentialsProvider credentialsProvider,
            HttpServletResponse response,
            boolean transferHttpHeaders)
            throws IOException {

        _log.debug("Retrieving URL: "+url);

        // GetMethod contentRequest = new GetMethod(url);
        HttpGet contentRequest = new HttpGet(url);
        //InputStream is = null;
        try {

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();

            // Execute the method.
            CloseableHttpResponse resp = httpClient.execute(contentRequest);
            // Did it work?
            StatusLine status = resp.getStatusLine();
            if (status.getStatusCode() != HttpStatus.SC_OK) {
                String msg = "Unable to read data from primary data source. Resource URL: '"+url+
                        "' The server returned the HTTP status: '"+status.getStatusCode()+"' protocol: "+status.getProtocolVersion()+"  reason: "+status.getReasonPhrase();

                _log.error(msg);
                throw new IOException(msg);
            }
            else {
                if(transferHttpHeaders){
                    Header[] headers = resp.getAllHeaders();
                    String name, value;

                    for(Header h:headers){
                        name = h.getName();
                        value = h.getValue();
                        // DO NOT Transfer the Transfer-Encoding header cause if you do you'll bone what ever Tomcat
                        // is doing.
                        if(!name.equalsIgnoreCase("Transfer-Encoding"))
                            response.setHeader(name,value);
                    }
                }
                ServletOutputStream os = response.getOutputStream();
                HttpEntity entity1 = resp.getEntity();
                entity1.writeTo(os);
                EntityUtils.consume(entity1);
            }
        }
        finally {
            _log.debug("Releasing Http connection.");
            contentRequest.releaseConnection();
        }
    }

    /*


    private static String uatURS = "https://uat.urs.earthdata.nasa.gov";


    public static String getHttpResourceReturnAsString(String url, List<Header> headers,CredentialsProvider credsProvider) throws IOException {

        _log.debug("URL: {}", url);

        HttpHost targetHost = new HttpHost(uatURS, 443, "https");

        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();

        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);

        CloseableHttpClient httpclient = HttpClients.createDefault();


        HttpGet httpGet = new HttpGet(url);

        if(headers !=null) {
            Header[] hdrs = new Header[headers.size()];
            headers.toArray(hdrs);
            httpGet.setHeaders(hdrs);
        }

        CloseableHttpResponse resp = null;
        BufferedReader in = null;
        try {
            StringBuilder sb = new StringBuilder();
            resp = httpclient.execute(httpGet);

            sb.append("HTTP STATUS: ").append(resp.getStatusLine()).append("\n");
            _log.debug("HTTP STATUS: {}", resp.getStatusLine());
            HttpEntity entity1 = resp.getEntity();

            for(Header header : resp.getAllHeaders()){
                sb.append("  ").append(header.getName()).append(": ").append(header.getValue()).append("\n");
            }


            in = new BufferedReader(
                    new InputStreamReader(entity1.getContent(),HyraxStringEncoding.getCharset()));
            String line;
            while( (line = in.readLine()) != null ) sb.append(line);

            return sb.toString();

        } finally {

            if(in!=null)
                in.close();

            if(resp!=null)
                resp.close();


        }
    }


    public static String getHttpResourceReturnAsString(String url, List<Header> headers) throws IOException {
        return getHttpResourceReturnAsString(url, headers, getNetRCCredentialsProvider());
    }



    private static String UAT_URS = "uat.urs.earthdata.nasa.gov";

    public static void main(String[] args) throws Exception {
        HttpHost target = new HttpHost(UAT_URS, 443, "https");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials("hyrax_sbx", "98764uygEWtqw"));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
        try {

            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local
            // auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);

            // Add AuthCache to the execution context
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            AuthScheme as = authCache.get(target);

            System.out.println("AuthRealm: " + as.getRealm() + "  AuthSchemeName " + as.getSchemeName());

            HttpGet httpget = new HttpGet(uatURS);

            System.out.println("Executing request " + httpget.getRequestLine() + " to target " + target);
            for (int i = 0; i < 3; i++) {
                CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
                try {
                    System.out.println("----------------------------------------");
                    System.out.println(response.getStatusLine());
                    System.out.println("Response body is " + EntityUtils.toString(response.getEntity()).length()+" chars");
                } finally {
                    response.close();
                }
            }
        } finally {
            httpclient.close();
        }
    }

    */

}






