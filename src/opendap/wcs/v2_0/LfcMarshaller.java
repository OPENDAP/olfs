package opendap.wcs.v2_0;

import org.jdom.Element;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * This is a test class - leave it out of production build
 * @author ukari
 *
 */
public class LfcMarshaller {
	public static void main(String[] args) {
	
		// create root LFC
		LocalFileCatalog lfc = new LocalFileCatalog();


		
		ConcurrentHashMap<String,CoverageDescription> covs = 
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
		covs.put("1", cd);
		
		
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
		
		
		
		
		lfc.setCoverageDescriptionElements(covs);
		
		/*
		
		// EO Coverages
		
		ConcurrentHashMap<String,CoverageDescription> ecovs = 
				new ConcurrentHashMap<String,CoverageDescription>();
		
		CoverageDescription ecd = new CoverageDescription();
		try
		{
		 ecd.setDapDatasetUrl(new java.net.URL("http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc"));
		}
		catch (Exception e)
		{
		  System.out.println(e);
		}
		ecd.setMyFile(new java.io.File("eo_ncep_model_example.xml"));
		ecovs.put("1", ecd);
		
		
		// domain coordinates
		LinkedHashMap elhm = new LinkedHashMap();
		
		DomainCoordinate ed1  = new DomainCoordinate("time", "time", "112", "Hours since 2016-06-17T00:00:00.000Z");
		DomainCoordinate ed2  = new DomainCoordinate("latitude", "latitude", "361", "degrees_north");
		DomainCoordinate ed3  = new DomainCoordinate("longitude", "longitude", "720", "degrees_east");
		DomainCoordinate ed4  = new DomainCoordinate("isobaric", "isobaric", "31", "Pa");
		
		elhm.put("time", ed1); elhm.put("isobaric", ed4); elhm.put("latitude", ed2); elhm.put("longitude", ed3);
		
		ecd.setDomainCoordinatesLinkedHashMap(elhm);
	
		Field ef1 = new Field(); ef1.setDapId("u_component_of_wind_isobaric"); ef1.setName("u_component_of_wind_isobaric");
		Field ef2 = new Field(); ef2.setDapId("v_component_of_wind_isobaric"); ef2.setName("v_component_of_wind_isobaric");
		
		ArrayList<Field> efields = new ArrayList<Field>(); efields.add(ef1); efields.add(ef2);
		ecd.setFields(efields);
		
		
		
		
		
		
		lfc.setEoCoverageDescriptionElements(ecovs);
		
		
		
		// DataSeries
		DataSetSeries dss = new DataSetSeries();
		dss.name = "MODIS_L3_chl-a";
		dss.coverages = new Vector<CoverageDescription>();
		dss.coverages.add(cd); dss.coverages.add(ecd);
		
		
		lfc.setEoDataSetSeries(dss);
		
		
		// create coverage element(s)
		//WcsCoverage wcsc = new WcsCoverage();
		//wcsc.fileName="MERRA2_400.tavgM_2d_int_Nx.201601.nc4.xml";
		
		// add them to LFC root 
		//List<WcsCoverage> wcsCoverageList = new ArrayList<WcsCoverage>();
		//wcsCoverageList.add(wcsc);
		//lfc.wcs = wcsCoverageList;
		 * 
		 * 
		 */
		
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
