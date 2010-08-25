package opendap.semantics.IRISail;

import java.io.*;
import java.lang.reflect.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;

import org.openrdf.sail.Sail;
import org.openrdf.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends org.openrdf.repository.sail.SailRepository. It can be used
 * to populate a repository from a file or URL. It inherits all fields and
 * methods from parent class SailRepository. It also has new methods to
 * recursively add imports and seealso statements refered documents. It is RDF
 * schema aware.
 * 
 * @author Haibo liu, iri.columbia.edu
 * @version 1.0
 */
public class IRISailRepository extends SailRepository {
    // imports holds contexts (URL of owl, xsd, etc)
    Vector<String> imports;
    HashMap<String, Boolean> downService;
    // constructor
    private Logger log;

    private String resourceDir;
    private String contentDir;

    private Vector<String> constructQuery;
    private HashMap<String, String> constructContext;

    private ProcessingTypes postProcessFlag;

    private AtomicBoolean isRepositoryDown;

    private ConcurrentHashMap<String, String> serverIDs = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, String> wcsIDs = new ConcurrentHashMap<String, String>();

    public void startup() throws org.openrdf.repository.RepositoryException {
        super.initialize();
        setRepositoryDown(false);
    }

    private void setRepositoryDown(Boolean repositoryState)
            throws org.openrdf.repository.RepositoryException {
        isRepositoryDown.set(repositoryState);
    }

    public Boolean isRepositoryDown()
            throws org.openrdf.repository.RepositoryException {
        return isRepositoryDown.get();
    }

    /**
     * Does a repository shutdown, but only if the repository is running!.
     * 
     * @throws RepositoryException
     */
    @Override
    public void shutDown() throws RepositoryException {

        log.debug("shutDown(): Shutting down Repository...");
        if (!isRepositoryDown()) {
            super.shutDown();
            setRepositoryDown(true);
            log.info("shutDown(): Semantic Repository Has Been Shutdown.");
        } else {
            log.info("shutDown(): Semantic Repository was already down.");
        }
        log.debug("shutDown(): Repository shutdown complete.");
    }

    public IRISailRepository(Sail sail, String resourceDir, String contentDir) {
        super(sail);
        log = LoggerFactory.getLogger(getClass());
        imports = new Vector<String>();
        downService = new HashMap<String, Boolean>();
        this.resourceDir = resourceDir;
        this.contentDir = contentDir;
        isRepositoryDown = new AtomicBoolean();
        constructQuery = new Vector<String>();
        constructContext = new HashMap<String, String>();

    }

    /*
     * Run all Construct queries and statement into repository
     */

