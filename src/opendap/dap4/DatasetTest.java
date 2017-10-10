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
package opendap.dap4;

import java.util.*;

import opendap.io.HyraxStringEncoding;
import org.junit.*;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Dataset Tests
 * 
 * JUnit 4 parameterization providing for 
 * injecting test Dataset(s) 
 */
@RunWith(Parameterized.class)
public class DatasetTest {

    private Dataset dataset;

    private Logger _log;
    private boolean _datasetIsNotNull = false;

    /**
     * Inject dataSet via constructor
     *
     * @param dmrUrl
     */
    public DatasetTest(String dmrUrl)
            throws IOException, JDOMException, JAXBException, XMLStreamException {
        _log = LoggerFactory.getLogger(this.getClass());
        String dmrXml = "";
        JAXBContext jc = JAXBContext.newInstance(Dataset.class);
        Unmarshaller um = jc.createUnmarshaller();
        if (dmrUrl.startsWith("http")) {
            Element dmrElement = opendap.xml.Util.getDocumentRoot(dmrUrl, opendap.http.Util.getNetRCCredentialsProvider());
            if(dmrElement==null)
                throw new IOException("Failed to get DMR document root for "+dmrUrl);
            
            XMLOutputter xmlo = new XMLOutputter();
            dmrXml = xmlo.outputString(dmrElement);
        } else {
            // not protocol like http or ftp?  then raw hard-wired data from resources directory
            _log.debug("Current relative path is: " + Paths.get(".").toAbsolutePath().normalize().toString());
            //TODO: get this from configs
            Path file = Paths.get("./resources/WCS/2.0/tests/xml/" + dmrUrl);
            dmrXml = new String(Files.readAllBytes(file), HyraxStringEncoding.getCharset());
        }
        InputStream is = new ByteArrayInputStream(dmrXml.getBytes("UTF-8"));
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader xsr = factory.createXMLStreamReader(is);
        XMLReaderWithNamespaceInMyPackageDotInfo xr = new XMLReaderWithNamespaceInMyPackageDotInfo(xsr);
        this.dataset = (Dataset) um.unmarshal(xr);
        if (dataset == null) {
            String msg = "JAXB failed to produce a Dataset from the DMR...aborting all tests";
            _log.debug(msg);
        } else {
            _datasetIsNotNull = true;
        }
    }


    ////////////////////////
    // Unit tests
    // (on raw DMR dataset XML , already embedded in this class)

    @Test
    public void hasExactlyFiveFloat32Variables() {
        // multiple raw datasets can be tested with _datasetIsRaw, _datasetIsRaw1 etc
        Assume.assumeTrue(_datasetIsNotNull);
        assertTrue(dataset.getVars32bitFloats().size() == 5);
    }

    @Test
    public void hasLatLongTimeDimensionsOfExpectedSize() {
        Assume.assumeTrue(_datasetIsNotNull);
        Dimension time = dataset.getDimension("/time");
        Dimension lat = dataset.getDimension("/lat");
        Dimension lon = dataset.getDimension("/lon");
        int timeSize = Integer.parseInt(time.getSize());
        int latSize = Integer.parseInt(lat.getSize());
        int lonSize = Integer.parseInt(lon.getSize());
        assertTrue(timeSize == 1 && latSize == 361 && lonSize == 576);

    }


    ////////////////////////
    //  Diagnostics
    //  (on external URLs)

    @Test
    public void usesCfConventions() {
        Assume.assumeTrue(_datasetIsNotNull);
        assertTrue(dataset.usesCfConventions());
    }

    @Test
    public void hasVariables() {
        Assume.assumeTrue(_datasetIsNotNull);
        assertTrue(dataset.getVariables().size() > 0);
    }

    @Test
    public void hasDataResolution() {
        Assume.assumeTrue(_datasetIsNotNull);
        assertTrue(dataset.getValueOfGlobalAttributeWithNameLike("DataResolution") != null);
    }


    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // run as many tests as needed - each corresponding to one DMR dataset (i.e. one test)
                {"dmrDataset_01.xml"},
                {"https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml"},
                {"http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml"},
        });
    }
    
}
