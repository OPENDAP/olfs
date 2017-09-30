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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
      Path file = Paths.get("./resources/WCS/2.0/tests/xml/" + dmr);
      dmr = new String(Files.readAllBytes(file));
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

              {"dmrDataset_01.xml"},
              {"https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml"},
              {"http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml"},
      });
  }
  

}
