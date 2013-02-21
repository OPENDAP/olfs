package opendap.bes.dapResponders.inactive;

import opendap.bes.BESDataSource;
import opendap.bes.BesDapResponder;
import opendap.bes.dapResponders.BesApi;
import opendap.bes.dapResponders.DapDispatcher;
import opendap.coreServlet.DataSourceInfo;
import opendap.coreServlet.Util;
import opendap.dap.DapResponder;
import opendap.namespaces.XLINK;
import opendap.namespaces.XML;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





public class DatasetServices_OLD extends BesDapResponder {

    private Logger log;

    //private static String defaultRequestSuffixRegex = "\\.dmr(((\\.html)?)|((\\.xml)?))?$";
    //private static String defaultRequestSuffixRegex = "\\.dap(((\\.html)?)|((\\.xml)?))?$";


    private static String defaultRequestSuffixRegex = "(((\\.html)?)|((\\.xml)?))?$";


    private ConcurrentHashMap<String,String> typeToSuffixMap;




    private Vector<DapResponder> _responders = null;

    private Namespace dapDatasetServicesNS = Namespace.getNamespace("ds","http://xml.opendap.org/ns/DAP/4.0/dataset-services#");


    public DatasetServices_OLD(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public DatasetServices_OLD(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public DatasetServices_OLD(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        typeToSuffixMap  = new ConcurrentHashMap<String, String>();

        typeToSuffixMap.put("text/xml",".xml");
        typeToSuffixMap.put("text/html",".html");



        setServiceRoleId("http://services.opendap.org/dap4/dataset-services");
        setServiceTitle("Dataset Services Response");
        setServiceDescription("An XML document itemizing the Services available for this dataset.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Services_Description_Service");
        setPreferredServiceSuffix("");

    }


     public void setDapResponders(Vector<DapResponder> responders){
        _responders = responders;
     }




    /**
     *
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl) {



        Pattern suffixPattern = Pattern.compile(defaultRequestSuffixRegex, Pattern.CASE_INSENSITIVE);
        Matcher suffixMatcher = suffixPattern.matcher(relativeUrl);

        boolean suffixMatched = false;

        log.debug("suffixMatcher.hitEnd():     {}",suffixMatcher.hitEnd());

        while(!suffixMatcher.hitEnd()){
            suffixMatched = suffixMatcher.find();
            log.debug("{}", Util.checkRegex(suffixMatcher,suffixMatched));
        }


        String besDataSourceId;

        if(suffixMatched){
            int start =  suffixMatcher.start();
            besDataSourceId = relativeUrl.substring(0,start);

            log.debug("Asking BES about resource: {}", besDataSourceId);

            try {
                DataSourceInfo dsi = new BESDataSource(besDataSourceId, getBesApi());
                if (dsi.isDataset()) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("matches() failed with an Exception. Msg: '{}'", e.getMessage());
            }


        }


        return false;

    }

    public Element getDatasetServicesElement(String datasetUrl) {

        Element datasetServices = new Element("DatasetServices");
        datasetServices.setAttribute("base",datasetUrl, XML.NS);
        datasetServices.addNamespaceDeclaration(XLINK.NS);

        if(_responders!=null){
            for(DapResponder service : _responders ){
                 datasetServices.addContent(service.getServiceElement(datasetUrl));
            }
        }

        datasetServices.addContent(getServerSideFunctions(datasetUrl));

        return datasetServices;

    }



    public void respondToHttpGetRequest(HttpServletRequest req, HttpServletResponse response) throws Exception {

        String datasetUrl = req.getRequestURL().toString();

        String context = req.getContextPath()+"/";


        Document serviceDescription = new Document();

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/datasetServices.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        serviceDescription.addContent( pi );

        Element datasetServices;


        //datasetServices = getDatasetServicesElement(datasetUrl);
        datasetServices = getDatasetServicesVersion3(datasetUrl);


        serviceDescription.setRootElement(datasetServices);





        response.setContentType("text/xml");
        response.setHeader("Content-Description", "DAP Service Description");

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());



        xmlo.output(serviceDescription,response.getOutputStream());


    }









    private Element getLinkElement(String mediaType, String href, String description ){

        Element link = new Element("link",dapDatasetServicesNS);
        link.setAttribute("type",mediaType);
        link.setAttribute("href",href);
        if(description!=null && !description.equals(""))
            link.setAttribute("description",description);

        return link;
    }

    private Element getLinkElement(String mediaType, String href, String description, String[] alternateTypes ){

        Element link = getLinkElement(mediaType, href, description);
        Element alt;
        for(String altType:alternateTypes){
            alt =  new Element("alt",dapDatasetServicesNS);
            alt.setAttribute("type",altType);
            link.addContent(alt);
        }

        return link;
    }


    private Element getServiceElement(String title, String role, String description, String descriptionLink){

        Element service = new Element("Service",dapDatasetServicesNS);
        service.setAttribute("title",title);
        service.setAttribute("role",role);
        service.addContent(getDescriptionElement(description,descriptionLink));

        return service;
    }

    private Element getDescriptionElement(String description, String link){

        Element descriptionElement = new Element("Description",dapDatasetServicesNS);
        descriptionElement.setAttribute("href",link);

        descriptionElement.setText(description);


        return descriptionElement;
    }

    private Element getDatasetServicesVersion3(String datasetUrl){

        Element datasetServices = new Element("DatasetServices", dapDatasetServicesNS);
        datasetServices.setAttribute("base",datasetUrl, XML.NS);
        datasetServices.addNamespaceDeclaration(dapDatasetServicesNS);

        String suffix;
        String href, title, descriptionLink, description;
        String role_id;
        String[] alternateRepresentations;
        Element service;
        Element serverSideFunctions;
        Element link;


        // - - - - - - - - - - - - - - - - - - - - -
        // Dataset Services Response (DSR)
        // - - - - - - - - - - - - - - - - - - - - -


        suffix = "";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap4/dataset-services";
        title = "DAP4 Dataset Services";
        descriptionLink = "http://services.opendap.org/service_description.html";
        description =  "DAP4 Dataset Services Response, an index of the available services for this dataset.";
        alternateRepresentations = new String[]{"text/xml","text/html"};

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("application/vnd.opendap.org.dataset-services+xml",href,"The normative DSR.",alternateRepresentations );
        service.addContent(link);

        link = getLinkElement("text/html",href+".html","HTML presentation view of the DSR." );
        service.addContent(link);

        link = getLinkElement("text/xml",href+".xml","The normative response, but with a generic Content-Type" );
        service.addContent(link);

        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -
        // Dataset Metadata Response (DMR)
        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".dmr";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap4/dataset-metadata";
        title = "DAP4 Dataset Metadata";
        descriptionLink = "http://services.opendap.org/service_description.html";
        description =  "DAP4 Dataset Metadata Document contains the metadata content of the data resource.";
        alternateRepresentations = new String[]{"text/html","text/xml","application/rdf+xml"};

        service = getServiceElement(title, role_id,description,descriptionLink);



        link = getLinkElement("application/vnd.opendap.org.dataset-metadata+xml",href,"The normative DMR.",alternateRepresentations );
        service.addContent(link);

        link = getLinkElement("text/html",href+".html", "The Data Request Form" );
        service.addContent(link);

        link = getLinkElement("text/xml",href+".xml","The normative response, but with a generic Content-Type" );
        service.addContent(link);

        link = getLinkElement("application/rdf+xml",href+".rdf","RDF representation of DMR" );
        service.addContent(link);

        datasetServices.addContent(service);




        // - - - - - - - - - - - - - - - - - - - - -
        // DAP4 Data Response
        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".dap";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap4/data";
        title = "DAP4 Data";
        descriptionLink = "http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Data_Service";
        description =  "DAP4 Data Object.";
        alternateRepresentations = new String[]{"text/plain","text/xml","application/x-netcdf"};

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("application/vnd.org.opendap.dap4.data",href,"The normative DAP4 Data Response",alternateRepresentations );
        service.addContent(link);

        link = getLinkElement("text/plain",href+".html", "The ASCII Data response" );
        service.addContent(link);

        link = getLinkElement("text/xml",href+".xml","The XML Data response" );
        service.addContent(link);

        link = getLinkElement("application/x-netcdf",href+".nc","The NetCDF representation of the Data response." );
        service.addContent(link);

        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -


        href = datasetUrl;
        role_id = "http://services.opendap.org/dap4/iso-19115";
        title = "ISO-19115 Metadata";
        descriptionLink = "http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_ISO_19115_Service";
        description =  "ISO-19115 Metadata Representation of the DMR.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("text/xml",href+".iso","Dataset metadata as ISO-19115" );
        service.addContent(link);

        link = getLinkElement("text/html",href+".rubric","The ISO-19115 conformance score." );
        service.addContent(link);

        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -


        suffix = ".dods";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap2/data";
        title = "DAP2 Data";
        descriptionLink = "http://services.opendap.org/dap2_data.html";
        description =  "DAP2 Data Object.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("application/octet-stream",href,null );
        service.addContent(link);
        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".ddx";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap2/DDX";
        title = "DAP2 DDX";
        descriptionLink = "http://services.opendap.org/ddx.html";
        description =  "OPeNDAP Data Description and Attribute XML Document.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("text/xml",href,null );
        service.addContent(link);
        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -


        suffix = ".dds";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap2/DDS";
        title = "DAP2 DDS";
        descriptionLink = "http://services.opendap.org/dds.html";
        description =  "OPeNDAP Data Description Structure.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("text/plain",href,null );
        service.addContent(link);
        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".das";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap2/DAS";
        title = "DAP2 DAS";
        descriptionLink = "http://services.opendap.org/das.html";
        description =  "OPeNDAP Dataset Attribute Structure.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("text/plain",href,null );
        service.addContent(link);
        datasetServices.addContent(service);



        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".info";
        href = datasetUrl+suffix;
        role_id = "http://services.opendap.org/dap2/INFO";
        title = "DAP2 INFO";
        descriptionLink = "http://services.opendap.org/das.html";
        description =  "OPeNDAP Dataset Information Page.";

        service = getServiceElement(title, role_id,description,descriptionLink);

        link = getLinkElement("text/html",href,null );
        service.addContent(link);
        datasetServices.addContent(service);




        // - - - - - - - - - - - - - - - - - - - - -
        // - - - - - - - - - - - - - - - - - - - - -

        if(DapDispatcher.allowDirectDataSourceAccess()){


            suffix = ".file";
            href = datasetUrl+suffix;
            role_id = "http://services.opendap.org/dap2/FileAccess";
            title = "FileAccess";
            descriptionLink = "http://services.opendap.org/dap2/dataset_file_access.html";
            description =  "Access to dataset file.";

            service = getServiceElement(title, role_id,description,descriptionLink);

            link = getLinkElement("text/xml",href,null );
            service.addContent(link);
            datasetServices.addContent(service);

        }
        // - - - - - - - - - - - - - - - - - - - - -


        serverSideFunctions = getServerSideFunctions(datasetUrl);
        datasetServices.addContent(serverSideFunctions);





        return datasetServices;


    }












    private Element getDatasetServices(String datsetUrl){



        Element datasetServices = new Element("DatasetServices");
        datasetServices.setAttribute("base",datsetUrl, XML.NS);
        datasetServices.addNamespaceDeclaration(XLINK.NS);

        String suffix;
        Element service;
        Element description;
        Element serverSideFunctions;

        String role_id;

        suffix = ".html";
        role_id = "http://services.opendap.org/dap4/Dataset#";
        service = new Element("Service");
        service.setAttribute("title","HTML Data Request Form");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix);
        service.setAttribute("role",role_id);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/dataRequestForm.html",XLINK.NS);
        description.setText("OPeNDAP HTML Data Request Form for data constraints and access");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".dap";
        role_id = "http://services.opendap.org/dap4/Data#";
        service = new Element("Service");
        service.setAttribute("title","DAP4 Data");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/dap4_data.html",XLINK.NS);
        description.setText("DAP4 Data Object");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".dods";
        role_id = "http://services.opendap.org/dap2/Data#";
        service = new Element("Service");
        service.setAttribute("title","DAP2 Data");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/dap2_data.html",XLINK.NS);
        description.setText("DAP2 Data Object");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".ddx";
        role_id = "http://services.opendap.org/dap2/DDX#";
        service = new Element("Service");
        service.setAttribute("title","DDX");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/ddx.html",XLINK.NS);
        description.setText("OPeNDAP Data Description and Attribute XML Document");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".dds";
        role_id = "http://services.opendap.org/dap2/DDS#";
        service = new Element("Service");
        service.setAttribute("title","DDS");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/dds.html",XLINK.NS);
        description.setText("OPeNDAP Dataset Description Structure");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".das";
        role_id = "http://services.opendap.org/dap2/DAS#";
        service = new Element("Service");
        service.setAttribute("title","DAS");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/das.html",XLINK.NS);
        description.setText("OPeNDAP Dataset Attribute Structure (DAS)");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".info";
        role_id = "http://services.opendap.org/dap2/INFO#";
        service = new Element("Service");
        service.setAttribute("title","INFO");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/info.html",XLINK.NS);
        description.setText("OPeNDAP Dataset Information Page");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".rdf";
        role_id = "http://services.opendap.org/dap4/RDF#";
        service = new Element("Service");
        service.setAttribute("title","RDF");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/rdf.html",XLINK.NS);
        description.setText("An RDF representation of the DDX document.");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        suffix = ".nc";
        role_id = "http://services.opendap.org/dap4/NetCDF3#";
        service = new Element("Service");
        service.setAttribute("title","NetCDF-File");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/netcdf_fileout.html",XLINK.NS);
        description.setText("NetCDF file-out response.");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -


        suffix = ".iso";
        role_id = "http://services.opendap.org/dap4/ISO-19115#";
        service = new Element("Service");
        service.setAttribute("title","ISO-19115");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/iso_metedata.html",XLINK.NS);
        description.setText("ISO 19115 Metadata Representation of the DDX.");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -


        suffix = ".rubric";
        role_id = "http://services.opendap.org/dap4/ISO-19115-Score#";
        service = new Element("Service");
        service.setAttribute("title","ISO-19115-Score");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/iso_score.html",XLINK.NS);
        description.setText("ISO 19115 Metadata Representation conformance score for this dataset.");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        if(DapDispatcher.allowDirectDataSourceAccess()){

            suffix = ".file";
            role_id = "http://services.opendap.org/dap4/FileAccess#";
            service = new Element("Service");
            service.setAttribute("title","FileAccess");
            //service.setAttribute("suffix",suffix);
            service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
            service.setAttribute("role",role_id, XLINK.NS);

            description = new Element("Description");
            description.setAttribute("href","http://services.opendap.org/dataset_file_access.html",XLINK.NS);
            description.setText("Access to dataset file.");
            service.addContent(description);
            datasetServices.addContent(service);

        }
        // - - - - - - - - - - - - - - - - - - - - -



        suffix = "";
        role_id = "http://services.opendap.org/dap4/DatasetServices#";
        service = new Element("Service");
        service.setAttribute("title","DAP4 Dataset Services");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

        description = new Element("Description");
        description.setAttribute("href","http://services.opendap.org/service_description.html",XLINK.NS);
        description.setText("Service Description response.");
        service.addContent(description);
        datasetServices.addContent(service);

        // - - - - - - - - - - - - - - - - - - - - -

        serverSideFunctions = getServerSideFunctions(datsetUrl);
        datasetServices.addContent(serverSideFunctions);





        return datasetServices;


    }


    private Element getServerSideFunctions(String datasetUrl){



        Element description;
        Element serverSideFunctions;
        Element function;



        serverSideFunctions = new Element("ServerSideFunctions");

        function = new Element("Function");
        function.setAttribute("name","geogrid");
        function.setAttribute("href","http://docs.opendap.org/index.php/Server_Side_Processing_Functions#geogrid",XLINK.NS);

        description = new Element("Description");
        description.setText("Allows a DAP Grid variable to be sub-sampled using georeferenced values.");
        function.addContent(description);
        serverSideFunctions.addContent(function);

        function = new Element("Function");
        function.setAttribute("name","grid");
        function.setAttribute("href","http://docs.opendap.org/index.php/Server_Side_Processing_Functions#grid",XLINK.NS);

        description = new Element("Description");
        description.setText("Allows a DAP Grid variable to be sub-sampled using the values of the coordinate axes.");
        function.addContent(description);
        serverSideFunctions.addContent(function);

        function = new Element("Function");
        function.setAttribute("name","linear_scale");
        function.setAttribute("href","http://docs.opendap.org/index.php/Server_Side_Processing_Functions#linear_scale",XLINK.NS);

        description = new Element("Description");
        description.setText("Applies a linear scale transform to the named variable.");
        function.addContent(description);
        serverSideFunctions.addContent(function);

        function = new Element("Function");
        function.setAttribute("name","version");
        function.setAttribute("href","http://docs.opendap.org/index.php/Server_Side_Processing_Functions#version",XLINK.NS);

        description = new Element("Description");
        description.setText("Returns version information for each server side function.");
        function.addContent(description);
        serverSideFunctions.addContent(function);

        return serverSideFunctions;

    }

















}
