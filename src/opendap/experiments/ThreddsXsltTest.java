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
