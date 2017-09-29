/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
 * // Author: Uday Kari <ukari@opendap.org>
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
package opendap.wcs.v2_0;

import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.jdom.Element;
import org.jdom.JDOMException;

import opendap.dap4.Dataset;
import opendap.wcs.srs.SimpleSrs;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.*;
import javax.xml.stream.XMLStreamException;

/**
 * Junit Tests on DynamicCoverageDescription
 *
 * @author Uday Kari
 */
@RunWith(Parameterized.class)

/**
 * Tests Dynamic Coverage Description
 * @author Uday Kari
 *
 */
public class DynamicCoverageDescriptionTest {

  private Element _dmrElement;
  private DynamicService _dynamicService;

  /**
   * Inject DMR via constructor
   *
   * @param dmrUrl
   */
  public DynamicCoverageDescriptionTest(String dmr) throws
  IOException, JDOMException, JAXBException, XMLStreamException {
    JAXBContext jc = JAXBContext.newInstance(Dataset.class);
    Unmarshaller um = jc.createUnmarshaller();
    if (dmr.startsWith("http")) {
        _dmrElement = opendap.xml.Util.getDocumentRoot(dmr, opendap.http.Util.getNetRCCredentialsProvider());
    } else {
      InputStream stream = new ByteArrayInputStream(dmr.getBytes(StandardCharsets.UTF_8.name()));
      _dmrElement = opendap.xml.Util.getDocument(stream).detachRootElement();
    }
    
    // construct mock dynamic service
    SimpleSrs defaultSrs = new SimpleSrs("urn:ogc:def:crs:EPSG::4326", "latitude longitude", "deg deg", 2);
    DynamicService ds = new DynamicService();
    ds.setSrs(defaultSrs);

    String s = "time";
    DomainCoordinate dc = new DomainCoordinate(s, s, "minutes since 1980-01-01 00:30:00", "", 1);
    dc.setMin(690);
    dc.setMax(9330);
    ds.addDomainCoordinate(dc);

    s = "latitude";
    dc = new DomainCoordinate(s, s, "deg", "", 361);
    dc.setMin(-90.0);
    dc.setMax(90.0);
    ds.addDomainCoordinate(dc);

    s = "longitude";
    dc = new DomainCoordinate(s, s, "deg", "", 576);
    dc.setMin(-180.0);
    dc.setMax(179.625);
    ds.addDomainCoordinate(dc);

    _dynamicService = ds;

  }
  

