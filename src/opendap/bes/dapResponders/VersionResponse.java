package opendap.bes.dapResponders;

import opendap.bes.BesDapResponder;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/24/12
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersionResponse extends BesDapResponder {

    private Logger log;

    private static String _matchAnythingRegex = ".*";
    private static String _preferredRequestSuffix = ".ver";
    private static String _defaultRequestSuffixRegex = "\\"+_preferredRequestSuffix;
    private static String _defaultRequestRegex = "/version/?|"+ _matchAnythingRegex + _defaultRequestSuffixRegex;


    public VersionResponse(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffixRegex,besApi);
    }

    public VersionResponse(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffixRegex,besApi);
    }

    public VersionResponse(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);

        setRequestMatchRegex(_defaultRequestRegex);

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/version#");
        setServiceTitle("Server Version");
        setServiceDescription("An XML document containing information about the software version of the server..");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Server_Version_Service");
        setPreferredServiceSuffix(_preferredRequestSuffix);

    }


    /**
     *
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl){
       Pattern p = getRequestMatchPattern();
       Matcher m = p.matcher(relativeUrl);
       return m.matches();

    }




    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        BesApi besApi = getBesApi();

        log.debug("respondToHttpGetRequest() - Sending Version response...");

        response.setContentType("text/xml");
        response.setHeader("Content-Description", "dods_version");

        response.setStatus(HttpServletResponse.SC_OK);

        PrintStream ps = new PrintStream(response.getOutputStream());

        Document vdoc = besApi.getCombinedVersionDocument();

        if (vdoc == null) {
            throw new ServletException("Internal Error: Version Document not initialized.");
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        //XMLOutputter xout = new XMLOutputter();
        xout.output(vdoc, ps);
        ps.flush();


        log.debug("respondToHttpGetRequest() - Sent Version response.");


    }

}
