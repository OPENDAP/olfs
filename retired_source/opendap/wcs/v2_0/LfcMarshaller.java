/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
 * // Author: Uday Kari  <ukari@opendap.org>
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This is a test class - leave it out of production build
 * @author ukari
 *
 */
@Deprecated
public class LfcMarshaller {
	public static void main(String[] args) {
	
		// create root LFC
		LocalFileCatalog lfc = new LocalFileCatalog();


		
		ConcurrentHashMap<String,CoverageDescription> coveragesMap = 
				new ConcurrentHashMap<String,CoverageDescription>();
		
		CoverageDescription cd = new CoverageDescription();
		try
		{
		 cd.setDapDatasetUrl("http://localhost:8080/opendap/testbed-13/MERRA2_400.tavgM_2d_int_Nx.201601.nc4");
		}
		catch (Exception e)
		{
		  System.out.println(e);
		}
		cd.setMyFile(new java.io.File("MERRA2_400.tavgM_2d_int_Nx.201601.nc4.xml"));
		coveragesMap.put("1", cd);
		
		
		// domain coordinates
		LinkedHashMap lhm = new LinkedHashMap();

		/*
		try
		{
			DomainCoordinate d1  = new DomainCoordinate("time","time","seconds since 1978-01-01","",108);
            DomainCoordinate d2  = new DomainCoordinate("latitude","latitude","degrees_north","",4800);
			DomainCoordinate d3  = new DomainCoordinate("longitude","longitude","degrees_east","",4800);

            cd.addDomainCoordinate(d1);
			cd.addDomainCoordinate(d2);
			cd.addDomainCoordinate(d3);
		}
		catch (Exception e)
		{
			
		}

        */

		try
		{
			Field f1  = new Field("tcwv");
			Field f2  = new Field("u");
			ArrayList<Field> fields = new ArrayList<Field>();
			fields.add(f1);
			fields.add(f2);
			// cd.setFields(fields);
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
		 ecd.setDapDatasetUrl("http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc");
		}
		catch (Exception e)
		{
		  System.out.println(e);
		}
		ecd.setMyFile(new java.io.File("eo_ncep_model_example.xml"));
		eoCoveragesMap.put("1", ecd);
		
		// domain coordinates
		LinkedHashMap elhm = new LinkedHashMap();
		      /*
		try
		{

            DomainCoordinate d1  = new DomainCoordinate("time","time","Hours since 2016-06-17T00:00:00.000Z","",112);

            DomainCoordinate d2  = new DomainCoordinate("latitude","latitude","degrees_north","",361);
            DomainCoordinate d3  = new DomainCoordinate("longitude","longitude","degrees_east","",720);
            DomainCoordinate d4  = new DomainCoordinate("isobaric","isobaric","Pa","",31);

            cd.addDomainCoordinate(d1);
			cd.addDomainCoordinate(d4);
			cd.addDomainCoordinate(d2);
			cd.addDomainCoordinate(d3);

		}
		catch (Exception e)
		{
			
		}
        */

		try
		{
            Field f3  = new Field("u_component_of_wind_isobaric");
            Field f4  = new Field("v_component_of_wind_isobaric");
			ArrayList<Field> efields = new ArrayList<Field>(); 
			efields.add(f3);
			efields.add(f4);
			// ecd.setFields(efields);
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
