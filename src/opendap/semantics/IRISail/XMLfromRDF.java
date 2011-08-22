/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2011 OPeNDAP, Inc.
//
// Authors:
//     Haibo Liu  <haibo@iri.columbia.edu>
//     Nathan David Potter  <ndp@opendap.org>
//     M. Benno Blumenthal <benno@iri.columbia.edu>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.semantics.IRISail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create a JDOM document by querying against Sesame-OWLIM RDF store.
 * The <code>rootElementStr</code>
 * is the outer wrapper of the document. The <code>topURI</code> is the top element to retrieve
 * from the repository.
 *
 */
public class XMLfromRDF {
	private RepositoryConnection con;
	private Document doc;
	private Element root;
	private String queryString0;
	private Logger log;

    /**
     * Constructor, create the top query and the outer wrapper of the document. 
     * @param con-connection to the repository.
     * @param rootElementStr-the outer wrapper of the document.
     * @param topURI-the top element in the document.
     */
	public XMLfromRDF(RepositoryConnection con, String rootElementStr, String topURI) throws InterruptedException {
		log = LoggerFactory.getLogger(getClass());
		//URI uri = new URIImpl(topURI);
		int pl = topURI.lastIndexOf("#");
		String ns;
		if(pl > 0){
		ns = topURI.substring(0,pl);
		}else{
		    pl = topURI.lastIndexOf("/");
		    ns = topURI.substring(0,pl);
		}
		root = new Element(rootElementStr,ns);
		doc = new Document(root);
		this.con = con;

		queryString0 = "SELECT DISTINCT topprop:, obj, valueclass, targetns "+
		"FROM "+
		"{containerclass} rdfs:subClassOf {} owl:onProperty {topprop:}; owl:allValuesFrom {valueclass}, "+
		"{subject} topprop: {obj} rdf:type {valueclass}, "+
		"[{topprop:} rdfs:isDefinedBy {} xsd:targetNamespace {targetns}] " +
		"using namespace "+
        "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
        "owl = <http://www.w3.org/2002/07/owl#>, "+
        "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
        "rdfs = <http://www.w3.org/2000/01/rdf-schema#>, " +
        "topprop = <"+topURI+">";
	}
    /**
     * Constructor, sets the repository connection. Query string will be passed in through
     * getXMLfromRDF(String, String). 
     * @param con-connection to the repository.
     * 
     */
    public XMLfromRDF(RepositoryConnection con) {
        log = LoggerFactory.getLogger(getClass());
        
        this.con = con;
        
    }	
    /**
     * Start the process of building the document. First level children are retrieved through
     * query zero and added to the document.
     *
      * @param topURI-searching phrase for the first level children
     */

