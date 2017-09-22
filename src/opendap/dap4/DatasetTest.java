package opendap.dap4;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Traditional  UnitOfWork_StateUnderTest Junit Tests on Dataset to assist in refactor, regression etc
 * and also an be do double duty as diagnostics on various Dataset DMRs through parameterization
 * which provides for the "happy path" of injecting the test Dataset as URL (in addition to this the 
 * dataset can be injected as a file or raw XML with known values - last option being useful for 
 * the traditional (as opposed to diagnostic) assertions.
 *     
 * @author udaykari
 *
 */
@RunWith(Parameterized.class)
public class DatasetTest {

  private Dataset dataset;
 
  private Logger _log;
  private boolean _datasetIsNotNull = false;
 
  /**
   * Inject dataSet via constructor 
   * @param dmrUrl
   */
  public DatasetTest(String testDmrUrl) throws 
    IOException, JDOMException, JAXBException, XMLStreamException  {
    _log = LoggerFactory.getLogger(this.getClass());
     Element dmrElement = opendap.xml.Util.getDocumentRoot(testDmrUrl,  opendap.http.Util.getNetRCCredentialsProvider());
     JAXBContext jc = JAXBContext.newInstance(Dataset.class);
     Unmarshaller um = jc.createUnmarshaller();
     XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
     String dmrXml = xmlo.outputString(dmrElement);
     InputStream is = new ByteArrayInputStream(dmrXml.getBytes("UTF-8"));
     XMLInputFactory factory = XMLInputFactory.newInstance();
     XMLStreamReader xsr = factory.createXMLStreamReader(is);
     XMLReaderWithNamespaceInMyPackageDotInfo xr = new XMLReaderWithNamespaceInMyPackageDotInfo(xsr);
     this.dataset = (Dataset) um.unmarshal(xr);
     if (dataset == null) {
       String msg = "JAXB failed to produce a Dataset from the DMR.";
       _log.debug(msg);
      } else {
       _datasetIsNotNull = true;
      }
  }

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
  //TODO: This is non-trivial test
  public void hasSrs() {
    Assume.assumeTrue(_datasetIsNotNull);
    assertTrue(true);
  }
  
  @Test
  //TODO: this is non-trivial test
  public void hasDimensions() {
    Assume.assumeTrue(_datasetIsNotNull);
    assertTrue(true);
  }
  
  @Test
  //TODO:  non-trivial
  public void hasEnvelopeWithTimePeriod() {
    Assume.assumeTrue(_datasetIsNotNull);
    assertTrue(true);
  }

  @Test
  public void hasDataResolution() {
    Assume.assumeTrue(_datasetIsNotNull);
    assertTrue(dataset.getValueOfGlobalAttributeWithNameLike("DataResolution") != null);
  }
  
  @Parameters
  public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][]{
        // run as many tests as needed - each URL corresponding to one DMR dataset (i.e. one test)
        {"https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml"},
        {"http://test.opendap.org/opendap/testbed-13/MERRA2_100.statD_2d_slv_Nx.19800101.SUB.nc4.dmr.xml"},
      });
  }
  
}
