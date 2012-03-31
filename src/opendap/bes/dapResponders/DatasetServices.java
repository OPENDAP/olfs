package opendap.bes.dapResponders;

import opendap.bes.BESDataSource;
import opendap.bes.BesDapResponder;
import opendap.coreServlet.DataSourceInfo;
import opendap.coreServlet.ReqInfo;
import opendap.dap.DapResponder;
import opendap.namespaces.XLINK;
import opendap.namespaces.XML;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/24/12
 * Time: 11:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatasetServices extends BesDapResponder {

    private Logger log;

    private static String defaultRequestSuffixRegex = ".*";

    private Vector<BesDapResponder> _responders = null;


    public DatasetServices(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public DatasetServices(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public DatasetServices(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/DatasetServices#");
        setServiceTitle("Dataset Services Description");
        setServiceDescription("An XML document itemizing the Services available for this dataset.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Services_Description_Service");
        setPreferredServiceSuffix("");

    }


     public void setDapResponders(Vector<BesDapResponder> responders){
        _responders = responders;
     }




    /**
     *
     * @param relativeUrl
     * @return
     */
    @Override
    public boolean matches(String relativeUrl) {


        Pattern p = getRequestMatchPattern();
        Matcher m = p.matcher(relativeUrl);

        if (m.matches()) {

            String besDataSourceId = relativeUrl;
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




    public void respondToHttpGetRequest(HttpServletRequest req, HttpServletResponse response) throws Exception {

        String name = ReqInfo.getLocalUrl(req);
        String datasetUrl = req.getRequestURL().toString();

        String context = req.getContextPath()+"/";

        String dataSourceId = ReqInfo.getBesDataSourceID(name);
        BesApi besApi = getBesApi();


        Document serviceDescription = new Document();

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/serviceDescription.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        serviceDescription.addContent( pi );

        Element datasetServices = getDatasetServicesElement(datasetUrl);


        serviceDescription.setRootElement(datasetServices);





        response.setContentType("text/xml");
        response.setHeader("Content-Description", "DAP Service Description");

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());



        xmlo.output(serviceDescription,response.getOutputStream());


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
















    private Element getDatasetServices(String datsetUrl){



        Element datasetServices = new Element("DatasetServices");
        datasetServices.setAttribute("base",datsetUrl, XML.NS);
        datasetServices.addNamespaceDeclaration(XLINK.NS);

        String suffix;
        Element service;
        Element description;
        Element serverSideFunctions;
        Element function;

        String role_id;

        suffix = ".html";
        role_id = "http://services.opendap.org/dap4/Dataset#";
        service = new Element("Service");
        service.setAttribute("title","HTML Data Request Form");
        //service.setAttribute("suffix",suffix);
        service.setAttribute("href",datsetUrl+suffix, XLINK.NS);
        service.setAttribute("role",role_id, XLINK.NS);

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
        service.setAttribute("title","Service Description");
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
