<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="/opendap/xsl/datasetServices.xsl"?>
<ds:DatasetServices xmlns:ds="http://xml.opendap.org/ns/DAP/4.0/dataset-services#" xml:base="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65">
  <ds:DapVersion>3.2</ds:DapVersion>
  <ds:DapVersion>3.0</ds:DapVersion>
  <ds:DapVersion>2.0</ds:DapVersion>
  <ds:ServerSoftwareVersion>Hyrax-@HyraxVersion@</ds:ServerSoftwareVersion>
  <ds:Service title="DAP4 Data Response" role="http://services.opendap.org/dap4/data">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Data_Service">DAP4 Data Response object.</ds:Description>
    <ds:link type="application/vnd.org.opendap.dap4.data" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap" description="The normative form of the DAP4 Data Response">
      <ds:alt type="text/plain" />
      <ds:alt type="text/xml" />
      <ds:alt type="application/x-netcdf" />
      <ds:alt type="image/tiff;application=geotiff" />
      <ds:alt type="image/jp2;application=gmljp2" />
    </ds:link>
    <ds:link type="text/plain" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap.csv" description="A comma separated values (CSV) representation of the DAP4 Data Response object." />
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap.xml" description="XML representation of the DAP4 Data Response object." />
    <ds:link type="application/x-netcdf" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap.nc" description="NetCDF-3 representation of the DAP4 Data Response object." />
    <ds:link type="image/tiff;application=geotiff" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap.tiff" description="GeoTIFF representation of the DAP4 Data Response object." />
    <ds:link type="image/jp2;application=gmljp2" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dap.jp2" description="GML-JPEG2000 representation of the DAP4 Data Response object." />
  </ds:Service>
  <ds:Service title="Dataset Metadata Response" role="http://services.opendap.org/dap4/dataset-metadata">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata">DAP4 Dataset Description and Attribute XML Document.</ds:Description>
    <ds:link type="application/vnd.org.opendap.dap4.dataset-metadata+xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr" description="The normative form of the Dataset Metadata Response">
      <ds:alt type="text/xml" />
      <ds:alt type="text/html" />
      <ds:alt type="application/rdf+xml" />
    </ds:link>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr.xml" description="Normative representation of the Dataset Metadata Response document with a generic content type." />
    <ds:link type="text/html" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr.html" description="HTML representation of the Dataset Metadata Response document." />
    <ds:link type="application/rdf+xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr.rdf" description="RDF representation of the Dataset Metadata Response document." />
  </ds:Service>
  <ds:Service title="ISO-19115 Metadata" role="http://services.opendap.org/dap4/dataset-metadata">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata">ISO-19115 metadata extracted form the normative DMR.</ds:Description>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr.iso" description="The normative form of the ISO-19115 Metadata">
      <ds:alt type="text/html" />
    </ds:link>
    <ds:link type="text/html" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dmr.rubric" description="ISO-19115 Conformance Score for the Dataset Metadata Response document." />
  </ds:Service>
  <ds:Service title="Dataset Services Response" role="http://services.opendap.org/dap4/dataset-services">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Services_Description_Service">An XML document itemizing the Services available for this dataset.</ds:Description>
    <ds:link type="application/vnd.org.opendap.dap4.dataset-services+xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65" description="The normative form of the Dataset Services Response">
      <ds:alt type="text/html" />
      <ds:alt type="text/xml" />
    </ds:link>
    <ds:link type="text/html" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.html" description="The HTML representation of the DSR." />
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.xml" description="The XML representation of the DSR." />
  </ds:Service>
  <ds:Service title="Data File Access" role="http://services.opendap.org/dap4/file-access">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services">Simple (download) access to the underlying data file.</ds:Description>
    <ds:link type="*/*" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.file" description="The normative form of the Data File Access" />
  </ds:Service>
  <ds:Service title="DAP2 Data" role="http://services.opendap.org/dap2/data">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_Data_Service">DAP2 Data Object.</ds:Description>
    <ds:link type="application/octet-stream" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dods" description="The normative form of the DAP2 Data" />
  </ds:Service>
  <ds:Service title="DAP2 ASCII Data" role="http://services.opendap.org/dap2/ascii">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_ASCII_Data_Service">The DAP2 Data response in ASCII form.</ds:Description>
    <ds:link type="text/plain" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.asc(ii)?" description="The normative form of the DAP2 ASCII Data" />
  </ds:Service>
  <ds:Service title="DAP2 NetCDF-3 File" role="http://services.opendap.org/dap2/netcdf-3">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_NetCDF_File-out_Service">DAP2 data returned in a NetCDF-3 file.</ds:Description>
    <ds:link type="application/x-netcdf" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.nc" description="The normative form of the DAP2 NetCDF-3 File" />
  </ds:Service>
  <ds:Service title="DAP2 XML Data Response" role="http://services.opendap.org/dap2/xml-data">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_XML_Data_Service">An XML document containing both the DAP2 dataset's structural metadata along with data values.</ds:Description>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.xdap" description="The normative form of the DAP2 XML Data Response" />
  </ds:Service>
  <ds:Service title="DAP2 DDX" role="http://services.opendap.org/dap2/ddx">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_DDX_Service">OPeNDAP Data Description and Attribute XML Document.</ds:Description>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.ddx" description="The normative form of the DAP2 DDX" />
  </ds:Service>
  <ds:Service title="DAP2 DDS" role="http://services.opendap.org/dap2/dds">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_DDS_Service">DAP2 Data Description Structure (DDS).</ds:Description>
    <ds:link type="text/plain" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.dds" description="The normative form of the DAP2 DDS" />
  </ds:Service>
  <ds:Service title="DAP2 DAS" role="http://services.opendap.org/dap2/das">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_DAS_Service">DAP2 Dataset Attribute Structure (DAS).</ds:Description>
    <ds:link type="text/plain" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.das" description="The normative form of the DAP2 DAS" />
  </ds:Service>
  <ds:Service title="DAP2 RDF" role="http://services.opendap.org/dap4/rdf">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_RDF_Service">An RDF representation of the DAP2 Dataset response (DDX) document.</ds:Description>
    <ds:link type="application/rdf+xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.rdf" description="The normative form of the DAP2 RDF" />
  </ds:Service>
  <ds:Service title="DAP2 INFO" role="http://services.opendap.org/dap2/info">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_Info_Service">DAP2 Dataset Information HTML Page.</ds:Description>
    <ds:link type="text/html" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.info" description="The normative form of the DAP2 INFO" />
  </ds:Service>
  <ds:Service title="Server Software Version." role="http://services.opendap.org/dap4/server-version">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata">An XML document containing detailed software version information for this server.</ds:Description>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.ver" description="The normative form of the Server Software Version." />
  </ds:Service>
  <ds:Service title="ISO-19115 Metadata" role="http://services.opendap.org/dap4/dataset-metadata">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata">ISO-19115 metadata extracted form the normative DMR.</ds:Description>
    <ds:link type="text/xml" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.iso" description="The normative form of the ISO-19115 Metadata" />
  </ds:Service>
  <ds:Service title="ISO-19115 Conformance Score." role="http://services.opendap.org/dap4/dataset-metadata">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata">ISO-19115 Conformance Score for the Dataset Metadata Response document.</ds:Description>
    <ds:link type="text/html" href="http://localhost:8080/opendap/hyrax/gateway/687474703A2F2F6C6F63616C686F73743A383038302F6F70656E6461702F646174612F6E632F666E6F63312E6E632E66696C65.rubric" description="The normative form of the ISO-19115 Conformance Score." />
  </ds:Service>
  <ds:function name="geogrid" role="http://services.opendap.org/dap4/server-side-function/geogrid">
    <ds:Description href="http://docs.opendap.org/index.php/Server_Side_Processing_Functions#geogrid">Allows a DAP Grid variable to be sub-sampled using georeferenced values.</ds:Description>
  </ds:function>
  <ds:function name="grid" role="http://services.opendap.org/dap4/server-side-function/grid">
    <ds:Description href="http://docs.opendap.org/index.php/Server_Side_Processing_Functions#grid">Allows a DAP Grid variable to be sub-sampled using the values of the coordinate axes.</ds:Description>
  </ds:function>
  <ds:function name="linear_scale" role="http://services.opendap.org/dap4/server-side-function/linear_scale">
    <ds:Description href="http://docs.opendap.org/index.php/Server_Side_Processing_Functions#linear_scale">Applies a linear scale transform to the named variable.</ds:Description>
  </ds:function>
  <ds:function name="version" role="http://services.opendap.org/dap4/server-side-function/version">
    <ds:Description href="http://docs.opendap.org/index.php/Server_Side_Processing_Functions#version">Returns version information for each server side function.</ds:Description>
  </ds:function>
  <ds:extension name="async" role="http://services.opendap.org/dap4/extension/asynchronousTransactions">
    <ds:Description href="http://docs.opendap.org/index.php/DAP4:_Asynchronous_Request-Response_Proposal_v3">This server supports asynchronous transactions..</ds:Description>
  </ds:extension>
</ds:DatasetServices>