    public void runConstruct() throws RepositoryException {

        log
                .debug("-----------------------------------------------------------------");
        log
                .debug("------------------- Entering runConstruct() ---------------------");
        log
                .debug("-----------------------------------------------------------------");

        GraphQueryResult graphResult = null;
        RepositoryConnection con = null;
        Vector<Statement> Added = new Vector<Statement>();

        Boolean modelChanged = true;
        int runNbr = 0;
        int runNbrMax = 20;
        long startTime, endTime;
        startTime = new Date().getTime();

        int queryTimes = 0;
        long ruleStartTime, ruleEndTime;
        int totalStAdded = 0; // number of statements added
        int totalStAddedIn1Pass = 0; // number of statements added in 1 PASS
        
        findConstruct();
        
         //log.debug("Before running the construct rules:\n " +
         //opendap.coreServlet.Util.getMemoryReport());
        con = this.getConnection();
         
        while (modelChanged && runNbr < runNbrMax) {
            
            runNbr++;
            modelChanged = false;
            totalStAddedIn1Pass = 0;
            log.info("Total construct rule number =  " + this.constructQuery.size());
            //log.debug("Applying Construct Rules. Beginning Pass #" + runNbr
            //        + " \n" + opendap.coreServlet.Util.getMemoryReport());
            int ruleNumber = 0;
            for (String qstring : this.constructQuery) {
                ruleNumber++;
                queryTimes++;
                ruleStartTime = new Date().getTime();
                int stAdded = 0; // track statements added by each rule

                Vector<Statement> toAdd = new Vector<Statement>();
                String constructURL = this.constructContext.get(qstring);

                //URI uriaddress = new URIImpl(constructURL);
                URI uriaddress = new URIImpl(Terms.externalInferencingContextUri);
                Resource[] context = new Resource[1];
                context[0] = uriaddress;

                String processedQueryString = convertSWRLQueryToSeasameQuery(qstring);
                
                try {
                     //log.debug("Prior to making new repository connection:\n "
                     //+ opendap.coreServlet.Util.getMemoryReport());
                    log.debug("Original construct rule ID: " + constructURL);
                    GraphQuery graphQuery = con.prepareGraphQuery(
                            QueryLanguage.SERQL, processedQueryString);
                    
                    log.info("Querying the repository. PASS #" + queryTimes
                            + " (construct rules pass #" + runNbr + ")");

                    graphResult = graphQuery.evaluate();
                    GraphQueryResult graphResultStCount = graphQuery.evaluate();
                    log.info("Completed querying. ");

                     //log.debug("After evaluating construct rules:\n " +
                     //opendap.coreServlet.Util.getMemoryReport());

                    log.info("Post processing query result and adding statements ... ");

                    if (graphResult.hasNext()) {
                        modelChanged = true;

                        ValueFactory creatValue = this.getValueFactory();

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
                                                  // subtract, getWcsID
                            break;
                        case NONE:
                        default:
                            log.info("Adding none-postprocess statements ...");

                            con.add(graphResult, context);
                            int nonePostprocessSt = 0;
                            while (graphResultStCount.hasNext()) {
                                graphResultStCount.next();
                                nonePostprocessSt++;
                                stAdded++;
                            }
                            /*
                             * int nonePostprocessSt = 0; while
                             * (graphResult.hasNext()) { Statement st =
                             * graphResult.next(); con.add(st, context); //
                             * log.debug("Added statement = " //
                             * +st.toString()); toAdd.add(st); Added.add(st);
                             * nonePostprocessSt++; }
                             */
                            log.info("Complete adding " + nonePostprocessSt
                                    + " none-postprocess statements");
                            // log.debug("After processing default (NONE)
                            // case:\n " +
                            // opendap.coreServlet.Util.getMemoryReport());

                            break;
                        }

                        // log.info("Adding statements ...");
                        stAdded = 0;
                        if (toAdd != null) {
                            // con.add(toAdd, context);
                            log.info("Total added " + toAdd.size()
                                    + " statements.");
                            /*
                             * for(Statement sttoadd:toAdd){ log.debug("Add
                             * statement: "+sttoadd.toString()); }
                             */
                            stAdded = toAdd.size();
                        }

                    } // if (graphResult != null
                    else {
                        log.debug("No query result!");
                    }

                } catch (QueryEvaluationException e) {
                    log.error("Caught an QueryEvaluationException! Msg: "
                            + e.getMessage());

                } catch (RepositoryException e) {
                    log.error("Caught RepositoryException! Msg: "
                            + e.getMessage());
                } catch (MalformedQueryException e) {
                    log.error("Caught MalformedQueryException! Msg: "
                            + e.getMessage());
                    log.debug("MalformedQuery: " + processedQueryString);
                } finally {
                    if (graphResult != null) {
                        try {
                            graphResult.close();
                        } catch (QueryEvaluationException e) {
                            log.error("Caught a QueryEvaluationException! Msg: "
                                            + e.getMessage());
                        }
                    }

                }

                ruleEndTime = new Date().getTime();
                double ruleTime = (ruleEndTime - ruleStartTime) / 1000.0;
                                
                //log.debug("Processed construct rule : " + processedQueryString);
                log.debug("Construct rule " + ruleNumber + " takes " + ruleTime
                        + " seconds in loop " + runNbr + " added " + stAdded
                        + " statements");
                
                totalStAdded = totalStAdded + stAdded;
                totalStAddedIn1Pass = totalStAddedIn1Pass+ stAdded;
            } // for(String qstring
            log.info("Completed pass " + runNbr + " of Construct evaluation"+"Queried the repository " + 
                    queryTimes + " times" + " added " + totalStAddedIn1Pass + " statements");
            log.info("Queried the repository " + queryTimes + " times");

            findConstruct();
        } // while (modelChanged

