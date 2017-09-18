package opendap.http;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;


public class Util {

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
            String userId = entry.login;
            String pword = String.valueOf(entry.password);

            credsProvider.setCredentials(
                    new AuthScope(entry.machine, secure_transport ? 443 : 80),
                    new UsernamePasswordCredentials(userId, pword));

        }
        return credsProvider;
    }


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


}






