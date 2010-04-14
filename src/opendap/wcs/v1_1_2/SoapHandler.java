package opendap.wcs.v1_1_2;

import opendap.coreServlet.ReqInfo;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import opendap.namespaces.SOAP;
import opendap.namespaces.NS;
import opendap.coreServlet.DispatchServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 9:00:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class SoapHandler extends XmlRequestHandler  {


    public SoapHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }
    public void init(DispatchServlet servlet, Element config) throws Exception {
        super.init(servlet,config);
    }


    public void handleWcsRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String serviceUrl = DispatchHandler.getServiceUrlString(request,_prefix);
        String dataAccessBase = ReqInfo.getBaseURI(request);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document wcsReq = sb.build(request.getInputStream());
        Element clientReq;
        Document soapResponseDoc;
        Element responseEnvelope;
        Element responseBody;
        Element soapEnvelope;
        Element soapBody;
        Document wcsResponse;
        Element wre;

        responseEnvelope = new Element("Envelope",SOAP.NS);
        responseBody = new Element("Body",SOAP.NS);

        soapEnvelope = wcsReq.getRootElement();

        if(NS.checkNamespace(soapEnvelope,"Envelope", SOAP.NS)){
            soapBody = soapEnvelope.getChild("Body", SOAP.NS);
            List soapContents = soapBody.getChildren();

            log.debug("Got " + soapContents.size() + " SOAP Body Elements.");
            for (Object soapContent : soapContents) {
                clientReq = (Element) soapContent;

                wcsResponse = getWcsResponse(serviceUrl, dataAccessBase, this, clientReq);
                wre = wcsResponse.getRootElement();
                wre.detach();
                responseBody.addContent(wre);
            }

            response.setContentType("text/xml");
            responseEnvelope.addContent(responseBody);
            soapResponseDoc = new Document(responseEnvelope);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            xmlo.output(soapResponseDoc,response.getOutputStream());
        }

    }


}
