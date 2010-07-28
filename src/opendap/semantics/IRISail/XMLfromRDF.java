/**
 * 
 */
package opendap.semantics.IRISail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
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
 * Create JDOM document by querying against Sesame2 RDF store.
 * @author Haibo Liu, haibo@iri.columbia.edu
 * @version 1.0
 */
public class XMLfromRDF {
	private RepositoryConnection con;
	private Document doc;
	private Element root;
	private String queryString0;
	private Logger log;
/**
 * Constructor, initializes doc, creates top query.
 */
	public XMLfromRDF(RepositoryConnection con, String rootElementStr, String topURI) {
		this.log = LoggerFactory.getLogger(getClass());
		URI uri = new URIImpl(topURI);
		int pl = topURI.lastIndexOf("#");
		String ns = topURI.substring(0,pl);
		Namespace topURINS = Namespace.getNamespace(ns);
		this.root = new Element(rootElementStr,topURINS);
		this.doc = new Document(root);
		this.con = con;
		/*this.queryString0 = "SELECT DISTINCT topnameprop, obj, valueclass "+
		"FROM "+
		"{container:} rdfs:range {containerclass} rdfs:subClassOf {} owl:onProperty {topprop:}; owl:allValuesFrom {valueclass}, "+ 
		"{subject} topnameprop {obj} rdf:type {valueclass} "+
		"WHERE topnameprop=topprop:" +
		"using namespace "+
		"xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
		"owl = <http://www.w3.org/2002/07/owl#>, "+
		"xsd = <http://www.w3.org/2001/XMLSchema#>, "+
		"container = <http://www.opengis.net/wcs/1.1#CoverageDescriptions>, "+
		"topprop = <"+topURI+">";
		*/
		/*this.queryString0 = "SELECT DISTINCT topnameprop, obj, valueclass "+
        "FROM "+
        "{containerclass} rdfs:subClassOf {} owl:onProperty {topprop:}; owl:allValuesFrom {valueclass}, "+ 
        "{subject} topnameprop {obj} rdf:type {valueclass} "+
        "WHERE topnameprop = topprop: " +
        "using namespace "+
        "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
        "owl = <http://www.w3.org/2002/07/owl#>, "+
        "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
        "topprop = <"+topURI+">";
		*/
		this.queryString0 = "SELECT DISTINCT topprop:, obj, valueclass "+
		"FROM "+
		"{containerclass} rdfs:subClassOf {} owl:onProperty {topprop:}; owl:allValuesFrom {valueclass}, "+
		"{subject} topprop: {obj} rdf:type {valueclass} "+
		"using namespace "+
        "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
        "owl = <http://www.w3.org/2002/07/owl#>, "+
        "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
        "topprop = <"+topURI+">";
	}
	public void getXMLfromRDF(){
		TupleQueryResult result0 = null;
		try{
		    log.debug("queryString0: " +queryString0);
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, queryString0);
			
			result0 = tupleQuery.evaluate();
			//log.debug("Qresult: "+result0.hasNext());
			List<String> bindingNames = result0.getBindingNames();
			//log.debug(bindingNames.probeServletContext());
			while ( result0.hasNext()) {
				BindingSet bindingSet = (BindingSet) result0.next();
				//log.debug(bindingSet.probeServletContext());
										
				if (bindingSet.getValue("topnameprop") !=null && bindingSet.getValue("obj") != null 
						&& bindingSet.getValue("valueclass") != null){
					Value valueOftopnameprop = (Value) bindingSet.getValue("topnameprop");
					Value valueOfobj = (Value) bindingSet.getValue("obj");
					Value valueOfobjtype = (Value) bindingSet.getValue("objtype");
					//log.debug("topprop:");
					//log.debug(valueOftopnameprop.stringValue());
					//log.debug("obj:");
					//log.debug(valueOfobj.stringValue());
					Value valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
					//log.debug("valueclass:");
					//log.debug(valueOfvalueclass.stringValue());
					
					String uritypestr;
					if (valueOfobjtype!= null){uritypestr= valueOfobjtype.stringValue();}
					else{
						uritypestr= "nullstring";	
					}
					String queryString1 = createQueryString(valueOfobj.toString(), valueOfvalueclass);
					String parent,ns;
														
					if (valueOftopnameprop.toString().lastIndexOf("#") >= 0){
								
						int pl = valueOftopnameprop.toString().lastIndexOf("#");
						ns = valueOftopnameprop.toString().substring(0,pl);
						parent = valueOftopnameprop.toString().substring(pl+1);
						//log.debug("H# "+parent);
					}else if(valueOftopnameprop.toString().lastIndexOf("/") >= 0){
						int pl = valueOftopnameprop.toString().lastIndexOf("/");
						ns = valueOftopnameprop.toString().substring(0,pl);
						parent = valueOftopnameprop.toString().substring(pl+1);
						//log.debug("H/ "+parent);
					}else{
						parent = valueOftopnameprop.toString();
						ns = valueOftopnameprop.toString();
						//log.debug("H "+parent);
					}
								
					Element chd1 = new Element(parent,ns); //duplicated as the root
					
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
                    e.printStackTrace();
                }
            }
		}
	}
	
	
	public void getXMLfromRDF(String topURI){
        TupleQueryResult result0 = null;
        try{
            log.debug("queryString0: " +queryString0);
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, queryString0);
            
            result0 = tupleQuery.evaluate();
            //log.debug("Qresult: "+result0.hasNext());
            List<String> bindingNames = result0.getBindingNames();
            //log.debug(bindingNames.probeServletContext());
            while ( result0.hasNext()) {
                BindingSet bindingSet = (BindingSet) result0.next();
                //log.debug(bindingSet.probeServletContext());
                                        
                if (bindingSet.getValue("obj") != null 
                        && bindingSet.getValue("valueclass") != null){
                    
                    Value valueOfobj = (Value) bindingSet.getValue("obj");
                    Value valueOfobjtype = (Value) bindingSet.getValue("objtype");
                    //log.debug("topprop:");
                    //log.debug(topURI);
                    //log.debug("obj:");
                    //log.debug(valueOfobj.stringValue());
                    Value valueOfvalueclass = (Value) bindingSet.getValue("valueclass");
                    //log.debug("valueclass:");
                    //log.debug(valueOfvalueclass.stringValue());
                    
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
                        //log.debug("H# "+parent);
                    }else if(topURI.lastIndexOf("/") >= 0){
                        int pl = topURI.lastIndexOf("/");
                        ns = topURI.substring(0,pl);
                        parent = topURI.substring(pl+1);
                        //log.debug("H/ "+parent);
                    }else{
                        parent = topURI;
                        ns = topURI;
                        //log.debug("H "+parent);
                    }
                                
                    Element chd1 = new Element(parent,ns); //duplicated as the root
                    //chd1.setText(valueOfobj.probeServletContext());
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
                    e.printStackTrace();
                }
            }
        }
    }
	//private void addChildren(String qString, Element prt, String parentObjTypestr, RepositoryConnection con, Document doc){
	private void addChildren(String qString, Element prt, RepositoryConnection con, Document doc){
		TupleQueryResult result = null;
		boolean objisURI = false; //true if ojb is a URI/URL
		//log.debug("Sesame2Builder "+qString);
		
		try{
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);
							
			result = tupleQuery.evaluate();
			//log.debug("Qresult: "+result.hasNext());
			List<String> bindingNames = result.getBindingNames();
			//log.debug(bindingNames.probeServletContext());
			
			SortedMap<String,BindingSet >   mapOrderObj   =   new TreeMap<String, BindingSet>();
			
			while ( result.hasNext()) {
				BindingSet bindingSet = (BindingSet) result.next();
				//log.debug("In try: "+bindingSet.probeServletContext());
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
					/*
					if(valueOforder != null){
						log.debug(valueOforder.stringValue());
					}else{
						log.debug("NULL");
					}
					*/
					String formtypestr = valueOfform.stringValue();
					URI formtype = new URIImpl(formtypestr);
					if(valueOfobjtype != null){ //have type description (element,attribute ...)
						String uritypestr = valueOfobjtype.stringValue();
						//log.debug("valueOfobjtype= "+valueOfobjtype.stringValue());
						String parent,ns;
						
						if (valueOfnameprop.toString().lastIndexOf("#") >= 0){
							
							int pl = valueOfnameprop.toString().lastIndexOf("#");
							//log.debug("H4valueOfobjtype!=null valueOfnameprop3= "+valueOfnameprop.probeServletContext());
							//log.debug("H4valueOfobjtype!=null pl= "+pl);
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);
							//log.debug("H4#valueOfobjtype!=null "+parent);
						}else if(valueOfnameprop.toString().lastIndexOf("/") >= 0){
							int pl = valueOfnameprop.toString().lastIndexOf("/");
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);
							//log.debug("H4/valueOfobjtype!=null "+parent);
						}else{
							parent = valueOfnameprop.toString();
							ns = valueOfnameprop.toString();
							//log.debug("H4valueOfobjtype!=null "+parent);
						}
												
						URI uritype = new URIImpl(uritypestr); 
						
						//log.debug(formtypestr);
						if(uritype.getLocalName().equalsIgnoreCase("attribute")){
							URI urinameprop= new URIImpl(valueOfnameprop.stringValue());
							if(formtype.getLocalName().equalsIgnoreCase("qualified")){
							
							Namespace attributeNS = Namespace.getNamespace("attributeNS",urinameprop.getNamespace());
							//log.debug(formtype.probeServletContext());
							
							prt.setAttribute(urinameprop.getLocalName(),valueOfobj.stringValue(),attributeNS);
							}
							else{
								prt.setAttribute(urinameprop.getLocalName(),valueOfobj.stringValue());	
							}
							//log.debug("IN attribute");
						}else if(uritype.getLocalName().equalsIgnoreCase("simpleContent")){
							//log.debug("In simpleContent: ");
							prt.setText(valueOfobj.stringValue());
						}
						else{
							//log.debug("In element: ");
							//log.debug(valueOfobjtype.stringValue());
							Element chd; 
							if (valueOforder != null){//order matters
								//String mapkey = valueOforder.stringValue()+"-"+valueOfobj.probeServletContext(); //key=0001-http
								
								String mapkeydigit = null;
                                if (valueOforder.stringValue().length() == 1) mapkeydigit = "00" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 2) mapkeydigit = "0" +valueOforder.stringValue();
                                if (valueOforder.stringValue().length() == 3) mapkeydigit = valueOforder.stringValue();
                                //String mapkey = valueOforder.stringValue()+"-"+valueOfobj.stringValue(); //key=0001-http
                                String mapkey = mapkeydigit+"-"+valueOfnameprop.stringValue()+valueOfobj.stringValue(); //key=0001-http
								//log.debug("mapkey= "+mapkey);
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
							//log.debug("H4valueOfobjtype=null valueOfnameprop3= "+valueOfnameprop.probeServletContext());
							//log.debug("H4valueOfobjtype=null pl= "+pl);
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);
							//log.debug("H4#valueOfobjtype=null "+parent);
						}else if(valueOfnameprop.toString().lastIndexOf("/") >= 0){
							int pl = valueOfnameprop.toString().lastIndexOf("/");
							ns = valueOfnameprop.toString().substring(0,pl);
							parent = valueOfnameprop.toString().substring(pl+1);
							//log.debug("H4/valueOfobjtype=null "+parent);
						}else{
							parent = valueOfnameprop.toString();
							ns = valueOfnameprop.toString();
							//log.debug("H4valueOfobjtype=null "+parent);
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
							//log.debug("objisURI ? "+objisURI);
						}
						if (objisURI){
							addChildren(queryStringc, chd, con,doc);
						}
					}
			} //while ( result4.hasNext())
			
			Iterator<String> iterator = mapOrderObj.keySet().iterator();
						
			while (iterator.hasNext()) {
		      Object key = iterator.next();
		      BindingSet bindingSet = (BindingSet) mapOrderObj.get(key);
				
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
            if (result != null) {
                try {

                    result.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
		}
		
		
	}//void addChildren
	private String createQueryString(String parentstr, Value parentclassstr){
		String queryStringc;
		String objURI = parentstr.substring(0, 7);
		
		if (objURI.equalsIgnoreCase("http://")){
			queryStringc = "SELECT DISTINCT nameprop, obj, valueclass, order1, objtype, form "+
			"FROM "+
			"{parent:} nameprop {obj}, "+
			"{parentclass:} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}; "+
			"owl:allValuesFrom {valueclass}, "+
			"{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; "+
			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, "+
			"{parentclass:} rdfs:subClassOf {} subprop {restriction}, "+
			"[{parentclass:} xsd2owl:uses {nameprop},{{parentclass:} xsd2owl:uses {nameprop}} "+
			"xsd2owl:useCount {order1}] "+
			"using namespace "+
			      "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
			      "owl = <http://www.w3.org/2002/07/owl#>, "+
			      "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
			      "parent = <" + parentstr + ">," +
			      "parentclass = <"+ parentclassstr + ">";     
					
		}
		else{
			queryStringc = "SELECT DISTINCT nameprop, obj, valueclass, order1, objtype, form "+
			"FROM "+
			"{" + parentstr + "} nameprop {obj}, "+
			"{parentclass:} xsd2owl:isConstrainedBy {restriction} owl:onProperty {nameprop}; "+
			"owl:allValuesFrom {valueclass}, "+
			"{subprop} rdfs:subPropertyOf {xsd2owl:isConstrainedBy}; "+
			"xsd2owl:hasTarget {objtype}; xsd2owl:hasTargetForm {form}, "+
			"{parentclass:} rdfs:subClassOf {} subprop {restriction}, "+
			"[{parentclass:} xsd2owl:uses {nameprop},{{parentclass:} xsd2owl:uses {nameprop}} "+
			"xsd2owl:useCount {order1}] "+
			"using namespace "+
			      "xsd2owl = <http://iridl.ldeo.columbia.edu/ontologies/xsd2owl.owl#>, "+
			      "owl = <http://www.w3.org/2002/07/owl#>, "+
			      "xsd = <http://www.w3.org/2001/XMLSchema#>, "+
			      "parentclass = <"+ parentclassstr + ">"; 
			
		}
		return queryStringc;
	}
	
	public void printDoc(){
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat() );
		try {
			outputter.output(this.doc, System.out);
		} catch (IOException e1) {
			e1.printStackTrace();
		}	
	}
	public Document getDoc(){
		return this.doc;
	}
	public Element getRootElement(){
		return this.doc.getRootElement();
	}
}