	public void getXMLfromRDF(String topURI, String serfqString0, ValueFactory f )  throws InterruptedException{
	    
        TupleQueryResult result0 = null;
        try{
            log.debug("queryString0: " +serfqString0);
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, serfqString0);
            
            result0 = tupleQuery.evaluate();

            while ( result0.hasNext()) {
                BindingSet bindingSet = (BindingSet) result0.next();

                if (bindingSet.getValue("element") != null 
                        && bindingSet.getValue("uri") != null 
                        && bindingSet.getValue("class") != null){
                    
                    Value valueOfElement = (Value) bindingSet.getValue("element");
                    Value valueOfUri = (Value) bindingSet.getValue("uri");
                    
                    Value valueOfClass = (Value) bindingSet.getValue("class");
                   
                    String queryString1 = createQueryString(valueOfUri.stringValue(), valueOfClass);
                    String parent,ns;
                    log.debug("queryString1: " +queryString1);                                    
                    if (topURI.lastIndexOf("#") >= 0){
                                
                        int pl = topURI.lastIndexOf("#");
                        ns = topURI.substring(0,pl);
                        parent = topURI.substring(pl+1);

                    }else if(topURI.lastIndexOf("/") >= 0){
                        int pl = topURI.lastIndexOf("/");
                        ns = topURI.substring(0,pl);
                        parent = topURI.substring(pl+1);

                    }else{
                        parent = topURI;
                        ns = topURI;

                    }
                    URI uri = new URIImpl(valueOfElement.toString());
                    Value targetNS = (Value) bindingSet.getValue("targetns");
                    ns = uri.getNamespace();
                    if(targetNS != null){
                        ns = targetNS.stringValue();
                    }
                    root = new Element(uri.getLocalName(),ns);
                    doc = new Document(root);
                    Map <String,String> docAttributes = new HashMap<String,String>();
                    String type = "text/xsl";
                    String href = null;
                    //string of URL of the stylesheet of topURI (string/URL)
                    href=RepositoryOps.getUrlForStyleSheet(con, f, topURI);
                    if(href != null){
                    docAttributes.put("type", type);
                    docAttributes.put("href", href);
                    ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet",docAttributes);
                    //doc.getContent().add( 0, pi );

                    doc.addContent(0, pi);
                    }
                    this.addChildren(queryString1, root, con,doc);
                } //if (bindingSet.getValue("topnameprop") 
            } //while ( result0.hasNext())
        }catch ( QueryEvaluationException e){
            log.error(e.getMessage());
        }catch (RepositoryException e){
            log.error(e.getMessage());
        }catch (MalformedQueryException e) {
            log.error(e.getMessage());
        }finally{
            if(result0!=null){
                try {
                    result0.close();
                } catch (Exception e) {
                    log.error("Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());

                }
            }
        }
    }


	
    /**
     * Start the process of building the document. First level children are retrieved through
     * query zero and added to the document.
     *
      * @param topURI-searching phrase for the first level children
     */
	public void getXMLfromRDF(String topURI)  throws InterruptedException{
        TupleQueryResult result0 = null;
        try{
            log.debug("queryString0: " +queryString0);
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, queryString0);
            
            result0 = tupleQuery.evaluate();

            
            while ( result0.hasNext()) {
                BindingSet bindingSet = (BindingSet) result0.next();

                if (bindingSet.getValue("obj") != null 
                        && bindingSet.getValue("valueclass") != null){
                    
                    Value valueOfobj = (Value) bindingSet.getValue("obj");
                    Value valueOfobjtype = (Value) bindingSet.getValue("objtype");
                    Value valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
                   
                    String uritypestr;
                    
                    if (valueOfobjtype!= null){uritypestr= valueOfobjtype.stringValue();}
                    else{
                        uritypestr= "nullstring";   
                    }
                    String queryString1 = createQueryString(valueOfobj.toString(), valueOfvalueclass);
                    String parent,ns;
                    log.debug("queryString1: " +queryString1);                                    
                    if (topURI.lastIndexOf("#") >= 0){
                                
                        int pl = topURI.lastIndexOf("#");
                        ns = topURI.substring(0,pl);
                        parent = topURI.substring(pl+1);

                    }else if(topURI.lastIndexOf("/") >= 0){
                        int pl = topURI.lastIndexOf("/");
                        ns = topURI.substring(0,pl);
                        parent = topURI.substring(pl+1);

                    }else{
                        parent = topURI;
                        ns = topURI;

                    }
                    Value targetNS = (Value) bindingSet.getValue("targetns");
                    if(targetNS != null){
                        ns = targetNS.stringValue();
                    }
                    
                    Element chd1 = new Element(parent,ns); //first level children
                    
                    root.addContent(chd1);
                    this.addChildren(queryString1, chd1, con,doc);
                } //if (bindingSet.getValue("topnameprop") 
            } //while ( result0.hasNext())
        }catch ( QueryEvaluationException e){
            log.error(e.getMessage());
        }catch (RepositoryException e){
            log.error(e.getMessage());
        }catch (MalformedQueryException e) {
            log.error(e.getMessage());
        }finally{
            if(result0!=null){
                try {
                    result0.close();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively retrieve children and add to the document.
     *
     * @param qString-query string for retrieving children
     * @param prt-parent
     * @param con-connection to the repository
     * @param doc-the document to build
     */
	private void addChildren(String qString, Element prt, RepositoryConnection con, Document doc)  throws InterruptedException{
		TupleQueryResult result = null;
		boolean objisURI = false; //true if ojb is a URI/URL
		
		try{
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);
							
			result = tupleQuery.evaluate();

			SortedMap<String,BindingSet >   mapOrderObj   =   new TreeMap<String, BindingSet>();
			
			while ( result.hasNext()) {
				BindingSet bindingSet = (BindingSet) result.next();

				Value valueOfnameprop;
				Value valueOfobj;
				Value valueOfvalueclass;
				Value valueOforder;
				Value valueOfobjtype;
				Value valueOfform;
				if (bindingSet.getValue("nameprop") != null && bindingSet.getValue("obj") != null 
						&& bindingSet.getValue("valueclass") != null){
					valueOfnameprop = (Value) bindingSet.getValue("nameprop");
					valueOfobj = (Value) bindingSet.getValue("obj");
					valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
					valueOforder = (Value) bindingSet.getValue("order1");
					valueOfobjtype = (Value) bindingSet.getValue("objtype");
					valueOfform = (Value) bindingSet.getValue("form");
				}else{
					valueOfnameprop = (Value) bindingSet.getValue("prop");
					valueOfobj = (Value) bindingSet.getValue("obj");
					valueOfvalueclass = (Value) bindingSet.getValue("rangeclass");
					valueOforder = (Value) bindingSet.getValue("order1");
					valueOfobjtype = (Value) bindingSet.getValue("objtype");
					valueOfform = (Value) bindingSet.getValue("form");
				}

					String formtypestr = valueOfform.stringValue();
					URI formtype = new URIImpl(formtypestr);
					if(valueOfobjtype != null){ //have type description (element,attribute ...)
						String uritypestr = valueOfobjtype.stringValue();

						String parent,ns;
						
						if (valueOfnameprop.toString().lastIndexOf("#") >= 0){
							
							int pl = valueOfnameprop.toString().lastIndexOf("#");

							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);

						}else if(valueOfnameprop.toString().lastIndexOf("/") >= 0){
							int pl = valueOfnameprop.toString().lastIndexOf("/");
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);

						}else{
							parent = valueOfnameprop.toString();
							ns = valueOfnameprop.toString();

						}
						Value targetNS = (Value) bindingSet.getValue("targetns");
	                    if(targetNS != null){
	                        ns = targetNS.stringValue();
	                    }						
						URI uritype = new URIImpl(uritypestr); 
						

						if(uritype.getLocalName().equalsIgnoreCase("attribute")){
							URI urinameprop= new URIImpl(valueOfnameprop.stringValue());
							if(formtype.getLocalName().equalsIgnoreCase("qualified")){
							
							Namespace attributeNS = Namespace.getNamespace("attributeNS",urinameprop.getNamespace());

							prt.setAttribute(urinameprop.getLocalName(),valueOfobj.stringValue(),attributeNS);
							}
							else{
								prt.setAttribute(urinameprop.getLocalName(),valueOfobj.stringValue());	
							}

						}else if(uritype.getLocalName().equalsIgnoreCase("simpleContent")){

							prt.setText(valueOfobj.stringValue());
						}
						else{

							Element chd; 
							if (valueOforder != null){//order matters

								String mapkeydigit = null;
                                if (valueOforder.stringValue().length() == 1) mapkeydigit = "00" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 2) mapkeydigit = "0" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 3) mapkeydigit = valueOforder.stringValue();

                                String mapkey = mapkeydigit+"-"+valueOfnameprop.stringValue()+valueOfobj.stringValue(); //key=0001-http

								mapOrderObj.put(mapkey,bindingSet);	
							}else{//order does not matter
								if(formtype.getLocalName().equalsIgnoreCase("unqualified")){
								chd = new Element(parent);
								}else{
									chd = new Element(parent,ns);
								}
								String objURI = valueOfobj.toString().substring(0, 1);
								
								if (objURI.equalsIgnoreCase("\"")) //literal
								{
									chd.setText(valueOfobj.stringValue());
								}
								else{	
									String queryStringc = createQueryString(valueOfobj.toString(), valueOfvalueclass);
									addChildren(queryStringc, chd, con,doc);
								}//if (obj3isURI/bnode)
								
								prt.addContent(chd);
								
							}
						}
						
					}else{ //no type description (element, attribute ...)
						String parent,ns;
						String uritypestr = "nullstring";
						if (valueOfnameprop.toString().lastIndexOf("#") >= 0){
							
							int pl = valueOfnameprop.toString().lastIndexOf("#");

							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);

						}else if(valueOfnameprop.toString().lastIndexOf("/") >= 0){
							int pl = valueOfnameprop.toString().lastIndexOf("/");
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);

						}else{
							parent = valueOfnameprop.toString();
							ns = valueOfnameprop.toString();

						}
						Element chd;
						if(formtype.getLocalName().equalsIgnoreCase("unqualified")){
							chd = new Element(parent);
							}else{
								chd = new Element(parent,ns);
							}
						prt.addContent(chd);
						
