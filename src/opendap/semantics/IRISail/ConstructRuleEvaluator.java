
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

import org.openrdf.model.*;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

 /**
  * This class is used to run external inference rules on an Sesame-OWLIM repository.
  * The inference rules are written in SeRQL.
  * The inferred statements are added into the repository. The inference rules are run
  * repeatedly until no rules generate any new statements.
  * <p/>
  * The <code>constructQuery</code> is a String Vector holds the inference rules.
  * The <code>constructContext</code> is a Hash Map holds pairs of inference rules and its ID.  
  */
public class ConstructRuleEvaluator {

    private  Logger log;


    public static enum ProcessingTypes {
        NONE, xsString, DropQuotes, RetypeTo, Increment, Function
    }

    private Vector<String> constructQuery;
    private HashMap<String, String> constructContext;

    private ProcessingTypes postProcessFlag;


    public ConstructRuleEvaluator() {
        log = LoggerFactory.getLogger(getClass());
        constructQuery = new Vector<String>();
        constructContext = new HashMap<String, String>();

    }


     /**
      * Run all Construct queries and ingest resulting statements into repository
      *
      * @param repository-repository to use
      * @throws RepositoryException
      */
     /**
      * 
      * @param repository
      * @return
      * @throws InterruptedException
      * @throws RepositoryException
      */
     public boolean  runConstruct(Repository repository) throws InterruptedException, RepositoryException {

         log.debug("-----------------------------------------------------------------");
         log.debug("------------------- Starting runConstruct() ---------------------");
         log.debug("-----------------------------------------------------------------");

         GraphQueryResult graphResult = null;
         RepositoryConnection con = null;
         Vector<Statement> Added = new Vector<Statement>();

         Boolean modelChanged = true;
         int runNbr = 0;
         int runNbrMax = 99;
         long startTime, endTime;
         startTime = new Date().getTime();

         int queryTimes = 0;
         long ruleStartTime, ruleEndTime;
         int totalStAdded = 0; // number of postprocessed statements added
         int totalStAddedIn1Pass = 0; // number of post processed statements added in 1 PASS
         int notPostProcessed = 0; // number of not postprocessed statements added
         int notPostProcessed1Pass = 0; // number of not  postprocessed statements added in one pass
         
         findConstruct(repository);

         //log.debug("Before running the construct rules:\n " +
         //opendap.coreServlet.Util.getMemoryReport());


         boolean repositoryChanged = false;


         try {
             con = repository.getConnection();
             ValueFactory creatValue = repository.getValueFactory(); //moved from line 159
             Hashtable<String, Double> ruleTimeTotal = new Hashtable<String, Double>();

             while (modelChanged && runNbr < runNbrMax ) {

                 runNbr++;
                 modelChanged = false;
                 totalStAddedIn1Pass = 0;
                 notPostProcessed1Pass = 0;
                 
                 if (runNbr == 1) {
                     log.info("runConstruct(): Total number of construct rule(s): " + this.constructQuery.size());
                 }
                 //log.debug("Applying Construct Rules. Beginning Pass #" + runNbr
                 //        + " \n" + opendap.coreServlet.Util.getMemoryReport());
                 int ruleNumber = 0;
                 for (String qstring : this.constructQuery) {
                     ruleNumber++;
                     queryTimes++;
                     ruleStartTime = new Date().getTime();
                     int stAdded = 0; // track statements added by each rule
                     int notPostProcessedAdded = 0; // track not post processed st by each rule
                     
                     Vector<Statement> toAdd = new Vector<Statement>();
                     String constructURL = this.constructContext.get(qstring);

                     //URI uriaddress = new URIImpl(constructURL);
                     URI uriaddress = new URIImpl(Terms.externalInferencingContext.getUri());
                     Resource[] context = new Resource[1];
                     context[0] = uriaddress;


                     String processedQueryString = convertSWRLQueryToSeasameQuery(qstring);
                     if (runNbr == 1) {
                         log.debug("runConstruct(): Original construct: " + qstring);
                         log.debug("runConstruct(): Processed construct: " + processedQueryString);
                     }

                     try {
                         //log.debug("Prior to making new repository connection:\n "
                         //+ opendap.coreServlet.Util.getMemoryReport());
                         log.debug("runConstruct(): Original construct rule ID: " + constructURL);
                         GraphQuery graphQuery = con.prepareGraphQuery(
                                 QueryLanguage.SERQL, processedQueryString);

                         graphResult = graphQuery.evaluate();
                         log.debug("runConstruct(): Completed querying. ");
                         
                         ProcessController.checkState();

                         if (graphResult.hasNext()) {
                             modelChanged = true;
                             repositoryChanged = repositoryChanged || modelChanged;

                             // ValueFactory creatValue = repository.getValueFactory();

                             switch (postProcessFlag) {

                                 case xsString:
                                     process_xsString(graphResult, creatValue, Added,
                                             toAdd, con, context);
                                     //log.debug("After processing xs:string:\n "
                                     //        + opendap.coreServlet.Util
                                     //                .getMemoryReport());
                                     break;

                                 case DropQuotes:
                                     process_DropQuotes(graphResult, creatValue, Added,
                                             toAdd, con, context);
                                     //log.debug("After processing DropQuotes:\n "
                                     //        + opendap.coreServlet.Util
                                     //                .getMemoryReport());
                                     break;

                                 case RetypeTo:
                                     process_RetypeTo(graphResult, creatValue, Added,
                                             toAdd, con, context);
                                     //log.debug("After processing RetypeTo:\n "
                                     //        + opendap.coreServlet.Util
                                     //                .getMemoryReport());
                                     break;

                                 case Increment:
                                     process_Increment(graphResult, creatValue, Added,
                                             toAdd, con, context);
                                     //log.debug("After processing Increment:\n "
                                     //        + opendap.coreServlet.Util
                                     //                .getMemoryReport());
                                     break;

                                 case Function:

                                     process_fn(graphResult, creatValue, Added, toAdd,
                                             con, context);// postpocessing Join,

                                     break;
                                 case NONE:
                                 default:
                                     log.debug("runConstruct(): Add statements without post-process ...");

                                     con.add(graphResult, context);
                                     notPostProcessedAdded++;
                                                                                                                
                                     break;
                             }


                             if (toAdd != null) {
                                 stAdded += toAdd.size();
                             }

                         } // if (graphResult.hasNext
                         else {
                             //log.debug("runConstruct(): The construct rule returned zero statements.");
                         }

                     } catch (QueryEvaluationException e) {
                         log.error("runConstruct(): Caught an QueryEvaluationException! Msg: " + e.getMessage());
                     } catch (RepositoryException e) {
                         log.error("runConstruct(): Caught RepositoryException! Msg: "+ e.getMessage());
                     } catch (MalformedQueryException e) {
                         log.error("runConstruct(): MalformedQuery: " + processedQueryString);
                     } finally {
                         if (graphResult != null) {
                             try {
                                 graphResult.close();
                             } catch (Exception e) {
                                     log.error("runConstruct(): Caught an " + e.getClass().getName() + " Msg: " + e.getMessage());
                             }
                         }

                     }

                     ruleEndTime = new Date().getTime();
                     double ruleTime = (ruleEndTime - ruleStartTime) / 1000.0;

                     log.debug("runConstruct(): Construct rule " + ruleNumber + " takes " + ruleTime
                             + " seconds in loop " + runNbr + " added " + stAdded+ " post processed statements and "
                             + notPostProcessedAdded+" not post processed statements streams.");

                     totalStAdded = totalStAdded + stAdded;
                     totalStAddedIn1Pass = totalStAddedIn1Pass + stAdded;
                     notPostProcessed1Pass = notPostProcessed1Pass + notPostProcessedAdded;
                     
                     if(ruleTimeTotal.get(qstring) != null){
                         Double ruleTotal = ruleTimeTotal.get(qstring) + ruleTime;
                         ruleTimeTotal.put(qstring, ruleTotal) ;
                     }else{
                         ruleTimeTotal.put(qstring, ruleTime) ;
                     }
                 } // for(String qstring
                 notPostProcessed = notPostProcessed + notPostProcessed1Pass;
                 log.info("runConstruct(): Completed pass " + runNbr + "  " +
                         "Queried the repository " + ruleNumber + " times" + " added " + totalStAddedIn1Pass + 
                         " post processed statements, and  "+ notPostProcessed1Pass + 
                         " not post processed statements streams. Total Repository Queries: " + queryTimes);
                 ProcessController.checkState();

                 findConstruct(repository);
             } // while (modelChanged
             for (String qstring : this.constructQuery) {
                 
                 DecimalFormat df = new DecimalFormat("#.#####");
             log.debug("rule " +this.constructContext.get(qstring) + " takes " + df.format(ruleTimeTotal.get(qstring)) + " seconds");
             }
             //the construct rules run too many times
             if (runNbr >= runNbrMax) {
                 log.warn("runConstruct(): The construct rules have executed to the maximum number times allowed!");
             }
         }
         finally {

             if (con != null) {
                 try {
                     con.close();
                 } catch (RepositoryException e) {
                     log.error("runConstruct(): Caught a RepositoryException! Msg: " + e.getMessage());
                 }
             }
             endTime = new Date().getTime();
             double totaltime = (endTime - startTime) / 1000.0;
             log.info("runConstruct(): Summary: ");
             log.info("runConstruct(): Queried the repository " + queryTimes + " times");
             log.info("runConstruct(): Added " + totalStAdded + " post processed statement(s) and "+notPostProcessed+ " not post processed statement(s) in " + totaltime + " seconds.");
             log.debug("-----------------------------------------------------------------");
             log.debug("------------------- Leaving runConstruct() ---------------------");
             log.debug("-----------------------------------------------------------------");
         }
         return repositoryChanged;
         

     }

    
    /**
     * Find all Construct queries stored in the repository
     * @param repository-repository to use
     */
    private void findConstruct(Repository repository)  throws InterruptedException{
        TupleQueryResult result = null;
        RepositoryConnection con = null;
        List<String> bindingNames;

        log.debug("Locating Construct rules...");

        try {
            con = repository.getConnection();
            String queryString = "SELECT queries, contexts "
                    + "FROM "
                    + "{contexts} rdfcache:"+Terms.hasSerqlConstructQuery.getLocalId() +" {queries} "
                    + "using namespace "
                    + "rdfcache = <"+ Terms.rdfCacheNamespace+">";

            log.debug("queryString: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = (BindingSet) result.next();

                    Value firstValue = bindingSet.getValue("queries");
                    if (!constructQuery.contains(firstValue.stringValue())) {
                        constructQuery.add(firstValue.stringValue());
                    }
                    //log.debug("Adding construct to import pool: "
                    //        + firstValue.toString());
                    Value secondValue = bindingSet.getValue("contexts");
                    constructContext.put(firstValue.stringValue(), secondValue
                            .stringValue());

                }
            } else {
                log.debug("No Construct rules found in the repository!");
            }
        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("runConstruct(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
            if(con!=null){
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("runConstruct(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
        }

        log.debug("Number of constructs identified:  " + constructQuery.size());

    }

   
    /***************************************************************************
     * Convert construct queries into legal SeRQL queries
     *
     * @param queryString
     * @return
     */
    private String convertSWRLQueryToSeasameQuery(String queryString)  throws InterruptedException{

        postProcessFlag = ProcessingTypes.NONE;

        Pattern stringPattern = Pattern.compile("xs:string\\(([^)]+)\\)");

        Pattern dropquotesPattern = Pattern
                .compile("iridl:dropquotes\\(([^)]+)\\)");
        Pattern minusPattern = Pattern.compile("MINUS.*( using)?");

        Pattern rdfCachePattern = Pattern.compile("rdfcache:"+Terms.reTypeToContext.getLocalId());
        Pattern xsd2owlPattern = Pattern
                .compile("xsd2owl:increment\\(([^)]+)\\)");

        String pproces4sub2 = "\\{\\s*\\{(\\w+)\\s*\\}\\s*(.+)\\{(\\w+)\\s*\\}\\s*\\}";
        Pattern rproces4psub2 = Pattern.compile(pproces4sub2);

        String processedQueryString = queryString;
        
        Matcher mreifStr = rproces4psub2.matcher(processedQueryString);

        Boolean hasReified = false;

        if (mreifStr.find()) {  //reified statements
            String reifstr = " {} rdf:type {rdf:Statement} ; "
                    + " rdf:subject {" + mreifStr.group(1) + "} ;"
                    + " rdf:predicate {" + mreifStr.group(2) + "} ;"
                    + " rdf:object {" + mreifStr.group(3) + "} ;";

            processedQueryString = mreifStr.replaceFirst(reifstr);

            hasReified = true;
           
        }

        Matcher stringMatcher = stringPattern.matcher(processedQueryString); // xs:string

        Matcher dropquotesMatcher = dropquotesPattern
                .matcher(processedQueryString); // iridl:dropquotes

        Matcher rdfcacheMatcher = rdfCachePattern.matcher(processedQueryString); // rdfcache:retypeTo

        Matcher xsd2owlMatcher = xsd2owlPattern.matcher(processedQueryString); // xsdToOwl:increment


        Pattern comma = Pattern.compile(",");

        //Pattern p_fn_className = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\)).+using namespace.+\\2 *= *<import:([^#]+)#>",
        //                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        Pattern p_fn_className   = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(((\"[^\"]*\"|[^)\"]*)*)\\)).+using namespace.+\\2 *= *<import:([^#]+)#>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        Matcher functionMatcher = p_fn_className.matcher(processedQueryString);

        if (stringMatcher.find()) {
            postProcessFlag = ProcessingTypes.xsString;
            String vname = stringMatcher.group(1);
            processedQueryString = stringMatcher.replaceAll(vname);
            log.debug("Will postprocess xs:string(" + vname + ")");

        } else if (dropquotesMatcher.find()) {
            postProcessFlag = ProcessingTypes.DropQuotes;
            String vname = dropquotesMatcher.group(1);
            processedQueryString = dropquotesMatcher.replaceAll(vname);
            Matcher m23 = minusPattern.matcher(processedQueryString);
            String vname2 = m23.group(1);
            processedQueryString = m23.replaceFirst(vname2);
            log.debug("Will postprocess iridl:dropquotes(" + vname + ")");

        } else if (rdfcacheMatcher.find()) {
            postProcessFlag = ProcessingTypes.RetypeTo;
            log.debug("Will postprocess rdfcache:"+Terms.reTypeToContext.getLocalId());

        } else if (xsd2owlMatcher.find()) {
            postProcessFlag = ProcessingTypes.Increment;
            String vname = xsd2owlMatcher.group(1);

            processedQueryString = xsd2owlMatcher.replaceAll(vname);

        }
        else if (functionMatcher.find()) {
            functionMatcher.reset(); //reset the matcher
            String fullyQualifiedFunctionName;
            while (functionMatcher.find()) {
                String expand = "";
                String rdfFunctionName = functionMatcher.group(3);
                String rdfClassName = functionMatcher.group(6);


                fullyQualifiedFunctionName = rdfClassName + "#" + rdfFunctionName;

                log.debug("fullyQualifiedFunctionName = " + fullyQualifiedFunctionName); // full name of the function
                log.debug("class_name = " + rdfClassName); // class name of the function

                Method myFunction = getMethodForFunction(rdfClassName, rdfFunctionName);

                if (myFunction != null) {
                    postProcessFlag = ProcessingTypes.Function;
                }

                //String[] splittedStr = comma.split(functionMatcher.group(4));
                CSVSplitter splitter = new CSVSplitter();
                String[] splittedStr = splitter.split(functionMatcher.group(4));

                int i = 0;
                String fn = functionMatcher.group(2);
                String functionName = functionMatcher.group(3);

                expand += "}  <"+ Terms.callFunction.getUri() +"> {" + fn + ":" + functionName
                        + "} ; <"+ Terms.withArguments.getUri() +"> {} rdf:first {";
                for (String element : splittedStr) {
                    i++;
                    if (i < splittedStr.length) {
                        if(!element.equals(",")){
                        expand += element + "} ; rdf:rest {} rdf:first {";
                        }else{
                            expand += element;
                        }
                        log.debug("element " + i + " = " + element);
                    } else {
                        expand += element + "} ; rdf:rest {rdf:nil";
                        log.debug("element " + i + " = " + element);
                    }
                    log.debug("Will postprocess fn:" + functionMatcher.group(3));
                }



                processedQueryString = processedQueryString.substring(0, functionMatcher.start(1)) + expand + processedQueryString.substring(functionMatcher.end(1));
                log.debug("Inside convertSWRLQueryToSeasameQuery: " + processedQueryString);
                functionMatcher.reset(processedQueryString);


            }

        }

        return processedQueryString;

    }

    /***************************************************************************
     * Drop quotes in statements generated from running a construct rule.
     *
     * @param graphResult
     * @param creatValue
     * @param Added
     * @param toAdd
     * @param con
     * @param context
     * @throws QueryEvaluationException
     * @throws RepositoryException
     */
    private static void process_DropQuotes(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context)  throws InterruptedException, QueryEvaluationException,
            RepositoryException {

        // pproces2 =\"\\\"([^\\]+)\\\"\"(\^\^[^>]+>)? \.
        String pproces2 = "\\\"\\\\\\\"([^\\\\]+)\\\\\\\"\\\"(\\^\\^[^>]+>)? \\.";
        Pattern rproces2 = Pattern.compile(pproces2);

        while (graphResult.hasNext()) {
            Statement st = graphResult.next();

            Value obj = st.getObject();
            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();
            String statementStr = obj.toString();
            String newStatementStr = "";

            Matcher m = rproces2.matcher(statementStr);
            String vname = m.group(1);
            statementStr = m.replaceAll('"' + vname + '"'
                    + "^^<http://www.w3.org/2001/XMLSchema#string> .");

            String patternBn = "^_:";
            Pattern bn = Pattern.compile(patternBn);

            String sbjStr = sbj.toString();
            Matcher msbjStr = bn.matcher(sbjStr);
            if (msbjStr.find()) {

                // log.debug("Skipping blank node "+sbjStr);
            } else {
                newStatementStr = statementStr;

            }
            statementStr = newStatementStr;

            Value stStr = creatValue.createLiteral(statementStr);
            Statement stToAdd = new StatementImpl(sbj, prd, stStr);

            toAdd.add(stToAdd);
            Added.add(stToAdd);
            con.add(stToAdd, context); // add process_DropQuotes created st

        }
        // log.debug("After processing dropQuotes:\n " +
        // opendap.coreServlet.Util.getMemoryReport());

    }


    /***************************************************************************
     * Process "xs:string" in statements generated from running a construct rule.
     *
     * @param graphResult
     * @param creatValue
     * @param Added
     * @param toAdd
     * @param con
     * @param context
     * @throws org.openrdf.query.QueryEvaluationException
     * @throws org.openrdf.repository.RepositoryException
     */

    public static void process_xsString(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context)  throws InterruptedException, QueryEvaluationException,
            RepositoryException {

        // pproces1 = (\"[^"]+\")\s+\.
        String pproces1 = "(\\\"[^\"]+\\\")\\s+\\.";
        // Create a Pattern object
        Pattern rproces1 = Pattern.compile(pproces1);
        // Now create matcher object.
        while (graphResult.hasNext()) {
            Statement st = graphResult.next();

            Value obj = st.getObject();
            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();
            String statementStr = obj.stringValue();
            Matcher m = rproces1.matcher(statementStr);
            /*if (m.find()) {
                String vname = m.group(1);
                String replaceStr = vname
                        + "^^<http://www.w3.org/2001/XMLSchema#string> .";
                statementStr = m.replaceAll(replaceStr);
                // log.debug("postprocess1 statementStr=" +statementStr);
                // log.debug("vnam=" +vname);
            }*/
            URI dataType = creatValue.createURI("http://www.w3.org/2001/XMLSchema#string");
            Value stStr = creatValue.createLiteral(statementStr, dataType);
            Statement stToAdd = new StatementImpl(sbj, prd, stStr);

            toAdd.add(stToAdd);
            Added.add(stToAdd);
            con.add(stToAdd, context); // add process_xsString created st

        }
        // log.debug("After processing xs:string:\n " +
        // opendap.coreServlet.Util.getMemoryReport());
    }


    /***************************************************************************
     * Cast data type in statements generated from running a construct rule
     *
     * @param graphResult
     * @param creatValue
     * @param Added
     * @param toAdd
     * @param con
     * @param context
     * @throws QueryEvaluationException
     * @throws RepositoryException
     */

    private void process_RetypeTo(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context)  throws InterruptedException, QueryEvaluationException,
            RepositoryException {

        // pproces3 =\"\\\"([^\\]+)\\\"\"\^\^
        String pproces3 = "\\\"\\\\\\\"([^\\\\]+)\\\\\\\"\\\"\\^\\^";
        Pattern rproces3 = Pattern.compile(pproces3);
        String pproces3sub = "(.+)";
        Pattern rproces3sub = Pattern.compile(pproces3sub);

        String pproces3subsub1 = "<"+ Terms.reTypeToContext.getUri() +"> <([^>]+)>";
        String pproces3subsub2 = "([^ ]+) <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> (\"(.+)\")\\^\\^";

        Pattern rproces3subsub1 = Pattern.compile(pproces3subsub1);
        Pattern rproces3subsub2 = Pattern.compile(pproces3subsub2);

        while (graphResult.hasNext()) {
            Statement st = graphResult.next();

            Value obj = st.getObject();
            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();
            String statementStr = obj.toString();
            Matcher mproces3 = rproces3.matcher(statementStr);

            String replace = mproces3.group(1) + '"' + "^^";
            statementStr = mproces3.replaceAll(replace);

            Matcher mproces3sub = rproces3sub.matcher(statementStr);

            String newStStr = statementStr;
            String lastline = "";
            String line = "";

            String node = "";
            String value = "";
            String newtype = "";
            String statement = "";
            while (mproces3sub.find()) {
                String lastline2 = lastline;
                lastline = line;
                line = mproces3sub.group(1);

                Matcher msub1 = rproces3subsub1.matcher(line);
                if (msub1.find()) {
                    newtype = msub1.group(1);
                    Matcher msub2 = rproces3subsub2.matcher(lastline);
                    if (msub2.find()) {
                        node = msub2.group(1);
                        value = msub2.group(3);
                    }
                    String psub3 = "(.+) " + node + " .";
                    Pattern cpsub3 = Pattern.compile(psub3);
                    Matcher msub3 = cpsub3.matcher(lastline2);
                    if (msub3.find()) {
                        statement = msub3.group(1);
                    }
                    newStStr = statement + " " + '"' + value + '"' + "^^<"
                            + newtype + "> .\n";
                }
            }

            Value stStr = creatValue.createLiteral(newStStr);
            Statement stToAdd = new StatementImpl(sbj, prd, stStr);
            log.debug("original st=" + st.toString());
            log.debug("new stToAdd=" + stToAdd.toString());
            log.debug("In postprocess3");

            toAdd.add(stToAdd);
            Added.add(stToAdd);
            con.add(stToAdd, context);// add process_RetypeTo created st

        }
        // log.debug("After processing RetypeTo:\n " +
        // opendap.coreServlet.Util.getMemoryReport());

    }

    /***************************************************************************
     * Increment number by 1 in statements generated from running a construct rule
     *
     * @param graphResult
     * @param creatValue
     * @param Added
     * @param toAdd
     * @param con
     * @param context
     * @throws QueryEvaluationException
     * @throws RepositoryException
     */
    private void process_Increment(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context)  throws InterruptedException, QueryEvaluationException,
            RepositoryException {

        String pproces4 = "(.+)";
        Pattern rproces4 = Pattern.compile(pproces4);

        String pproces4sub = "\"(\\d+)\"";

        Pattern rproces4psub = Pattern.compile(pproces4sub);

        String pproces4sub2 = "\\{\\s*\\{(\\w+)\\s*\\}\\s*(.+)\\{(\\w+)\\s*\\}\\s*\\}";
        Pattern rproces4psub2 = Pattern.compile(pproces4sub2);

        while (graphResult.hasNext()) {
            Statement st = graphResult.next();

            Value obj = st.getObject();
            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();
            String statementStr = obj.toString();

            String numincrStr = ""; // after increment

            Matcher mproces4 = rproces4.matcher(statementStr);
            if (mproces4.find()) {
                statementStr = mproces4.group(1);
                Matcher mproces4sub = rproces4psub.matcher(statementStr);

                if (mproces4sub.find()) { // find number, do increment
                    int numincr = Integer.parseInt(mproces4sub.group(1));
                    // log.debug("before increment numincr = " +numincr);
                    numincr++;

                    numincrStr = Integer.toString(numincr);
                    // log.debug("after increment numincrStr = " +numincrStr);

                    statementStr = numincrStr;

                    Value stStr = creatValue.createLiteral(statementStr);
                    Statement stToAdd = new StatementImpl(sbj, prd, stStr);
                    st = stToAdd;
                }
                // log.debug("new st = "+st.toString());

                toAdd.add(st);
                Added.add(st);
                con.add(st, context);// add st with incremented number
                // log.debug("Increment added new tatement stToAdd= "
                // + st.toString());

            } else {
                toAdd.add(st);
                Added.add(st);
                con.add(st, context);// add st without increment (not a
                // number)
                // log.debug("Increment added original tatement st= "
                // + st.toString());
            }

        } // while (graphResult.hasNext())

    }

    /***************************************************************************
     * process fn created statements
     *
     * @param graphResult
     * @param creatValue
     * @param Added
     * @param toAdd
     * @param con
     * @param context
     * @throws QueryEvaluationException
     * @throws RepositoryException
     */
    private void process_fn(GraphQueryResult graphResult,
                             ValueFactory creatValue, Vector<Statement> Added,
                             Vector<Statement> toAdd, RepositoryConnection con,
                             Resource[] context)  throws InterruptedException, QueryEvaluationException,
            RepositoryException {

        log.debug("Processing fn statements.");


        URI rdffirst = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
        URI rdfrest = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
        URI endList = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");
        URI myfn = creatValue.createURI(Terms.callFunction.getUri());
        URI myfnlist = creatValue.createURI(Terms.withArguments.getUri());


        URI prdLastSt = null;
        Resource sbjLastSt = null;

        Statement oldSt = null;
        while (graphResult.hasNext()) {

            Statement st = graphResult.next();
            URI prd = st.getPredicate();
            Value obj = st.getObject();

            // listnode is  equal to endList until a list is found. While a list is being parsed/processed it is
            // set to the current list node. After the list is processed it is set back to endList
            Value listnode = endList;

            Resource sbj;
            URI targetPrd;
            Resource targetSbj;
            Statement newSt = null;
            Method func = null;


            if (prd.equals(myfn)) {
                targetSbj = sbjLastSt;
                targetPrd = prdLastSt;


                String className; // class name
                String fnName; // function name

                if (prd.equals(myfn)) {
                    String functionImport = obj.stringValue();
                    int indexOfLastPoundSign = functionImport.lastIndexOf("#");

                    className = functionImport.substring("import:".length(), indexOfLastPoundSign);
                    fnName = functionImport.substring(indexOfLastPoundSign + 1);

                    func = getMethodForFunction(className, fnName);

                }
                Boolean isEndList = endList.equals(obj);
                List<String> rdfList = new ArrayList<String>();
                //int statementNbr = 1;
                while (graphResult.hasNext() && !isEndList) {
                    st = graphResult.next();
                    // log.debug("Current statement " + statementNbr++ + ": " + st);
                    obj = st.getObject();
                    prd = st.getPredicate();
                    sbj = st.getSubject();

                    if (myfnlist.equals(prd)) {
                        listnode = obj;
                    } else if (listnode.equals(sbj) && rdffirst.equals(prd)) {
                        String elementValue = obj.stringValue();
                        rdfList.add(elementValue);
                    } else if (listnode.equals(sbj) && rdfrest.equals(prd)) {
                        listnode = obj;
                        isEndList = endList.equals(obj);
                    }
                }

                if (func != null) {
                    Value stObj = null;
                    try {

                        // We can pass null here:
                        stObj = (Value) func.invoke(null, rdfList, creatValue);
                        //  because we know that the method that is being
                        // invoked is static, so we don't need an
                        // instance of class to invoke the method from.


                        newSt = new StatementImpl(targetSbj, targetPrd, stObj);
                    } catch (IllegalAccessException e) {
                        log.error("Unable to invoke processing function "
                                + func.getName()
                                + "' Caught IllegalAccessException, msg: "
                                + e.getMessage());
                    } catch (InvocationTargetException e) {
                        log.error("Unable to invoke processing function "
                                + func.getName()
                                + "' Caught InvocationTargetException, msg: "
                                + e.getMessage());
                    }
                } else {
                    log.warn("Process Function failed: No processing function found.");
                }

            } //if (prd.equals(myfn))

            prdLastSt = st.getPredicate();
            sbjLastSt = st.getSubject();

            if (newSt != null) {
                log.debug("new st to add = " + newSt.toString());
                st = newSt;
                oldSt = null;
            }

            if (oldSt != null) {
                toAdd.add(oldSt);
                Added.add(oldSt);
                con.add(oldSt, context); // add fn created new st
                log.debug("process_fn add context: " + context[0].toString());
            }
            oldSt = st;

        } // while (graphResult.hasNext())
        if (oldSt != null) {
            toAdd.add(oldSt);
            Added.add(oldSt);
            con.add(oldSt, context); // add fn created new st
            log.debug("process_fn add context: " + context[0].toString());
        }

        log.debug("After processing fn: " + toAdd.size()
                + " statements are added.\n ");
    }

    /**
     * Invoke a method from a class.
     * @param className
     * @param methodName
     * @return
     */
    public Method getMethodForFunction(String className,
                                              String methodName)  throws InterruptedException{

        Method method;


        try {
            Class<?> methodContext = Class.forName(className);
            log.debug("getMethodForFunction() - Located java class: "
                    + className);

            try {
                method = methodContext.getMethod(methodName, List.class, ValueFactory.class);

                if (Modifier.isStatic(method.getModifiers())) {
                    log.debug("getMethodForFunction() - Located static java method: "
                                    + getProcessingMethodDescription(method));
                    return method;
                }

                /*
                 * for(Constructor c : methodContext.getConstructors()){
                 * if(c.getGenericParameterTypes().length==0){
                 * log.debug("getMethodForFunction() - Located java class
                 * '"+className+"' with a no element " + "constructor and the
                 * method "+getProcessingMethodDescription(method)); return
                 * method; } }
                 */

            } catch (NoSuchMethodException e) {
                log.error("getMethodForFunction() - The class '" + className
                        + "' does not contain a method called '" + methodName
                        + "'");
            }

        } catch (ClassNotFoundException e) {
            log.error("getMethodForFunction() - Unable to locate java class: "
                    + className);
        }

        log.error("getMethodForFunction() - Unable to locate the requested java class/method combination. "
                        + "class: '"
                        + className
                        + "'   method: '"
                        + methodName
                        + "'");
        return null;

    }

    
    public  Method getMethodForFunction(Object classInstance,
            String methodName)  throws InterruptedException{

        Method method;

        Class<?> methodContext = classInstance.getClass();
        String className = methodContext.getName();


        try {
            method = methodContext.getMethod(methodName, List.class,
                    ValueFactory.class);
            log.debug("getMethodForFunction() - Located the java method: "
                    + getProcessingMethodDescription(method)
                    + " in an instance of the class '" + className + "'");
            return method;

        } catch (NoSuchMethodException e) {
            log.error("getMethodForFunction() - The class '" + className
                            + "' does not contain a method called '"
                            + methodName + "'");
        }

        log.error("getMethodForFunction() - Unable to locate the requested java class/method combination. "
                        + "class: '"
                        + className
                        + "'   method: '"
                        + methodName
                        + "'");
        return null;

    }


    public  String getProcessingMethodDescription(Method m)  throws InterruptedException {

        String msg = "";

        msg += m.getReturnType().getName() + " ";
        msg += m.getName();

        String params = "";
        for (Class c : m.getParameterTypes()) {
            if (!params.equals(""))
                params += ", ";
            params += c.getName();
        }
        msg += "(" + params + ")";

        String exceptions = "";
        for (Class c : m.getExceptionTypes()) {
            if (!exceptions.equals(""))
                exceptions += ", ";
            exceptions += c.getName();
        }
        msg += " " + exceptions + ";";

        return msg;

    }


}
