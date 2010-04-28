package opendap.wcs.v1_1_2;

import opendap.coreServlet.ReqInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.net.URLDecoder;

import opendap.coreServlet.DispatchServlet;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 9:00:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class FormHandler extends XmlRequestHandler  {


    public FormHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public void init(HttpServlet servlet, Element config) throws Exception {
        super.init(servlet,config);
    }

    public void handleWcsRequest(HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {

        String dataAccessBase = ReqInfo.getBaseURI(request);
        String serviceUrl = DispatchHandler.getServiceUrlString(request,_prefix);
        BufferedReader  sis = request.getReader();
        ServletOutputStream os = response.getOutputStream();
        int length;


        String encoding = request.getCharacterEncoding();
        if(encoding==null)
            encoding = "UTF-8";


        String formTag = "WCS_QUERY=";

        String sb = "";
        String reqDoc = "";

        //  Slurp up the document.
        while(sb!= null){
            if(sb != null){
                length =  sb.length() + reqDoc.length();
                if( length > WCS.MAX_REQUEST_LENGTH)
                    throw new IOException("Form Content Body too long. Try again with something smaller.");
                reqDoc += sb;
            }
            sb = sis.readLine();
        }

        log.debug("Form Interface received: " +reqDoc);

        // If you got a document,
        if(reqDoc!=null){

            // Decode it, because browsers seem to want to URL encode their post content. Whatever...
            reqDoc = URLDecoder.decode(reqDoc,encoding);

            // Strip off the form  name
            if(reqDoc.startsWith(formTag)) {
                reqDoc = reqDoc.substring(formTag.length());
            }


            ByteArrayInputStream baos = new ByteArrayInputStream(reqDoc.getBytes());

            response.setContentType("text/xml");

            Document wcsResponse = getWcsResponse(serviceUrl,dataAccessBase,this,baos);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            xmlo.output(wcsResponse,os);


        }

    }


}