        try {
            con.close();
        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        }
        endTime = new Date().getTime();
        double totaltime = (endTime - startTime) / 1000.0;
        log.info("In construct for " + totaltime + " seconds");
        log.info("Total number of statements added in construct: "
                + totalStAdded + " \n");

    }

    /*
     * Find all Construct queries
     */
    private void findConstruct() {
        TupleQueryResult result = null;
        RepositoryConnection con = null;
        List<String> bindingNames;

        log.debug("Locating Construct rules...");

        try {
            con = this.getConnection();
            String queryString = "SELECT queries, contexts "
                    + "FROM "
                    + "{contexts} rdfcache:"+Terms.serqlTextType+" {queries} "
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
                log.debug("No query result!");
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
                } catch (QueryEvaluationException e) {
                    log.error("Caught a QueryEvaluationException! Msg: "
                            + e.getMessage());
                }
            }
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException! Msg: "
                        + e.getMessage());
            }
        }

        log.info("Number of constructs identified:  " + constructQuery.size());

    }

    public enum FunctionTypes {
        None, getWcsID, Subtract, Join
    }

    /***************************************************************************
     * Convert construct queries into legal SeRQL queries
     * 
     * @param queryString
     * @return
     */
    private String convertSWRLQueryToSeasameQuery(String queryString) {

        postProcessFlag = ProcessingTypes.NONE;

        Pattern stringPattern = Pattern.compile("xs:string\\(([^)]+)\\)");

        Pattern dropquotesPattern = Pattern
                .compile("iridl:dropquotes\\(([^)]+)\\)");
        Pattern minusPattern = Pattern.compile("MINUS.*( using)?");

        Pattern rdfCachePattern = Pattern.compile("rdfcache:"+Terms.reTypeToContext);
        Pattern xsd2owlPattern = Pattern
                .compile("xsd2owl:increment\\(([^)]+)\\)");

        String pproces4sub2 = "\\{\\s*\\{(\\w+)\\s*\\}\\s*(.+)\\{(\\w+)\\s*\\}\\s*\\}";
        Pattern rproces4psub2 = Pattern.compile(pproces4sub2);

        String processedQueryString = queryString;
        log.info("Original construct: " + queryString);
        Matcher mreifStr = rproces4psub2.matcher(processedQueryString);

        Boolean hasReified = false;

        if (mreifStr.find()) {
            String reifstr = " {} rdf:type {rdf:Statement} ; "
                    + " rdf:subject {" + mreifStr.group(1) + "} ;"
                    + " rdf:predicate {" + mreifStr.group(2) + "} ;"
                    + " rdf:object {" + mreifStr.group(3) + "} ;";

            processedQueryString = mreifStr.replaceFirst(reifstr);

            hasReified = true;
            // log.info("query string has reified statements = " + hasReified);
        }

        Matcher stringMatcher = stringPattern.matcher(processedQueryString); // xs:string

        Matcher dropquotesMatcher = dropquotesPattern
                .matcher(processedQueryString); // iridl:dropquotes

        Matcher rdfcacheMatcher = rdfCachePattern.matcher(processedQueryString); // rdfcache:retypeTo

        Matcher xsd2owlMatcher = xsd2owlPattern.matcher(processedQueryString); // xsd2owl:increment
        
        //Pattern functionPattern = Pattern
        //        .compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\))");// fn:name(abc)
        Pattern comma = Pattern.compile(",");

                
        //Pattern p_fn = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\))");

        Pattern p_fn_className = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\)).+using namespace.+\\2 *= *<import:([^#]+)#>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        
        Matcher functionMatcher = p_fn_className.matcher(processedQueryString);
        
        //String expand = "";
        if (stringMatcher.find()) {
            postProcessFlag = ProcessingTypes.xsString;
            String vname = stringMatcher.group(1);
            processedQueryString = stringMatcher.replaceAll(vname);
            log.info("Will postprocess xs:string(" + vname + ")");

        } else if (dropquotesMatcher.find()) {
            postProcessFlag = ProcessingTypes.DropQuotes;
            String vname = dropquotesMatcher.group(1);
            processedQueryString = dropquotesMatcher.replaceAll(vname);
            Matcher m23 = minusPattern.matcher(processedQueryString);
            String vname2 = m23.group(1);
            processedQueryString = m23.replaceFirst(vname2);
            log.info("Will postprocess iridl:dropquotes(" + vname + ")");

        } else if (rdfcacheMatcher.find()) {
            postProcessFlag = ProcessingTypes.RetypeTo;
            log.info("Will postprocess rdfcache:"+Terms.reTypeToContext);

        } else if (xsd2owlMatcher.find()) {
            postProcessFlag = ProcessingTypes.Increment;
            String vname = xsd2owlMatcher.group(1);

            processedQueryString = xsd2owlMatcher.replaceAll(vname);

            // log.info("processedQueryString = " + processedQueryString);

        } 
        else if (functionMatcher.find()) {
            functionMatcher.reset(); //reset the matcher
            String fullyQualifiedFunctionName;
            while (functionMatcher.find()) {
                String expand = ""; 
                String rdfFunctionName = functionMatcher.group(3);
                String rdfClassName = functionMatcher.group(5);
                
                
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

                expand += "}  <"+ Terms.functionsContextUri +"> {" + fn + ":" + functionName
                        + "} ; <"+ Terms.listContextUri +"> {} rdf:first {";
                for (String element : splittedStr) {
                    i++;
                    if (i < splittedStr.length) {
                        if(!element.equals(",")){
                        expand += element + "} ; rdf:rest {} rdf:first {";
                        }else{
                            expand += element;   
                        }
                        log.info("element " + i + " = " + element);
                    } else {
                        expand += element + "} ; rdf:rest {rdf:nil";
                        log.info("element " + i + " = " + element);
                    }
                    log.info("Will postprocess fn:" + functionMatcher.group(3));
                }
                
                
                
                processedQueryString = processedQueryString.substring(0, functionMatcher.start(1)) + expand + processedQueryString.substring(functionMatcher.end(1));
                
                functionMatcher.reset(processedQueryString);
                
                
            }
            /*** 
            while (functionMatcher.find()) {
            
                String rdfFunctionName = functionMatcher.group(3);
                String rdfClassName = functionMatcher.group(5);
                
                
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

                expand += "}  <"+RepositoryUtility.functionsContextUri+"> {" + fn + ":" + functionName
                        + "} ; <"+RepositoryUtility.listContextUri+"> {} rdf:first {";
                for (String element : splittedStr) {
                    i++;
                    if (i < splittedStr.length) {
                        if(!element.equals(",")){
                        expand += element + "} ; rdf:rest {} rdf:first {";
                        }else{
                            expand += element;   
                        }
                        log.info("element " + i + " = " + element);
                    } else {
                        expand += element + "} ; rdf:rest {rdf:nil";
                        log.info("element " + i + " = " + element);
                    }
                    log.info("Will postprocess fn:" + functionMatcher.group(3));
                }
                
                
                
                processedQueryString = processedQueryString.substring(0, functionMatcher.start(1)) + expand + processedQueryString.substring(functionMatcher.end(1));
                
                functionMatcher.reset(processedQueryString);
                
                
            }*/

        }

        log.info("Processed construct: " + processedQueryString);
        return processedQueryString;

    }

    /***************************************************************************
     * processing xs:string
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

    private void process_xsString(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context) throws QueryEvaluationException,
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
            String statementStr = obj.toString();
            Matcher m = rproces1.matcher(statementStr);
            if (m.find()) {
                String vname = m.group(1);
                String replaceStr = vname
                        + "^^<http://www.w3.org/2001/XMLSchema#string> .";
                statementStr = m.replaceAll(replaceStr);
                // log.debug("postprocess1 statementStr=" +statementStr);
                // log.debug("vnam=" +vname);
            }
            Value stStr = creatValue.createLiteral(statementStr);
            Statement stToAdd = new StatementImpl(sbj, prd, stStr);

            toAdd.add(stToAdd);
            Added.add(stToAdd);
            con.add(stToAdd, context); // add process_xsString created st

        }
        // log.debug("After processing xs:string:\n " +
        // opendap.coreServlet.Util.getMemoryReport());
    }

    /***************************************************************************
     * DropQuotes
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
    private void process_DropQuotes(GraphQueryResult graphResult,
            ValueFactory creatValue, Vector<Statement> Added,
            Vector<Statement> toAdd, RepositoryConnection con,
            Resource[] context) throws QueryEvaluationException,
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
     * cast type
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
            Resource[] context) throws QueryEvaluationException,
            RepositoryException {

        // pproces3 =\"\\\"([^\\]+)\\\"\"\^\^
        String pproces3 = "\\\"\\\\\\\"([^\\\\]+)\\\\\\\"\\\"\\^\\^";
        Pattern rproces3 = Pattern.compile(pproces3);
        String pproces3sub = "(.+)";
        Pattern rproces3sub = Pattern.compile(pproces3sub);

        String pproces3subsub1 = "<"+ Terms.reTypeToContextUri +"> <([^>]+)>";
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
     * Increment numbers
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
            Resource[] context) throws QueryEvaluationException,
            RepositoryException {

        String pproces4 = "(.+)";
        Pattern rproces4 = Pattern.compile(pproces4);

        // String pproces4sub ="(.+)\"(\\d+)\"(.+)";
        String pproces4sub = "\"(\\d+)\"";
        // pproces4= \"(\d+)\"
        // String pproces4sub ="\\\"(\\d+)\\\"";
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

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformRDFa(String inURI)
            throws SaxonApiException {
        log.debug("In transformFDRa");

        return transformRDFa(new StreamSource(inURI));


    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformRDFa(InputStream is)
            throws SaxonApiException {
        log.debug("In transformFDRa");

        return transformRDFa(new StreamSource(is));


    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformRDFa(StreamSource sourceURL)
            throws SaxonApiException {
        log.debug("In transformFDRa");

        String transformStyleFileName = resourceDir + "xsl/RDFa2RDFXML.xsl";

        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(new File(
                transformStyleFileName)));

        XdmNode source = proc.newDocumentBuilder().build(sourceURL);
        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        out.setOutputStream(outStream);
        XsltTransformer trans = exp.load();
        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();
        log.info(outStream.toString());
        log.debug("Output written to OutputStream");
        return outStream;


    }
    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformXSD(String inURI)
            throws SaxonApiException {
        return transformXSD(new StreamSource(inURI));
    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformXSD(InputStream is)
            throws SaxonApiException {
            return transformXSD(new StreamSource(is));
    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformXSD(StreamSource sourceUrl)
            throws SaxonApiException {
        log.debug("In transformXSD");
        String transformFileName = resourceDir + "xsl/xsd2owl.xsl";

        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(new File(
                transformFileName)));
        // XsltExecutable exp = comp.compile(new StreamSource(new
        // File("/data/benno/xslt/xsd2owl.xsl")));
        XdmNode source = proc.newDocumentBuilder().build(sourceUrl);
        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        out.setOutputStream(outStream);
        XsltTransformer trans = exp.load();
        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();
        log.debug("Output written to OutputStream");
        return outStream;
    }

    /**
     * Checks and returns last modified time of an URL (context) via http
     * connection. The input is a string of an URL.
     *
     * @param urlstring
     */

    public String getLTMODContext(String urlstring) {
        String ltmodstr = "";
        try {
            URL myurl = new URL(urlstring);
            HttpURLConnection hc = (HttpURLConnection) myurl.openConnection();
            long ltmod = hc.getLastModified();
            // log.debug("lastModified: "+ltmod);
            ltmodstr = getLastModifiedTimeString(ltmod);
        } catch (MalformedURLException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getLocalizedMessage());
        } catch (IOException e) {
            log.error("Caught an IOException! Msg: " + e.getMessage(), e);
        }
        return ltmodstr;
    }

    public String getLastModifiedTimeString(Date date) {
        return getLastModifiedTimeString(date.getTime());
    }


    public String getLastModifiedTimeString(long epochTime) {
        String ltmodstr = "";
        Timestamp ltmodsql = new Timestamp(epochTime);
        String ltmodstrraw = ltmodsql.toString();
        ltmodstr = ltmodstrraw.substring(0, 10) + "T"
                + ltmodstrraw.substring(11, 19) + "Z";
        return ltmodstr;
    }


    /**
     * Checks and returns last modified time of a context (URI) via querying
     * against the repository on contexts.
     * 
     * @param urlstring
     */
    public String getLastModifiedTime(String urlstring) {
        TupleQueryResult result = null;
        String ltmodstr = "";
        URI uriaddress = new URIImpl(urlstring);
        Resource[] context = new Resource[1];
        context[0] = (Resource) uriaddress;
        RepositoryConnection con = null;

        //String queryString = "SELECT DISTINCT x, y FROM CONTEXT <"
        //        + uriaddress
        //        + "> {x} <"+RepositoryUtility.lastModifiedContextUri+"> {y} "
        //        + "where x=<" + uriaddress + ">";

        String queryString = "SELECT doc,lastmod FROM CONTEXT "
                  + "rdfcache:"+Terms.cacheContext+" {doc} rdfcache:"+Terms.lastModifiedContext+" {lastmod} "
                  + "where doc=<" + uriaddress + ">"
                  + "USING NAMESPACE "
                  + "rdfcache = <"+ Terms.rdfCacheNamespace+">";
        try {
            con = this.getConnection();

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfY;

            while (result.hasNext()) { // should have only one value
                bindingSet = (BindingSet) result.next();
                Set<String> names = bindingSet.getBindingNames();
                // for (String name : names) {
                // log.debug("BindingNames: " + name);
                // }
                valueOfY = (Value) bindingSet.getValue("lastmod");
                ltmodstr = valueOfY.stringValue();
                // log.debug("Y:" + valueOfY.stringValue());

            }

        } catch (QueryEvaluationException e) {
            log.error("Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            try {
                result.close();

            } catch (Exception e) {
                log.error("Caught an Exception! Msg: " + e.getMessage());
            }
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException! Msg: "
                        + e.getMessage());
            }
        }

        return ltmodstr;
    }


    /**
     * Insert a statement declaring the content type of the document.
     *
     * @param importURL
     * @param contentType
     * @param con
     */
    public void setContentTypeContext(String importURL, String contentType, RepositoryConnection con) {
        if (!this.imports.contains(importURL)) { // not in the repository yet
            
            ValueFactory valueFactory = this.getValueFactory();
            URI s = valueFactory.createURI(importURL);
            URI contentTypeContext = valueFactory.createURI(Terms.contentTypeContextUri);
            URI cacheContext = valueFactory.createURI(Terms.cacheContextUri);
            
            Literal o = valueFactory.createLiteral(contentType);

            try {

                con.add((Resource) s, contentTypeContext, (Value) o, (Resource) cacheContext);

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }

    /**
     * Set last_modified_time of the URI in the repository.
     * @param importURL
     * @param con
     */
    public void setLTMODContext(String importURL, RepositoryConnection con) {
        String ltmod = this.getLTMODContext(importURL);
        setLTMODContext(importURL, ltmod, con);
    }


    /**
     *
     *
     * @param importURL
     * @param ltmod
     * @param con
     */
    public void setLTMODContext(String importURL, String ltmod, RepositoryConnection con) {

        if (!this.imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            // log.debug("lastmodified " + ltmod);
            ValueFactory f = this.getValueFactory();
            URI s = f.createURI(importURL);
            URI p = f.createURI(Terms.lastModifiedContextUri);
            URI cont = f.createURI(Terms.cacheContextUri);
            URI sxd = f.createURI("http://www.w3.org/2001/XMLSchema#dateTime");
            Literal o = f.createLiteral(ltmod, sxd);

            try {

                con.add((Resource) s, p, (Value) o, (Resource) cont);

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }

    /***************************************************************************
     * *************************************************************************
     * *************************************************************************
     * *************************************************************************
     * *************************************************************************
     * *************************************************************************
     */

    public enum ProcessingTypes {
        NONE, xsString, DropQuotes, RetypeTo, Increment, Function
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
                             Resource[] context) throws QueryEvaluationException,
            RepositoryException {

        log.debug("Processing fn statements.");


        URI rdffirst = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
        URI rdfrest = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
        URI endList = creatValue.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");
        URI myfn = creatValue.createURI(Terms.functionsContextUri);
        URI myfnlist = creatValue.createURI(Terms.listContextUri);

        FunctionTypes functionTypeFlag = FunctionTypes.None;
        Value objLastSt = null;

        URI prdLastSt = null;
        Resource sbjLastSt = null;
        
        Statement oldSt = null;
        while (graphResult.hasNext()) {
            Statement st = graphResult.next();
            Statement newSt = null;

            Value obj = st.getObject();
            Value listnode = null;

            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();


            URI targetPrd = null;

            Resource targetSbj = null;


            Method func = null;


            if (prd.equals(myfn)) {
                targetSbj = sbjLastSt;
                targetPrd = prdLastSt;


                String className; // class name
                String fnName; // function name
                //if (prd.equals(myfn) && isSbjBn) {
                if (prd.equals(myfn)) {
                    String functionImport = obj.stringValue();
                    int indexOfLastPoundSign = functionImport.lastIndexOf("#");

                    className = functionImport.substring("import:".length(), indexOfLastPoundSign);
                    fnName = functionImport.substring(indexOfLastPoundSign + 1);

                    func = getMethodForFunction(className, fnName);

                }
                Boolean isEndList = endList.equals(obj);
                List<String> rdfList = new ArrayList<String>();
                int statementNbr = 1;
                while (graphResult.hasNext() && !isEndList) {
                    st = graphResult.next();
                    statementNbr++;
                    // log.debug("Current statement " + statementNbr + ": " +
                    // st);
                    obj = st.getObject();
                    prd = st.getPredicate();
                    sbj = st.getSubject();
                    //mbnode = bnode.matcher(sbj.toString());
                    //log.debug(" sbjLastSt = " + targetSbj );
                    //log.debug(" prdLastSt = " + targetPrd );

                    //log.debug(" sbj = " + sbj );
                    //log.debug(" prd = " + prd );
                    //log.debug(" obj = " + obj );
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
                        stObj = (Value) func.invoke(this, rdfList, creatValue);
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

            objLastSt = st.getObject();
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



    public static Method getMethodForFunction(String className,
                                              String methodName) {

        Method method;

        Logger log = org.slf4j.LoggerFactory.getLogger(IRISailRepository.class);

        try {
            Class methodContext = Class.forName(className);
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

    public static Method getMethodForFunction(Object classInstance,
            String methodName) {

        Method method;

        Class methodContext = classInstance.getClass();
        String className = methodContext.getName();

        Logger log = org.slf4j.LoggerFactory.getLogger(IRISailRepository.class);

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

    public static String getProcessingMethodDescription(Method m) {

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