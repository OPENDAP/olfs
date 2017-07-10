package opendap.wcs.v2_0;

import org.jdom.Element;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.*;


/**
 * This is a test class - leave it out of production build
 * @author ukari
 *
 */
public class LfcMarshaller {
	public static void main(String[] args) {
	
		// create root LFC
		LocalFileCatalog lfc = new LocalFileCatalog();


		
		ConcurrentHashMap<String,CoverageDescription> coveragesMap = 
				new ConcurrentHashMap<String,CoverageDescription>();
		
		CoverageDescription cd = new CoverageDescription();
		try
		{
		 cd.setDapDatasetUrl(new java.net.URL("http://localhost:8080/opendap/testbed-13/MERRA2_400.tavgM_2d_int_Nx.201601.nc4"));
		}
		catch (Exception e)
		{
		  System.out.println(e);
		}
		cd.setMyFile(new java.io.File("MERRA2_400.tavgM_2d_int_Nx.201601.nc4.xml"));
		coveragesMap.put("1", cd);
		
		
		// domain coordinates
		LinkedHashMap lhm = new LinkedHashMap();
		
		try
		{
			Element e1 = new Element("DomainCoordinate"); 
			e1.setAttribute("name", "time"); e1.setAttribute("dapID", "time");e1.setAttribute("size", "108"); 
			e1.setAttribute("units", "seconds since 1970-01-01");
			DomainCoordinate d1  = new DomainCoordinate(e1);
			
			Element e2 = new Element("DomainCoordinate"); 
			e2.setAttribute("name", "latitude"); e2.setAttribute("dapID", "latitude");e2.setAttribute("size", "4800"); 
			e2.setAttribute("units", "degrees_north");
			DomainCoordinate d2  = new DomainCoordinate(e2);
			
			Element e3 = new Element("DomainCoordinate"); 
			e3.setAttribute("name", "longitude"); e3.setAttribute("dapID", "longitude");e3.setAttribute("size", "4800"); 
			e3.setAttribute("units", "degrees_east");
			DomainCoordinate d3  = new DomainCoordinate(e3);
				
			
			lhm.put("time", d1); lhm.put("latitude", d2); lhm.put("longitude", d3);
		}
		catch (Exception e)
		{
			
		}
		
		
		
		cd.setDomainCoordinatesLinkedHashMap(lhm);
	
		
		
		try
		{
			Element e4 = new Element("field"); e4.setNamespace(WCS.SWE_NS);
			e4.setAttribute("name", "tcwv");
			Field f1  = new Field(e4);
			
			Element e5 = new Element("field"); e5.setNamespace(WCS.SWE_NS);
			e5.setAttribute("name", "u");
			Field f2  = new Field(e5);
			
			
			ArrayList<Field> fields = new ArrayList<Field>(); fields.add(f1); fields.add(f2);
			cd.setFields(fields);
			
		}
		catch (Exception e)
		{
			
		}
		
		
		
		
		lfc.setCoverageDescriptionElements(coveragesMap);
		
		
		
		// EO WCS Coverages
		
		ConcurrentHashMap<String,EOCoverageDescription> eoCoveragesMap = 
				new ConcurrentHashMap<String,EOCoverageDescription>();
		
		EOCoverageDescription ecd = new EOCoverageDescription();
		try
		{
		 ecd.setDapDatasetUrl(new java.net.URL("http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc"));
		}
		catch (Exception e)
		{
		  System.out.println(e);
		}
		ecd.setMyFile(new java.io.File("eo_ncep_model_example.xml"));
		eoCoveragesMap.put("1", ecd);
		
		// domain coordinates
		LinkedHashMap elhm = new LinkedHashMap();
		
		try
		{
			Element e6 = new Element("DomainCoordinate"); 
			e6.setAttribute("name", "time"); e6.setAttribute("dapID", "time");e6.setAttribute("size", "112"); 
			e6.setAttribute("units", "Hours since 2016-06-17T00:00:00.000Z");
			DomainCoordinate d1  = new DomainCoordinate(e6);
			
			Element e7 = new Element("DomainCoordinate"); 
			e7.setAttribute("name", "latitude"); e7.setAttribute("dapID", "latitude");e7.setAttribute("size", "361"); 
			e7.setAttribute("units", "degrees_north");
			DomainCoordinate d2  = new DomainCoordinate(e7);
			
			Element e8 = new Element("DomainCoordinate"); 
			e8.setAttribute("name", "longitude"); e8.setAttribute("dapID", "longitude");e8.setAttribute("size", "720"); 
			e8.setAttribute("units", "degrees_east");
			DomainCoordinate d3  = new DomainCoordinate(e8);
				
			Element e9 = new Element("DomainCoordinate"); 
			e9.setAttribute("name", "isobaric"); e9.setAttribute("dapID", "isobaric");e9.setAttribute("size", "31"); 
			e9.setAttribute("units", "Pa");
			DomainCoordinate d4  = new DomainCoordinate(e9);
				
			elhm.put("time", d1); elhm.put("isobaric", d4); elhm.put("latitude", d2); elhm.put("longitude", d3);
		}
		catch (Exception e)
		{
			
		}
		

		ecd.setDomainCoordinatesLinkedHashMap(elhm);
		
		
		try
		{
			Element e10 = new Element("field"); e10.setNamespace(WCS.SWE_NS);
			e10.setAttribute("name", "u_component_of_wind_isobaric");
			Field f3  = new Field(e10);
			
			Element e11 = new Element("field"); e11.setNamespace(WCS.SWE_NS);
			e11.setAttribute("name", "v_component_of_wind_isobaric");
			Field f4  = new Field(e11);
			
			
			ArrayList<Field> efields = new ArrayList<Field>(); 
			efields.add(f3); efields.add(f4);
			
			ecd.setFields(efields);
			
		}
		catch (Exception e)
		{
			
		}
	

		lfc.setEoCoverageDescriptionElements(eoCoveragesMap);
		
		
		
		// DataSeries
		
		ConcurrentHashMap<String,EODatasetSeries> datasetSeriesMap = 
				new ConcurrentHashMap<String,EODatasetSeries>();
		
		
		EODatasetSeries dss = new EODatasetSeries();
		dss.setId("MODIS_L3_chl-a");
		
		Vector<EOCoverageDescription> members = new Vector();
		members.add(ecd);
        dss.setEoCoverageDescriptionElements(members);
		
		datasetSeriesMap.put("1", dss);
		lfc.setEoDataSeriesElements(datasetSeriesMap);
		
		
		//Marshall the LFC
		 try 
		 {

				
				JAXBContext jaxbContext = JAXBContext.newInstance(LocalFileCatalog.class);
				Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

				// output pretty printed
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

				
				jaxbMarshaller.marshal(lfc, System.out);

			 } 
		     catch (JAXBException e) 
		     {
				e.printStackTrace();
			 }
		
		
		
	}
	
}
