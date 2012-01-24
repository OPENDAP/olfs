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
import java.util.Locale;
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
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.util.LiteralUtil;

import org.openrdf.query.algebra.IsLiteral;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;

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
                    Value valueOfUri = bindingSet.getValue("uri");
                    log.debug("valueOfUri.stringValue " + valueOfUri.stringValue());
                    
                    Value valueOfClass = (Value) bindingSet.getValue("class");
                   
                    String queryString1 = createQueryString();
                    String parent,ns;
                    log.debug("queryStringValue1: " +queryString1);
                    // convert rdf uri to xml namespace and parent
                    parent = getParent(topURI);
                    ns = getNs(topURI);
                    
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
                    this.addChildren(queryString1, root, con,doc, valueOfUri, valueOfClass);
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
                    
                    if (valueOfobjtype!= null){
                    	uritypestr= valueOfobjtype.stringValue();}
                    else{
                        uritypestr= "nullstring";   
                    }
                                        
          		    String queryString1 = createQueryString();
                    String parent,ns;

                    // convert rdf uri to xml namespace and parent
                    parent = getParent(topURI);
                    ns = getNs(topURI);
                    
                    Value targetNS = (Value) bindingSet.getValue("targetns");
                    if(targetNS != null){
                        ns = targetNS.stringValue();
                    }
                    
                    Element chd1 = new Element(parent,ns); //first level children
                    
                    root.addContent(chd1);
                    this.addChildren(queryString1, chd1, con,doc, valueOfobj, valueOfvalueclass);
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
     * @param Parent-parent
     * @param con-connection to the repository
     * @param doc-the document to build
     */
	private void addChildren(String qString, Element prt, RepositoryConnection con, Document doc, Value Parent, Value Parentclass)  throws InterruptedException{

		TupleQueryResult result = null;
		
		try{
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);
			
			MapBindingSet bSet = new MapBindingSet();
			bSet.addBinding("parent",Parent);
			bSet.addBinding("parentclass",Parentclass);
			tupleQuery.setBinding("p", bSet.getValue("parent"));				
			tupleQuery.setBinding("pc", bSet.getValue("parentclass"));
			
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
				
			    valueOfnameprop = (Value) bindingSet.getValue("nameprop");
				valueOfobj = (Value) bindingSet.getValue("obj");
				valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
				valueOforder = (Value) bindingSet.getValue("order1");
				valueOfobjtype = (Value) bindingSet.getValue("objtype");
				valueOfform = (Value) bindingSet.getValue("form");
				
				if (bindingSet.getValue("valueclass") != null){
					valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
				}
					String formtypestr = valueOfform.stringValue();
					URI formtype = new URIImpl(formtypestr);
					if(valueOfobjtype != null){ //have type description (element,attribute ...)
						String uritypestr = valueOfobjtype.stringValue();

						String parent,ns;

						// convert rdf uri to xml namespace and parent
	                    parent = getParent(valueOfnameprop.toString());
	                    ns = getNs(valueOfnameprop.toString());
	                    
						Value targetNS = (Value) bindingSet.getValue("targetns");
	                    if(targetNS != null){
	                        ns = targetNS.stringValue();
	                    }						
						URI uritype = new URIImpl(uritypestr);
						

					    if(uritype.getLocalName().equalsIgnoreCase("attribute")){
							URI urinameprop= new URIImpl(valueOfnameprop.stringValue());
							if(formtype.getLocalName().equalsIgnoreCase("qualified")){
								// convert rdf namespace to xml namespace, no #'s
			                    ns = getNs(valueOfnameprop.toString());
							    Namespace attributeNS = Namespace.getNamespace("attributeNS",ns);

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
							if (valueOforder != null){ //order matters

								String mapkeydigit = null;
								// allow for up to 999 ordered xml elements
                                if (valueOforder.stringValue().length() == 1) mapkeydigit = "00" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 2) mapkeydigit = "0" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 3) mapkeydigit = valueOforder.stringValue();

                                String mapkey = mapkeydigit+"-"+valueOfnameprop.stringValue()+valueOfobj.stringValue(); //key=0001-http

								mapOrderObj.put(mapkey,bindingSet);	
							}else{ //order does not matter
								if(formtype.getLocalName().equalsIgnoreCase("unqualified")){
								chd = new Element(parent);
								}else{
									chd = new Element(parent,ns);
								}
								
								if (valueOfobj instanceof Literal) //literals do not have children
								{
									chd.setText(valueOfobj.stringValue());
								}
								else {	
									String queryStringc = createQueryString();
									addChildren(queryStringc, chd, con,doc, valueOfobj, valueOfvalueclass);
								}//if (valueOfobj)
								
								prt.addContent(chd);
								
							}
						}
						
					}else{ //no type description (element, attribute ...)
						String parent,ns;
						
	                    // convert rdf uri to xml namespace and parent
	                    parent = getParent(valueOfnameprop.toString());
	                    ns = getNs(valueOfnameprop.toString());
	                    
						Element chd;
						if(formtype.getLocalName().equalsIgnoreCase("unqualified")){
							chd = new Element(parent);
							}else{
								chd = new Element(parent,ns);
							}
						prt.addContent(chd);
						
						String queryStringc = createQueryString();

						if(valueOfobj instanceof Literal) // Literals do not have children
						{
							chd.setText(valueOfobj.stringValue());
						}
						else{
							addChildren(queryStringc, chd, con,doc, valueOfobj, valueOfvalueclass);
						}
					}
			} //while ( result.hasNext())
			
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
				
			    valueOfnameprop = (Value) bindingSet.getValue("nameprop");
				valueOfobj = (Value) bindingSet.getValue("obj");
				valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
				valueOforder = (Value) bindingSet.getValue("order1");
				valueOfobjtype = (Value) bindingSet.getValue("objtype");
				valueOfform = (Value) bindingSet.getValue("form");

				if (bindingSet.getValue("valueclass") != null){
					valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
				}
					
				String parent,ns;
				
                // convert rdf uri to xml namespace and parent
                parent = getParent(valueOfnameprop.toString());
                ns = getNs(valueOfnameprop.toString());
					
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
								
				if (valueOfobj instanceof Literal)  // Literals do not have children
				{
					Literal lit = new LiteralImpl(valueOfobj.toString());
					String lang = lit.getLanguage();
					log.info("valueOfobj strings: "  + valueOfobj.stringValue());
					chd.setText(valueOfobj.stringValue());
					chd.getText();
					
				}
				else{	
					String queryStringc = createQueryString();
					addChildren(queryStringc, chd, con,doc, valueOfobj, valueOfvalueclass);
				} //if (valueOfobj)
				
				prt.addContent(chd);
				
			} // while iterator.hasNext
					 
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
     * @return  a SeRQL query string
     */
	private String createQueryString()  throws InterruptedException{		
		// create query string for retrieving children of a parent; uses tokens p and pc for parent and parentclass
        String queryStringc;
			queryStringc = "SELECT DISTINCT nameprop, obj, valueclass, order1, objtype, form, targetns "+
			"FROM "+
			"{p} nameprop {obj}, "+
			"{pc} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}; "+
			"owl:allValuesFrom {valueclass}, "+
			"{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; "+
			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, "+
			"{pc} rdfs:subClassOf {} subprop {restriction}, "+
			"[{pc} xsd2owl:uses {nameprop},{{pc} xsd2owl:uses {nameprop}} "+
			"xsd2owl:useCount {order1}], "+
			"[{nameprop} rdfs:isDefinedBy {} xsd:targetNamespace {targetns}] " +
			"UNION " +
			"SELECT nameprop, obj, objtype, form FROM" +
			"{pc} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}," +
			"{obj} rdf:type {pc},{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; " +
			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, {pc} rdfs:subClassOf {} subprop {restriction}" +
			"WHERE (sameTerm(nameprop,rdf:about)OR sameTerm(nameprop,rdf:resource)) AND obj= p " +
			"using namespace "+
			      "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
			      "owl = <http://www.w3.org/2002/07/owl#>, "+
			      "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
			      "rdfs = <http://www.w3.org/2000/01/rdf-schema#> ";
		return queryStringc;
	}
	
	private String getParent(String topURI)  throws InterruptedException{
		// converts rdf parent to xml parent, no #'s
		String parent;
        if (topURI.lastIndexOf("#") >= 0){
            int pl = topURI.lastIndexOf("#");
            parent = topURI.substring(pl+1);
        }else if(topURI.lastIndexOf("/") >= 0){
            int pl = topURI.lastIndexOf("/");
            parent = topURI.substring(pl+1);
        }else{
        	parent = topURI;
        }
        return parent;
	}
		
	private String getNs(String topURI)  throws InterruptedException{
		// converts rdf namepspace to xml namespace
		String ns;
        if (topURI.lastIndexOf("#") >= 0){        
            int pl = topURI.lastIndexOf("#");
            ns = topURI.substring(0,pl);
        }else if(topURI.lastIndexOf("/") >= 0){
            int pl = topURI.lastIndexOf("/");
            ns = topURI.substring(0,pl);
        }else{
            ns = topURI;
        }
    return ns;
	}

	public Document getDoc(){
		return this.doc;
	}
	public Element getRootElement(){
		return this.doc.getRootElement();
	}
}