						String queryStringc = createQueryString(valueOfobj.toString(), valueOfvalueclass);
						String objURI = valueOfobj.toString().substring(0, 1);
						if(objURI.equalsIgnoreCase("\""))
						{
							chd.setText(valueOfobj.stringValue());
						}
						else{
							objisURI = true;	

						}
						if (objisURI){
							addChildren(queryStringc, chd, con,doc);
						}
					}
			} //while ( result4.hasNext())
			
			Iterator<String> iterator = mapOrderObj.keySet().iterator();
						
			while (iterator.hasNext()) {
		      Object key = iterator.next();
		      BindingSet bindingSet = mapOrderObj.get(key);
				
				Value valueOfnameprop;
				Value valueOfobj;
				Value valueOfvalueclass;
				Value valueOforder;
				Value valueOfobjtype;
				Value valueOfform;
				if (bindingSet.getValue("nameprop") != null && bindingSet.getValue("obj") != null 
						&& bindingSet.getValue("valueclass") != null){
					valueOfnameprop = (Value) bindingSet.getValue("nameprop");
					valueOfobj = (Value) bindingSet.getValue("obj");
					valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
					valueOforder = (Value) bindingSet.getValue("order1");
					valueOfobjtype = (Value) bindingSet.getValue("objtype");
					valueOfform = (Value) bindingSet.getValue("form");
				}else{
					valueOfnameprop = (Value) bindingSet.getValue("prop");
					valueOfobj = (Value) bindingSet.getValue("obj");
					valueOfvalueclass = (Value) bindingSet.getValue("rangeclass");
					valueOforder = (Value) bindingSet.getValue("order1");
					valueOfobjtype = (Value) bindingSet.getValue("objtype");
					valueOfform = (Value) bindingSet.getValue("form");
				}
					
				String parent,ns;
				
				if (valueOfnameprop.toString().lastIndexOf("#") >= 0){
					int pl = valueOfnameprop.toString().lastIndexOf("#");
					ns = valueOfnameprop.toString().substring(0,pl);
					parent = valueOfnameprop.toString().substring(pl+1);
				}else if(valueOfnameprop.toString().lastIndexOf("/") >= 0){
					int pl = valueOfnameprop.toString().lastIndexOf("/");
					ns = valueOfnameprop.toString().substring(0,pl);
					parent = valueOfnameprop.toString().substring(pl+1);
				}else{
					parent = valueOfnameprop.toString();
					ns = valueOfnameprop.toString();
				}
					
				Value targetNS = (Value) bindingSet.getValue("targetns");
                if(targetNS != null){
                    ns = targetNS.stringValue();
                }
				Element chd;
				String formtypestr = valueOfform.stringValue();
				URI formtype = new URIImpl(formtypestr);
				if(formtype.getLocalName().equalsIgnoreCase("unqualified")){
					chd = new Element(parent);
					}else{
						chd = new Element(parent,ns);
					}					
				if(valueOfobjtype.stringValue().equalsIgnoreCase("attribute")){
					prt.setAttribute(valueOfnameprop.stringValue(),valueOfobjtype.stringValue());
				}
				
				String objURI = valueOfobj.toString().substring(0, 1);
				
				if (objURI.equalsIgnoreCase("\""))
				{
					chd.setText(valueOfobj.stringValue());
					chd.getText();
					
				}
				else{	
								
					String queryStringc = createQueryString(valueOfobj.toString(), valueOfvalueclass);
					addChildren(queryStringc, chd, con,doc);
				} //if (obj3isURI)
				prt.addContent(chd);
			} //for   (int   i   =   0;   i   <   key.length;
					 
		}catch ( QueryEvaluationException e){
			log.error(e.getMessage());
		}catch (RepositoryException e){
			log.error(e.getMessage());
		}catch (MalformedQueryException e) {
			log.error(e.getMessage());
		} finally {
            if(result!=null){
                try {
                    result.close();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
		}
		
		
	}//void addChildren

    /**
     * Create the SeRQLquery string using the parent (URI) and the parent class (URI).
     * @param parentstr
     * @param parentclassstr
     * @return  a SeRQL query string
     */
	private String createQueryString(String parentstr, Value parentclassstr)  throws InterruptedException{
		String queryStringc;
//		String objURI = parentstr.substring(0, 7);
		
//		if (objURI.equalsIgnoreCase("http://")){ 
			queryStringc = "SELECT DISTINCT nameprop, obj, valueclass, order1, objtype, form, targetns "+
			"FROM "+
			"{parent:} nameprop {obj}, "+
			"{parentclass:} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}; "+
			"owl:allValuesFrom {valueclass}, "+
			"{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; "+
			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, "+
			"{parentclass:} rdfs:subClassOf {} subprop {restriction}, "+
			"[{parentclass:} xsd2owl:uses {nameprop},{{parentclass:} xsd2owl:uses {nameprop}} "+
			"xsd2owl:useCount {order1}], "+
			"[{nameprop} rdfs:isDefinedBy {} xsd:targetNamespace {targetns}] " +
			"using namespace "+
			      "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
			      "owl = <http://www.w3.org/2002/07/owl#>, "+
			      "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
			      "rdfs = <http://www.w3.org/2000/01/rdf-schema#>, "+
			      "parent = <" + parentstr + ">," +
			      "parentclass = <"+ parentclassstr + ">";     
					
//		}
//		else{
//			queryStringc = "SELECT DISTINCT nameprop, obj, valueclass, order1, objtype, form, targetns "+
//			"FROM "+
//			"{" + parentstr + "} nameprop {obj}, "+
//			"{parentclass:} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}; "+
//			"owl:allValuesFrom {valueclass}, "+
//			"{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; "+
//			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, "+
//			"{parentclass:} rdfs:subClassOf {} subprop {restriction}, "+
//			"[{parentclass:} xsd2owl:uses {nameprop},{{parentclass:} xsd2owl:uses {nameprop}} "+
//			"xsd2owl:useCount {order1}], "+
//			"[{nameprop} rdfs:isDefinedBy {} xsd:targetNamespace {targetns}] " +
//			"using namespace "+
//			      "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
//			      "owl = <http://www.w3.org/2002/07/owl#>, "+
//			      "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
//			      "rdfs = <http://www.w3.org/2000/01/rdf-schema#>, " +
//			      "parentclass = <"+ parentclassstr + ">"; 
			
//		}
		return queryStringc;
	}

	public Document getDoc(){
		return this.doc;
	}
	public Element getRootElement(){
		return this.doc.getRootElement();
	}
}
