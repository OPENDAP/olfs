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

package opendap.bes.dap4Responders.DatasetMetadata;

import opendap.PathBuilder;
import opendap.auth.AuthenticationControls;
import opendap.bes.Version;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.Request;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.TextHtml;
import opendap.namespaces.DAP;
import opendap.xml.Transformer;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

public class HtmlDMR extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".html";

    public HtmlDMR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public HtmlDMR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public HtmlDMR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("HTML representation of the DMR.");
        setServiceDescription("HTML representation of the Dataset Metadata Response document.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#Dataset_Metadata_Response");

        setNormativeMediaType(new TextHtml(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }

    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }

    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // String context = request.getContextPath();
        String collectionUrl = ReqInfo.getCollectionUrl(request);
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);
        QueryParameters qp = new QueryParameters(request);
        Request oreq = new Request(null,request);


        BesApi besApi = getBesApi();

        String supportEmail = besApi.getSupportEmail(requestedResourceId);
        String mailtoHrefAttributeValue = OPeNDAPException.getSupportMailtoLink(request,200,"n/a",supportEmail);

        log.debug("sendNormativeRepresentation() - Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");
        // XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document dmr = new Document();
        besApi.getDMRDocument(
                resourceID,
                qp,
                xmlBase,
                dmr);
        OutputStream os = response.getOutputStream();

        dmr.getRootElement().setAttribute("dataset_id",resourceID);
        // dmr.getRootElement().setAttribute("base", xmlBase, Namespace.XML_NAMESPACE);   // not needed - DMR has it

        String currentDir = System.getProperty("user.dir");
        log.debug("Cached working directory: "+currentDir);

        String xslDir = new PathBuilder(_systemPath).pathAppend("xsl").toString();

        log.debug("Changing working directory to "+ xslDir);
        System.setProperty("user.dir",xslDir);

        try {
            String xsltDocName = "dap4_ifh.xsl";

            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);
            // transformer.setParameter("serviceContext", request.getServletContext().getContextPath()); // This is ServletAPI-3.0
            transformer.setParameter("serviceContext", request.getContextPath()); // This is ServletAPI-2.5 (Tomcat 6 stopped here)
            transformer.setParameter("docsService", oreq.getDocsServiceLocalID());
            transformer.setParameter("HyraxVersion", Version.getHyraxVersionString());
            transformer.setParameter("JsonLD", getDatasetJsonLD(collectionUrl,dmr));
            transformer.setParameter("supportLink", mailtoHrefAttributeValue);

            AuthenticationControls.setLoginParameters(transformer,request);

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform(new JDOMSource(dmr), os);
            os.flush();
            log.info("Sent {}", getServiceTitle());
        }
        finally {
            log.debug("Restoring working directory to " + currentDir);
            System.setProperty("user.dir", currentDir);
        }
    }


    public String getDatasetJsonLD(String collectionUrl, Document dmr){

        String indent = indent_inc;
        Element dataset = dmr.getRootElement();

        StringBuilder sb = new StringBuilder("\n");
        sb.append("{\n");
        sb.append(indent).append("\"@context\": {\n");
        sb.append(indent).append(indent_inc).append("\"@vocab\": \"http://schema.org\"\n");
        sb.append(indent).append("},\n");
        sb.append(indent).append("\"@type\": \"Dataset\",\n");

        String name  = dataset.getAttributeValue("name");
        sb.append(indent).append("\"name\": \"").append(name).append("\",\n");


        Attribute xmlBase  = dataset.getAttribute("base", Namespace.XML_NAMESPACE);
        String datasetUrl;
        if(xmlBase==null){
            log.error("Unable to locate xml:base attribute for Dataset {}", name);
            datasetUrl = PathBuilder.pathConcat(collectionUrl,name);
        }
        else {
            datasetUrl = xmlBase.getValue();
        }
        sb.append(indent).append("\"url\": \"").append(datasetUrl).append("\",\n");
        sb.append(indent).append("\"includedInDataCatalog\": { \n");
        sb.append(indent).append(indent_inc).append("\"@type\": \"DataCatalog\", \n");
        sb.append(indent).append(indent_inc).append("\"name\": \"Hyrax Data Server\", \n");
        sb.append(indent).append(indent_inc).append("\"sameAs\": \"");
        sb.append(PathBuilder.pathConcat(collectionUrl,"contents.html\"")).append("\n");
        sb.append(indent).append("},\n");


        @SuppressWarnings("unchecked")
        List<Element> children = dataset.getChildren();
        Vector<Element> variables = new Vector<>();
        for(Element child : children){
            if(!child.getName().equals("Attribute") && !child.getName().equals("blob")){
                // It's not an Attribute so it must be a variable!
                variables.add(child);
            }
        }
        if(!variables.isEmpty()){
            sb.append(indent).append("\"variableMeasured\": [\n");
            // Top Level Attributes
            String topLevelAttributes = dap4AttributesToProperties(dataset, datasetUrl, indent, true);
            sb.append(topLevelAttributes);

            boolean first = topLevelAttributes.isEmpty();
            int mark = sb.length();
            for(Element variable : variables){
                if(!first)
                    sb.append(",\n");
                sb.append(dap4AttributesToProperties(variable,variable.getAttributeValue("name"), indent, first));
                if(sb.length()>mark)
                    first = false;
            }
            sb.append("\n");
            sb.append(indent).append("]\n");
            sb.append("}\n");
        }
        return sb.toString();
    }



    private Filter dap4AttributeFilter = new ElementFilter("Attribute", DAP.DAPv40_NS);
    private String indent_inc = "  ";

    public String dap4AttributesToProperties(Element variable, String name, String indent, boolean first){

        String myIndent = indent + indent_inc;
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("{\n");
        sb.append(myIndent).append("\"@type\": \"PropertyValue\",\n");
        sb.append(myIndent).append("\"name\": \"").append(name).append("\",\n");
        sb.append(myIndent).append("\"valueReference\": [ \n");

        if(variable.getName().equals("Sequence") || variable.getName().equals("Structure") ){
            // It's a Container type... Wut do?
            @SuppressWarnings("unchecked")
            List<Element> attributes = variable.getContent(dap4AttributeFilter);
            sb.append(dap4AttributesToProperties(attributes, myIndent));

            //boolean first = true;
            @SuppressWarnings("unchecked")
            List<Element> children = variable.getChildren();
            int mark = sb.length();
            for(Element child : children){
                if(!first)
                    sb.append(",\n");
                sb.append(dap4AttributesToProperties(child, child.getAttributeValue("name"), myIndent, first));
                if(sb.length()>mark)
                    first = false;
            }

            log.error("dap4AttributesToProperties() - We don't have a good mapping for container types to JSON-LD markup.");
        }
        else if(variable.getName().equals("Dimension")) {
            Element sizeAttr = new Element ("Attribute",DAP.DAPv40_NS);
            sizeAttr.setAttribute("name", "size");
            Element value = new Element("Value",DAP.DAPv40_NS);
            value.setText(variable.getAttributeValue("size"));
            sizeAttr.addContent(value);
            Vector<Element> attributes = new Vector<>();
            attributes.add(sizeAttr);
            sb.append(dap4AttributesToProperties(attributes, myIndent));
        }
        else {
            // It's an atomic variable or an Array!
            @SuppressWarnings("unchecked")
            List<Element> attributes = variable.getContent(dap4AttributeFilter);
            sb.append(dap4AttributesToProperties(attributes, myIndent));
        }

        sb.append("\n");
        sb.append(myIndent).append("]\n");
        sb.append(indent).append("}");


        return sb.toString();

    }


    public String dap4AttributesToProperties(List<Element> attributes, String indent){

        StringBuilder sb = new StringBuilder();
        String myIndent = indent + indent_inc;

        boolean first = true;
        int mark = sb.length();
        for(Element attribute : attributes){
            if(attribute.getChild("Attribute",DAP.DAPv40_NS)!=null){
                if(!first)
                    sb.append(",\n");
                // It's an AttrTable so dig...
                @SuppressWarnings("unchecked")
                List<Element> myAttributes = attribute.getContent(dap4AttributeFilter);
                sb.append(dap4AttributesToProperties(myAttributes,myIndent));
            }
            else {
                // Not an AttrTable so it must have values...
                String pValue = dap4AttributeToPropertyValue(attribute,myIndent);
                if(!pValue.isEmpty()){
                    if(!first)
                        sb.append(",\n");
                    sb.append(pValue);
                }
            }
            if(sb.length()>mark)
                first = false;
        }
        return sb.toString();
    }


    /**
     * Minmal JSON text encoder. This method escapes:
     * <ul>
     *     <li>The \ (backslash)</li>
     *     <li>The " (double-quote)</li>
     * </ul>
     * @param value The string to encode.
     * @return The encoded value
     */
    private String jsonEncodeString(String value){
        String str = value.trim();
        str = str.replace("\\","\\\\");
        str = str.replace("\"","\\\"");
        return str;
    }



    public String dap4AttributeToPropertyValue(Element attribute, String indent){
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<Element> values = attribute.getChildren("Value",DAP.DAPv40_NS);

        if(!values.isEmpty()){
            sb.append(indent).append("{\n");
            sb.append(indent).append(indent_inc).append("\"@type\": \"PropertyValue\", \n");
            sb.append(indent).append(indent_inc).append("\"name\": \"").append(jsonEncodeString(attribute.getAttributeValue("name"))).append("\", \n");
            //sb.append(indent).append(indent_inc).append("\"type\": \"").append(Encode.forJavaScript(attribute.getAttributeValue("type"))).append("\", \n");

            boolean jsEncode = true;
            String type = attribute.getAttributeValue("type");
            if(type !=null){
                type = type.toLowerCase();
                if (type.contains("int") ||
                        type.contains("float") ||
                        type.equals("byte")) {
                    jsEncode = false;
                }
            }
            if(values.size()==1){
                Element value = values.get(0);
                sb.append(indent).append(indent_inc).append("\"value\": \"");
                if(jsEncode) {
                    sb.append(jsonEncodeString(value.getTextTrim()));
                }
                else {
                    sb.append(value.getTextTrim());
                }
                sb.append("\"");
            }
            else {
                sb.append(indent).append(indent_inc).append("\"value\": [ ");
                boolean first = true;
                for(Element value : values){
                    if(!first)
                        sb.append(", ");
                    sb.append("\"");

                    if(jsEncode) {
                        sb.append(jsonEncodeString(value.getTextTrim()));
                    }
                    else {
                        sb.append(value.getTextTrim());
                    }
                    sb.append("\"");
                    first = false;
                }
                sb.append(" ]");
            }
            sb.append(indent).append("}");
        }
        return sb.toString();
    }


}
