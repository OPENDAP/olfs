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

package opendap.dap4;

import java.io.*;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.stream.*;

import org.jdom.Document;

import opendap.threddsHandler.*;

/**
 * Parse, sanity-check and echo DMR
 * @author ukari
 *
 */
public class DmrParser {
  public static void main(String[] args) {	

	ThreddsCatalogUtil tcc = new ThreddsCatalogUtil();

	// Get one DMR document and print it.
	String dmrXml = ""; 
	Dataset dataset=null;
	try
	{
	  Document dmr = tcc.getDocument("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml");
	  dmrXml = tcc.getXmlo().outputString(dmr);
	  
	  InputStream is = new ByteArrayInputStream(dmrXml.getBytes("UTF-8"));
	  XMLInputFactory factory = XMLInputFactory.newInstance();
	  XMLStreamReader xsr = factory.createXMLStreamReader(is);
	  XMLReaderWithNamespaceInMyPackageDotInfo xr = new XMLReaderWithNamespaceInMyPackageDotInfo(xsr);
	  
	  JAXBContext ctx = JAXBContext.newInstance(Dataset.class);
	  Unmarshaller um = ctx.createUnmarshaller();
	  
	  dataset = (Dataset) um.unmarshal(xr);
	  
	}
	catch (Exception e)
	{
	  System.out.println("could not get DMR, Exception: " + e);
	}
	
	// set the WCS coverage ID
	if (dataset == null) 
	{
		System.out.println("dataset....NULL; bye-bye");
		System.exit(0);
	}
	else
	{
		
		//////////////////////////////////////////////////////////
		//  this else block extends all the way to end of program
    	
    	System.out.println("Marshalling WCS from DMR at Url: ");
    	System.out.println(dataset.getUrl());
    	
    	////////////////////////////
    	// echo data set Dimensions - distinct from variable dimension
    	
    	List<Dimension> dimensions = dataset.getDimensions();
    	
    	if (dimensions == null)
    	{
    		System.out.println ("Dimensions List NULL");
    	}
    	else if (dimensions.size() == 0)
    	{
    		System.out.println("Dimensions list EMPTY");
    	}
    	else
    	{
    		ListIterator dimIter = dimensions.listIterator();
    		// we know it is non-null and non-empty, so safe to do this right away
    		while(dimIter.hasNext()) {
    			System.out.println(dimIter.nextIndex() + ". " +  dimIter.next());
    		}
    	} // close if then else (dimensions list is non-null and has elements)
    	
    	
    	////////////
    	// Echo Floats, Ints, just for testing
    	
    	List<Float32> float32s = dataset.getVars32bitFloats();
    	List<Float64> float64s = dataset.getVars64bitFloats();

    	List<Int32> int32s = dataset.getVars32bitIntegers();
    	List<Int64> int64s = dataset.getVars64bitIntegers();    	
    	
    	
    	if (float32s == null || float32s.isEmpty())
    	{
    		// do nothing
    	}
    	else
    	{
    		ListIterator<Float32> it = float32s.listIterator();
    		while(it.hasNext())
    		{
    			int ind = it.nextIndex();
    			Float32 var = it.next();
    			
    			System.out.println(ind + ". " + var);
    			
    			
    			// list all dims of this Float32
    			List<Dim> dims = var.getDims();
    			
    			if (dims == null || dims.isEmpty())
    			{
    				// do nothing
    			}
    			else
    			{
    				
    				ListIterator<Dim> dit = dims.listIterator();
    				while(dit.hasNext())
    				{
    					int j = dit.nextIndex();
    					Dim d = dit.next();
    					
    					System.out.println(j + ". " + d);
    					
    				} // end while dim iterator has more elements
    				
    			} // end if else dims is not NULL or EMPTY
    			
    			// list atrributes of this Float32 element
    			List<Attribute> attrs = var.getAttributes();
    			
    			if (attrs == null || attrs.isEmpty())
    			{
    				System.out.println("Float 32 has not attributes..??");
    			}
    			else
    			{
    				
    				ListIterator<Attribute> atit = attrs.listIterator();
    				while(atit.hasNext())
    				{
    					int k = atit.nextIndex();
    					Attribute a = atit.next();
    					
    					System.out.println(k + ". " + a);
    					
    				} // end while attributes iterator has more elements
    				
    			} // end if else attributes List is not NULL or EMPTY
    			
   
    		} // end while floats32 iterator has more elements
    	} // if elseif else (Float32 is not NULL or empty)
    	
    	
    	
    	// float 64
    	
    	if (float64s == null || float64s.isEmpty())
    	{
    		// do nothing
    	}
    	else
    	{
    		ListIterator<Float64> it = float64s.listIterator();
    		while(it.hasNext())
    		{
    			int ind = it.nextIndex();
    			Float64 var = it.next();
    			
    			System.out.println(ind + ". " + var);
    			
    			
    			// list all dims of this Float64
    			List<Dim> dims = var.getDims();
    			
    			if (dims == null || dims.isEmpty())
    			{
    				// do nothing
    			}
    			else
    			{
    				
    				ListIterator<Dim> dit = dims.listIterator();
    				while(dit.hasNext())
    				{
    					int j = dit.nextIndex();
    					Dim d = dit.next();
    					
    					System.out.println(j + ". " + d);
    					
    				} // end while dim iterator has more elements
    				
    			} // end if else dims is not NULL or EMPTY
    			
    			// list atrributes of this Float32 element
    			List<Attribute> attrs = var.getAttributes();
    			
    			if (attrs == null || attrs.isEmpty())
    			{
    				System.out.println("Float 64 has not attributes..??");
    			}
    			else
    			{
    				
    				ListIterator<Attribute> atit = attrs.listIterator();
    				while(atit.hasNext())
    				{
    					int k = atit.nextIndex();
    					Attribute a = atit.next();
    					
    					System.out.println(k + ". " + a);
    					
    				} // end while attributes iterator has more elements
    				
    			} // end if else attributes List is not NULL or EMPTY
    			
   
    		} // end while floats64 iterator has more elements
    	} // if elseif else (Float64 is not NULL or empty)
    	
    	
    	// Int32
    	
    	if (int32s == null || int32s.isEmpty())
    	{
    		// do nothing
    	}
    	else
    	{
    		ListIterator<Int32> it = int32s.listIterator();
    		while(it.hasNext())
    		{
    			int ind = it.nextIndex();
    			Int32 var = it.next();
    			
    			System.out.println(ind + ". " + var);
    			
    			
    			// list all dims of this int32
    			List<Dim> dims = var.getDims();
    			
    			if (dims == null || dims.isEmpty())
    			{
    				// do nothing
    			}
    			else
    			{
    				
    				ListIterator<Dim> dit = dims.listIterator();
    				while(dit.hasNext())
    				{
    					int j = dit.nextIndex();
    					Dim d = dit.next();
    					
    					System.out.println(j + ". " + d);
    					
    				} // end while dim iterator has more elements
    				
    			} // end if else dims is not NULL or EMPTY
    			
    			// list atrributes of this Int32 element
    			List<Attribute> attrs = var.getAttributes();
    			
    			if (attrs == null || attrs.isEmpty())
    			{
    				System.out.println("Int 32 has not attributes..??");
    			}
    			else
    			{
    				
    				ListIterator<Attribute> atit = attrs.listIterator();
    				while(atit.hasNext())
    				{
    					int k = atit.nextIndex();
    					Attribute a = atit.next();
    					
    					System.out.println(k + ". " + a);
    					
    				} // end while attributes iterator has more elements
    				
    			} // end if else attributes List is not NULL or EMPTY
    			
   
    		} // end while ints32 iterator has more elements
    	} // if elseif else (Int32 is not NULL or empty)
    	
    	
    	
    	// Int64
    	
    	if (int64s == null || int64s.isEmpty())
    	{
    		// do nothing
    	}
    	else
    	{
    		ListIterator<Int64> it = int64s.listIterator();
    		while(it.hasNext())
    		{
    			int ind = it.nextIndex();
    			Int64 var = it.next();
    			
    			System.out.println(ind + ". " + var);
    			
    			
    			// list all dims of this int32
    			List<Dim> dims = var.getDims();
    			
    			if (dims == null || dims.isEmpty())
    			{
    				// do nothing
    			}
    			else
    			{
    				
    				ListIterator<Dim> dit = dims.listIterator();
    				while(dit.hasNext())
    				{
    					int j = dit.nextIndex();
    					Dim d = dit.next();
    					
    					System.out.println(j + ". " + d);
    					
    				} // end while dim iterator has more elements
    				
    			} // end if else dims is not NULL or EMPTY
    			
    			// list atrributes of this Int64 element
    			List<Attribute> attrs = var.getAttributes();
    			
    			if (attrs == null || attrs.isEmpty())
    			{
    				System.out.println("Int 64 has not attributes..??");
    			}
    			else
    			{
    				
    				ListIterator<Attribute> atit = attrs.listIterator();
    				while(atit.hasNext())
    				{
    					int k = atit.nextIndex();
    					Attribute a = atit.next();
    					
    					System.out.println(k + ". " + a);
    					
    				} // end while attributes iterator has more elements
    				
    			} // end if else attributes List is not NULL or EMPTY
    			
   
    		} // end while ints64 iterator has more elements
    	} // if else-if else (Int64 is not NULL or empty)
    	
    	
    	/////////////////////////////////////////////////////////////////
    	// echo "container" attributes and, yes, attributes of attributes 
    	//  these attributes and *their* inner attributes (yes they are nested)
    	//  will later need to be sniffed for exceptions before marshalling WCS
    	// per Nathan's heauristic
    	
    	List<ContainerAttribute> containerAttributes = dataset.getAttributes();
    	
    	if (containerAttributes == null)
    	{
    		System.out.println ("Container Attribute List NULL");
    	}
    	else if (containerAttributes.size() == 0)
    	{
    		System.out.println("Container Attribute list EMPTY");
    	}
    	else
    	{
    		ListIterator<ContainerAttribute> it = containerAttributes.listIterator();
    		// test for conventions
			boolean foundConvention = false; boolean cfCompliant = false;
    		while (it.hasNext()) {
    			
    			boolean foundGlobal = false;
    			
    			int index = it.nextIndex();
    			ContainerAttribute containerAttribute = it.next();
    			
    			System.out.println(index + " " + containerAttribute);
    			
    			String ca_name = containerAttribute.getName();
    			if (ca_name == null || ca_name.trim().length() == 0)
    			{
    			   // do nothing
    			   System.out.println("Container attribute name NULL or empty");
    			}
    			else if (ca_name.contains("convention"))
    			{
    			  System.out.println("Found container attribute named convention(s)");	
    			  foundConvention = true;
    			}  // this will find plural conventions 
    			else if (ca_name.endsWith("_GLOBAL") || ca_name.equals("DODS_EXTRA"))
    			{
    			  System.out.println("Found container attribute name ending in _GLOBAL or DODS_EXTRA");	
    			  System.out.println("Looking for conventions...attribute");	
    			  
    			  foundGlobal = true;
    			}
    			
    			// now enumerate all attributes of the "container" attribute
    			
    			
    			List<Attribute> attributes = containerAttribute.getAttributes();
    			
    			if (attributes == null)
    			{
    		  		System.out.println (">> Attribute List NULL");
    	    	}
    	    	else if (attributes.size() == 0)
    	    	{
    	    		System.out.println(">> Attribute list EMPTY");
    	    	}   			
    	    	else
    	    	{
    	    		ListIterator<Attribute> attr_iterator = attributes.listIterator();
    	    		
    	    		
    	    		while (attr_iterator.hasNext())
    	    		{
    	    		
    	    		  Attribute a = attr_iterator.next(); int ai = attr_iterator.nextIndex();
    	    		  System.out.println(index + "." + ai + " " + a);
    	    		  
    	    		  if(foundGlobal)
    	    		  {
    	    			  // test for conventions
    	    			  
    	    			  String a_name = a.getName();
    	    			  
    	    			  if (a_name == null || a_name.trim().length() == 0)
    	    			  {
    	    				  // no action
    	    				  System.out.println("Attribute has no name??");
    	    			  }
    	    			  else if (a_name.toLowerCase().contains("convention"))
    	    			  {
    	    				  foundConvention = true;
    	    				  
    	    				  String a_value = a.getValue();
    	    				  
    	    				  System.out.println("Found attribute named convention(s), value = " + a_value);
    	    				  
    	    				  if (a_value.contains("CF-"))
    	    				  {
    	    					  cfCompliant = true;
    	    					  System.out.println("Dataset is CF Compliant!!");
    	    				  }
    	    				  
    	    			  }
    	    			  
    	    		  }  // end Found Global, now look at its attributes
    	    		  
    	    		} // end while attribute iterator has next
    	    		
    	    	} // end if - else if - else (attributes List is NOT NULL or EMPTY)
    			
    		} // end while (containerAttributes has elements) LOOP 
    		
    		
    		if (foundConvention) 
    		{
    			if (cfCompliant) 
    			{
    				// already announced success
    			}
    			else
    			{
    				System.out.println("Found GLOBAL Convention but may not be CF compliant...ERROR");
    			}
    		}
    		else
    		{
    			System.out.println("No conventions found...ERROR");
    		}
    		
    		
    	}  // end if - else if - else (CONTAINER attributes List is NOT NULL or EMPTY)
    	
		
	} // end if (dataset == null) else { // do everything
	
  } // end main

} // end class 