  @Test
  public void itAllWorks() throws IOException, WcsException {
    CoverageDescription cd = new DynamicCoverageDescription(_dmrElement, _dynamicService);
    assertTrue(cd != null);
  }
  
  
  @Parameters
  public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][]{
              // run as many tests as needed - each URL corresponding to one DMR dataset (i.e. one test)

              // this is raw xml from: http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml
              {"<Dataset xmlns=\"http://xml.opendap.org/ns/DAP/4.0#\" xml:base=\"http://test.opendap.org:80/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4\" dapVersion=\"4.0\" dmrVersion=\"1.0\" name=\"MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4\"><Dimension name=\"time\" size=\"1\" /><Dimension name=\"lat\" size=\"361\" /><Dimension name=\"lon\" size=\"576\" /><Float64 name=\"time\"><Dim name=\"/time\" /><Attribute name=\"standard_name\" type=\"String\"><Value>time</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>time</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>minutes since 1980-01-01 00:30:00</Value></Attribute><Attribute name=\"calendar\" type=\"String\"><Value>standard</Value></Attribute></Float64><Float64 name=\"lat\"><Dim name=\"/lat\" /><Attribute name=\"standard_name\" type=\"String\"><Value>latitude</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>latitude</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>degrees_north</Value></Attribute><Attribute name=\"axis\" type=\"String\"><Value>Y</Value></Attribute></Float64><Float64 name=\"lon\"><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>longitude</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>longitude</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>degrees_east</Value></Attribute><Attribute name=\"axis\" type=\"String\"><Value>X</Value></Attribute></Float64><Float32 name=\"HOURNORAIN\"><Dim name=\"/time\" /><Dim name=\"/lat\" /><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>time-during_an_hour_with_no_precipitation</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>time-during_an_hour_with_no_precipitation</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>s</Value></Attribute><Attribute name=\"_FillValue\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"missing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"fmissing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmax\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmin\" type=\"Float32\"><Value>-9.99999987e+14</Value></Attribute><Map name=\"/time\" /><Map name=\"/lat\" /><Map name=\"/lon\" /></Float32><Float32 name=\"T2MMAX\"><Dim name=\"/time\" /><Dim name=\"/lat\" /><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>K</Value></Attribute><Attribute name=\"_FillValue\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"missing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"fmissing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmax\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmin\" type=\"Float32\"><Value>-9.99999987e+14</Value></Attribute><Map name=\"/time\" /><Map name=\"/lat\" /><Map name=\"/lon\" /></Float32><Float32 name=\"T2MMEAN\"><Dim name=\"/time\" /><Dim name=\"/lat\" /><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>K</Value></Attribute><Attribute name=\"_FillValue\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"missing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"fmissing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmax\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmin\" type=\"Float32\"><Value>-9.99999987e+14</Value></Attribute><Map name=\"/time\" /><Map name=\"/lat\" /><Map name=\"/lon\" /></Float32><Float32 name=\"T2MMIN\"><Dim name=\"/time\" /><Dim name=\"/lat\" /><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>2-meter_air_temperature</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>K</Value></Attribute><Attribute name=\"_FillValue\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"missing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"fmissing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmax\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmin\" type=\"Float32\"><Value>-9.99999987e+14</Value></Attribute><Map name=\"/time\" /><Map name=\"/lat\" /><Map name=\"/lon\" /></Float32><Float32 name=\"TPRECMAX\"><Dim name=\"/time\" /><Dim name=\"/lat\" /><Dim name=\"/lon\" /><Attribute name=\"standard_name\" type=\"String\"><Value>total_precipitation</Value></Attribute><Attribute name=\"long_name\" type=\"String\"><Value>total_precipitation</Value></Attribute><Attribute name=\"units\" type=\"String\"><Value>kg m-2 s-1</Value></Attribute><Attribute name=\"_FillValue\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"missing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"fmissing_value\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmax\" type=\"Float32\"><Value>9.99999987e+14</Value></Attribute><Attribute name=\"vmin\" type=\"Float32\"><Value>-9.99999987e+14</Value></Attribute><Map name=\"/time\" /><Map name=\"/lat\" /><Map name=\"/lon\" /></Float32><Attribute name=\"NC_GLOBAL\" type=\"Container\"><Attribute name=\"CDI\" type=\"String\"><Value>Climate Data Interface version 1.6.9 (http://mpimet.mpg.de/cdi)</Value></Attribute><Attribute name=\"history\" type=\"String\"><Value>Thu Aug 17 14:16:14 2017: /usr/bin/ncks -O -L 1 --cnk_plc=g2d --cnk_dmn=lon,576 --cnk_dmn=lat,361 --cnk_dmn=time,1 /tmpdata/regridder/services_47574/cdoMERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4c /tmpdata/regridder/services_47574/chunkdeflatecdoMERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4c\012Thu Aug 17 14:16:14 2017: cdo -s -L -f nc4c -sellonlatbox,-180.0,180.0,-90.0,90.0 /ftp/data/s4pa/MERRA2/M2SDNXSLV.5.12.4/1980/01/MERRA2_100.statD_2d_slv_Nx.19800101.nc4 /tmpdata/regridder/services_47574/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4c</Value></Attribute><Attribute name=\"Conventions\" type=\"String\"><Value>CF-1</Value></Attribute><Attribute name=\"nco_openmp_thread_number\" type=\"Int32\"><Value>1</Value></Attribute><Attribute name=\"Comment\" type=\"String\"><Value>GMAO filename: d5124_m2_jan79.statD_2d_slv_Nx.19800101.nc4</Value></Attribute><Attribute name=\"History\" type=\"String\"><Value>Original file generated: Sat May 31 17:21:11 2014 GMT</Value></Attribute><Attribute name=\"Filename\" type=\"String\"><Value>MERRA2_100.statD_2d_slv_Nx.19800101.nc4</Value></Attribute><Attribute name=\"Institution\" type=\"String\"><Value>NASA Global Modeling and Assimilation Office</Value></Attribute><Attribute name=\"References\" type=\"String\"><Value>http://gmao.gsfc.nasa.gov</Value></Attribute><Attribute name=\"Format\" type=\"String\"><Value>NetCDF-4/HDF-5</Value></Attribute><Attribute name=\"SpatialCoverage\" type=\"String\"><Value>global</Value></Attribute><Attribute name=\"VersionID\" type=\"String\"><Value>5.12.4</Value></Attribute><Attribute name=\"TemporalRange\" type=\"String\"><Value>1980-01-01 -&gt; 2016-12-31</Value></Attribute><Attribute name=\"identifier_product_doi_authority\" type=\"String\"><Value>http://dx.doi.org/</Value></Attribute><Attribute name=\"ShortName\" type=\"String\"><Value>M2SDNXSLV</Value></Attribute><Attribute name=\"GranuleID\" type=\"String\"><Value>MERRA2_100.statD_2d_slv_Nx.19800101.nc4</Value></Attribute><Attribute name=\"ProductionDateTime\" type=\"String\"><Value>Original file generated: Sat May 31 17:21:11 2014 GMT</Value></Attribute><Attribute name=\"LongName\" type=\"String\"><Value>MERRA2 statD_2d_slv_Nx: 2d,Daily,Aggregated Statistics,Single-Level,Assimilation,Single-Level Diagnostics</Value></Attribute><Attribute name=\"Title\" type=\"String\"><Value>MERRA2 statD_2d_slv_Nx: 2d,Daily,Aggregated Statistics,Single-Level,Assimilation,Single-Level Diagnostics</Value></Attribute><Attribute name=\"SouthernmostLatitude\" type=\"String\"><Value>-90.0</Value></Attribute><Attribute name=\"NorthernmostLatitude\" type=\"String\"><Value>90.0</Value></Attribute><Attribute name=\"WesternmostLongitude\" type=\"String\"><Value>-180.0</Value></Attribute><Attribute name=\"EasternmostLongitude\" type=\"String\"><Value>179.375</Value></Attribute><Attribute name=\"LatitudeResolution\" type=\"String\"><Value>0.5</Value></Attribute><Attribute name=\"LongitudeResolution\" type=\"String\"><Value>0.625</Value></Attribute><Attribute name=\"DataResolution\" type=\"String\"><Value>0.5 x 0.625</Value></Attribute><Attribute name=\"Contact\" type=\"String\"><Value>http://gmao.gsfc.nasa.gov</Value></Attribute><Attribute name=\"identifier_product_doi\" type=\"String\"><Value>10.5067/9SC1VNTWGWV3</Value></Attribute><Attribute name=\"RangeBeginningDate\" type=\"String\"><Value>1980-01-01</Value></Attribute><Attribute name=\"RangeBeginningTime\" type=\"String\"><Value>00:00:00.000000</Value></Attribute><Attribute name=\"RangeEndingDate\" type=\"String\"><Value>1980-01-01</Value></Attribute><Attribute name=\"RangeEndingTime\" type=\"String\"><Value>23:59:59.000000</Value></Attribute><Attribute name=\"CDO\" type=\"String\"><Value>Climate Data Operators version 1.6.9 (http://mpimet.mpg.de/cdo)</Value></Attribute><Attribute name=\"NCO\" type=\"String\"><Value>20170817</Value></Attribute></Attribute><Attribute name=\"DODS_EXTRA\" type=\"Container\"><Attribute name=\"Unlimited_Dimension\" type=\"String\"><Value>time</Value></Attribute></Attribute></Dataset>"},
              {"https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml"},
              {"http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml"},
      });
  }
  

}
