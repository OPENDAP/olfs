package opendap.semantics.IRISail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
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
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
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

        log.debug("shutDown)(): Shutting down Repository...");
        if (!isRepositoryDown()) {
            super.shutDown();
            setRepositoryDown(true);
            log.info("shutDown(): Semantic Repository Has Been Shutdown.");
        } else {
            log.info("shutDown(): Semantic Repository was already down.");
        }
        log.debug("shutDown(): Repository shutdown complete.");
    }

    public Boolean hasContext(URI uriContext, RepositoryConnection con)
            throws RepositoryException {
        Boolean existContext = false;
        RepositoryResult<Resource> contextIDs = con.getContextIDs();
        while (contextIDs.hasNext()) {
            Resource contID = contextIDs.next();
            if (contID != null && contID.equals(uriContext))
                existContext = true;
        }
        contextIDs.close();
        return existContext;
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

        findConstruct();

        // log.debug("Before running the construct rules:\n " +
        // opendap.coreServlet.Util.getMemoryReport());
        con = this.getConnection();
        // con.setAutoCommit(false); //turn off autocommit
        while (modelChanged && runNbr < runNbrMax) {
            // log.debug("AutoCommit is " +con.isAutoCommit()); //check if
            // autocommit
            runNbr++;
            modelChanged = false;
            log.debug("Applying Construct Rules. Beginning Pass #" + runNbr
                    + " \n" + opendap.coreServlet.Util.getMemoryReport());
            int ruleNumber = 0;
            for (String qstring : this.constructQuery) {
                ruleNumber++;
                queryTimes++;
                ruleStartTime = new Date().getTime();
                int stAdded = 0; // track statements added by each rule

                Vector<Statement> toAdd = new Vector<Statement>();
                String constructURL = this.constructContext.get(qstring);

                URI uriaddress = new URIImpl(constructURL);
                Resource[] context = new Resource[1];
                context[0] = uriaddress;

                String processedQueryString = convertSWRLQueryToSeasameQuery(qstring);

                log.debug("Source Query String ID: " + constructURL);
                log.debug("Source Query String: " + qstring
                        + "   Processed Query String: " + processedQueryString);

                try {
                    // log.debug("Prior to making new repository connection:\n "
                    // + opendap.coreServlet.Util.getMemoryReport());

                    GraphQuery graphQuery = con.prepareGraphQuery(
                            QueryLanguage.SERQL, processedQueryString);

                    log.info("Querying the repository. PASS #" + queryTimes
                            + " (construct rules pass #" + runNbr + ")");

                    graphResult = graphQuery.evaluate();
                    GraphQueryResult graphResultStCount = graphQuery.evaluate();
                    log.info("Completed querying. ");

                    // log.debug("After evaluating construct rules:\n " +
                    // opendap.coreServlet.Util.getMemoryReport());

                    log
                            .info("Post processing query result and adding statements ... ");

                    if (graphResult.hasNext()) {
                        modelChanged = true;

                        ValueFactory creatValue = this.getValueFactory();

                        switch (postProcessFlag) {

                        case xsString:
                            process_xsString(graphResult, creatValue, Added,
                                    toAdd, con, context);
                            log.debug("After processing xs:string:\n "
                                    + opendap.coreServlet.Util
                                            .getMemoryReport());
                            break;

                        case DropQuotes:
                            process_DropQuotes(graphResult, creatValue, Added,
                                    toAdd, con, context);
                            log.debug("After processing DropQuotes:\n "
                                    + opendap.coreServlet.Util
                                            .getMemoryReport());
                            break;

                        case RetypeTo:
                            process_RetypeTo(graphResult, creatValue, Added,
                                    toAdd, con, context);
                            log.debug("After processing RetypeTo:\n "
                                    + opendap.coreServlet.Util
                                            .getMemoryReport());
                            break;

                        case Increment:
                            process_Increment(graphResult, creatValue, Added,
                                    toAdd, con, context);
                            log.debug("After processing Increment:\n "
                                    + opendap.coreServlet.Util
                                            .getMemoryReport());
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
                    log.debug("graphqueryString: " + qstring);
                } finally {
                    if (graphResult != null) {
                        try {
                            graphResult.close();
                        } catch (QueryEvaluationException e) {
                            log
                                    .error("Caught a QueryEvaluationException! Msg: "
                                            + e.getMessage());
                        }
                    }

                }

                ruleEndTime = new Date().getTime();
                double ruleTime = (ruleEndTime - ruleStartTime) / 1000.0;
                log.debug("Cnstruct rule " + ruleNumber + " takes " + ruleTime
                        + " seconds in loop " + runNbr + "added " + stAdded
                        + " statements");
                totalStAdded = totalStAdded + stAdded;
            } // for(String qstring
            log.info("Completed pass " + runNbr + " of Construct evaluation");
            log.info("Queried the repository " + queryTimes + " times");

            /*
             * if (totalStAdded > 0) { log.debug("Committing..."); con.commit();
             * //force flushing the memory log.debug("Commit finished"); }
             */

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
                + Added.size() + " \n");

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
                    + "{contexts} rdfcache:serql_text {queries} "
                    + "using namespace "
                    + "rdfcache = <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#>";

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
                    log.debug("Adding construct to import pool: "
                            + firstValue.toString());
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

    private String getServerUrlString(URL url) {

        String baseURL = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("file")) {
            log.debug("Protocol is FILE.");

        } else if (protocol.equalsIgnoreCase("http")) {
            log.debug("Protcol is HTTP.");

            String host = url.getHost();
            String path = url.getPath();
            int port = url.getPort();

            baseURL = protocol + "://" + host;

            if (port != -1)
                baseURL += ":" + port;
        }

        log.debug("ServerURL: " + baseURL);

        return baseURL;

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

        Pattern rdfCachePattern = Pattern.compile("rdfcache:retypeTo");
        Pattern xsd2owlPattern = Pattern
                .compile("xsd2owl:increment\\(([^)]+)\\)");

        String pproces4sub2 = "\\{\\s*\\{(\\w+)\\s*\\}\\s*(.+)\\{(\\w+)\\s*\\}\\s*\\}";
        Pattern rproces4psub2 = Pattern.compile(pproces4sub2);

        String processedQueryString = queryString;
        log.info("Construct queryString: " + queryString);
        Matcher mreifStr = rproces4psub2.matcher(processedQueryString);

        Boolean hasReified = false;

        log.debug("");

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
        
        Pattern functionPattern = Pattern
                .compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\))");// fn:name(abc)
        Pattern comma = Pattern.compile(",");

        Matcher functionMatcher = functionPattern.matcher(processedQueryString);
        
        Pattern p_fn = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\))");

        Matcher m_fn = p_fn.matcher(processedQueryString);

        
        Pattern p_fn_className = Pattern.compile("(([a-z]+):([A-Za-z]+)\\(([^)]+)\\)).+using namespace.+\\2 *= *<import:([^#]+)#>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        String queryStr = processedQueryString;
        Matcher m_fn_className = p_fn_className.matcher(queryStr);

        String expand = "";
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
            log.info("Will postprocess rdfcache:retypeTo");

        } else if (xsd2owlMatcher.find()) {
            postProcessFlag = ProcessingTypes.Increment;
            String vname = xsd2owlMatcher.group(1);

            processedQueryString = xsd2owlMatcher.replaceAll(vname);

            // log.info("processedQueryString = " + processedQueryString);

        } else if (m_fn_className.find()) {

            String fullyQualifiedFunctionName;

            String rdfFunctionName = m_fn_className.group(3);
            String rdfClassName = m_fn_className.group(5);

            fullyQualifiedFunctionName = rdfClassName + "#" + rdfFunctionName;

            log.debug("fullyQualifiedFunctionName = " + fullyQualifiedFunctionName); // full name of the function
            log.debug("class_name = " + rdfClassName); // class name of the function
                                                        
            if (functionMatcher.find()) {


                log.info("Found class name    '" + rdfClassName+"'");
                log.info("Found function_name '" + rdfFunctionName+"'");

                Method myFunction = getMethodForFunction(rdfClassName, rdfFunctionName);

                if (myFunction != null) {
                    postProcessFlag = ProcessingTypes.Function;
                }

                String[] splittedStr = comma.split(functionMatcher.group(4));
                int i = 0;
                String fn = functionMatcher.group(2);
                String functionName = functionMatcher.group(3);

                //@todo  myfn and mylist are in a fixed namespace, say http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#myfn
                // the following code should be changed to reflect this.
                expand += "} " + fn + ":myfn {" + fn + ":" + functionName
                        + "} ; " + fn + ":mylist {} rdf:first {";
                for (String element : splittedStr) {
                    i++;
                    if (i < splittedStr.length) {
                        expand += element + "} ; rdf:rest {} rdf:first {";
                        // log.info("element " + i + " = " + element);
                    } else {
                        expand += element + "} ; rdf:rest {rdf:nil";
                        // log.info("element " + i + " = " + element);
                    }
                    log.info("Will postprocess fn:" + functionMatcher.group(3));
                }
                // log.info("expand = " + expand);
                // processedQueryString = functionMatcher.replaceFirst(expand);
                processedQueryString = functionMatcher.replaceFirst(expand);
                functionMatcher = functionPattern.matcher(processedQueryString);
                if (functionMatcher.find()) {
                    splittedStr = comma.split(functionMatcher.group(4));
                    int j = 0;
                    expand = "";
                    fn = functionMatcher.group(2);
                    functionName = functionMatcher.group(3);

                    expand += "} " + fn + ":myfn {" + fn + ":" + functionName
                            + "} ; " + fn + ":mylist {} rdf:first {";
                    for (String element : splittedStr) {
                        j++;
                        if (j < splittedStr.length) {
                            expand += element + "} ; rdf:rest {} rdf:first {";
                            // log.info("element " + j + " = " + element);
                        } else {
                            expand += element + "} ; rdf:rest {rdf:nil";
                            // log.info("element " + j + " = " + element);
                        }
                    }
                    processedQueryString = functionMatcher.replaceFirst(expand);
                }
            }

        }

        // log.info("processedQueryString = " + processedQueryString);
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

        String pproces3subsub1 = "<http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#reTypeTo> <([^>]+)>";
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
     * Finds and returns all imports/seeAlso statement in the repository.
     * 
     * @return Stack<String>
     */
    public Vector<String> findImports() {
        TupleQueryResult result = null;
        RepositoryConnection con = null;
        List<String> bindingNames;
        Vector<String> importID = new Vector<String>();
        try {
            con = this.getConnection();
            String queryString = "SELECT DISTINCT ontfile "
                    + "FROM "
                    + "{} owl:imports {ontfile}, [ {ontfile} rdfcache:isContainedBy {collection} ] "
                    + "WHERE collection=NULL "
                    + "UNION "
                    + "SELECT DISTINCT ontfile FROM "
                    + "{} rdfs:seeAlso {ontfile}, [ {ontfile} rdfcache:isContainedBy {collection} ] "
                    + "WHERE collection=NULL "
                    + "UNION "
                    + "SELECT DISTINCT ontfile FROM "
                    + "{} rdfcache:isContainedBy {ontfile}, [ {ontfile} rdfcache:isContainedBy {collection} ] "
                    + "WHERE collection=NULL "
                    + "using namespace "
                    + "rdfs = <http://www.w3.org/2000/01/rdf-schema#>, "
                    + "owl = <http://www.w3.org/2002/07/owl#>,"
                    + "rdfcache = <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#>";

            log.debug("queryString: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                bindingNames = result.getBindingNames();
                // log.debug("There are " + bindingNames.size()
                // + " binding names for 'import'");

                while (result.hasNext()) {
                    BindingSet bindingSet = (BindingSet) result.next();
                    // Value firstValue
                    // =bindingSet.getValue((String)bindingNames.get(0));

                    Value firstValue = bindingSet.getValue("ontfile");
                    if (!importID.contains(firstValue.stringValue())
                            && !this.downService.containsKey(firstValue
                                    .stringValue())) {
                        importID.add(firstValue.stringValue());
                    }
                    log.debug("Add into import pool: "
                            + firstValue.stringValue());

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

        log.info("Number of imports:  " + importID.size());
        return importID;
    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     */
    public ByteArrayOutputStream transformXSD(String inURI)
            throws SaxonApiException {
        log.debug("In transformXSD");
        String transformFileName = resourceDir + "xsl/xsd2owl.xsl";

        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(new File(
                transformFileName)));
        // XsltExecutable exp = comp.compile(new StreamSource(new
        // File("/data/benno/xslt/xsd2owl.xsl")));
        XdmNode source = proc.newDocumentBuilder().build(
                new StreamSource(inURI));
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
     * Transforms a schema file into an xml. Compile and execute a simple
     * transformation that applies a stylesheet to an input stream, and
     * serializing the result to a file trans.xml which will be in turn added
     * into repository. This method is called by update.
     * 
     * @param inURI
     * @throws SaxonApiException
     */
    public void transformXSD2File(String inURI) throws SaxonApiException {
        log.debug("In transformXSD2File");
        String transformFileName = resourceDir + "xsl/xsd2owl.xsl";

        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(new File(
                transformFileName)));
        // XsltExecutable exp = comp.compile(new StreamSource(new
        // File("/data/benno/xslt/xsd2owl.xsl")));

        XdmNode source = proc.newDocumentBuilder().build(
                new StreamSource(inURI));

        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputFile(new File("trans.xml"));
        XsltTransformer trans = exp.load();
        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();
        log.debug("Output written to trans.xml");
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
            Timestamp ltmodsql = new Timestamp(ltmod);
            String ltmodstrraw = ltmodsql.toString();
            ltmodstr = ltmodstrraw.substring(0, 10) + "T"
                    + ltmodstrraw.substring(11, 19) + "Z";
        } catch (MalformedURLException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getLocalizedMessage());
        } catch (IOException e) {
            log.error("Caught an IOException! Msg: " + e.getMessage(), e);
        }
        return ltmodstr;
    }

    /**
     * Checks and returns last modified time of a context (URI) via querying
     * against the repository on contexts.
     * 
     * @param urlstring
     */
    public String chkLTMODContext(String urlstring) {
        TupleQueryResult result = null;
        String ltmodstr = "";
        URI uriaddress = new URIImpl(urlstring);
        Resource[] context = new Resource[1];
        context[0] = (Resource) uriaddress;
        RepositoryConnection con = null;

        String queryString = "SELECT DISTINCT x, y FROM CONTEXT <"
                + uriaddress
                + "> {x} <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#last_modified> {y} "
                + "where x=<" + uriaddress + ">";

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
                valueOfY = (Value) bindingSet.getValue("y");
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
     * Returns a Hash containing last modified time of a context (URI) from the
     * repository.
     */
    public HashMap<String, String> getLMT() {
        TupleQueryResult result = null;
        String ltmodstr = "";
        String idstr = "";
        HashMap<String, String> idltm = new HashMap<String, String>();
        RepositoryConnection con = null;
        String queryString = "SELECT DISTINCT id, lmt "
                + "FROM "
                + "{cd} wcs:Identifier {id}; "
                + "rdfs:isDefinedBy {doc} rdfcache:last_modified {lmt} "
                + "using namespace "
                + "rdfcache = <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#>, "
                + "wcs= <http://www.opengis.net/wcs/1.1#>";

        try {
            con = this.getConnection();

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfID = null;
            Value valueOfLMT;

            while (result.hasNext()) {
                bindingSet = (BindingSet) result.next();

                valueOfLMT = (Value) bindingSet.getValue("lmt");
                ltmodstr = valueOfLMT.stringValue();

                valueOfID = (Value) bindingSet.getValue("id");
                idstr = valueOfID.stringValue();

                idltm.put(idstr, ltmodstr);

                // log.debug("ID:" + valueOfID.stringValue());
                // log.debug("LMT:" + valueOfLMT.stringValue());

            }
        } catch (QueryEvaluationException e) {
            log.error("Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } catch (Throwable e) {
            log.error(e.getMessage());
        } finally {
            try {
                result.close();
                con.close();
            } catch (QueryEvaluationException e) {
                log.error("Caught a QueryEvaluationException! Msg: "
                        + e.getMessage());
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException! Msg: "
                        + e.getMessage());
            }
        }

        return idltm;
    }

    /*
     * Print all statements in the repository.
     */
    public void printRDF() {
        String queryString = "SELECT DISTINCT x, y FROM {x} p {y} ";
        TupleQueryResult result = null;
        Stack<BindingSet> importID = new Stack<BindingSet>();
        RepositoryConnection con = null;
        try {
            con = this.getConnection();

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            while (result.hasNext()) {
                BindingSet bindingSet = (BindingSet) result.next();
                importID.push(bindingSet);

                Value valueOfX = (Value) bindingSet.getValue("x");
                Value valueOfY = (Value) bindingSet.getValue("y");
                log.debug("X:" + valueOfX.stringValue());
                log.debug("Y:" + valueOfY.stringValue());
            }
        } catch (QueryEvaluationException e) {
            log.error(e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            try {
                result.close();
                con.close();
            } catch (Exception e) {
                log.error("Caught an Exception! Msg: " + e.getMessage());

            }
        }

        log.info("Number of RDF:  " + importID.size());
    }

    /**
     * Print the total number of statements within a context.
     * 
     * @param urlstring
     */
    public void printRDFContext(String urlstring) {
        URI uriaddress = new URIImpl(urlstring);
        TupleQueryResult result = null;
        Resource[] context = new Resource[1];
        context[0] = (Resource) uriaddress;
        RepositoryConnection con = null;

        String queryString = "SELECT DISTINCT x, y FROM CONTEXT <" + uriaddress
                + "> {x} p {y} ";

        try {
            con = this.getConnection();

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            log.debug(urlstring + " has Number of RDF? " + con.size(context));
        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            try {
                result.close();
                con.close();
            } catch (Exception e) {
                log.error("Caught an Exception! Msg: " + e.getMessage());
            }
        }

    }

    /**
     * Print last modified time of a context. This method takes a URL string as
     * a context and prints last modified time of it.
     */
    public void printLTMODContext(String urlstring) {
        URI uriaddress = new URIImpl(urlstring);
        Resource[] context = new Resource[1];
        context[0] = (Resource) uriaddress;
        TupleQueryResult result = null;
        Stack<BindingSet> importID = new Stack<BindingSet>();
        RepositoryConnection con = null;
        String queryString = "SELECT DISTINCT x, y FROM CONTEXT <"
                + uriaddress
                + "> {x} <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#last_modified> {y} "
                + "where x=<" + uriaddress + ">";

        try {
            con = this.getConnection();

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();
            BindingSet bindingSet;
            Value valueOfY;
            Value valueOfX;
            while (result.hasNext()) {
                bindingSet = result.next();
                importID.push(bindingSet);

                valueOfY = bindingSet.getValue("y");
                valueOfX = bindingSet.getValue("x");
                log.info("context: " + valueOfX.stringValue());
                log.info("last modified time: " + valueOfY.stringValue());
            }

        } catch (QueryEvaluationException e) {
            log.error("Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught an RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught an MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            try {
                result.close();
                con.close();
            } catch (Exception e) {
                log.error("Caught an Exception! Msg: " + e.getMessage());
            }
        }

        // log.debug("Number of LTMOD? " + importID.size());
    }

    /**
     * Return true if import context is newer.
     * 
     * @param importURL
     * @return Boolean
     */
    public Boolean olderContext(String importURL) {
        Boolean oldLMT = false;

        String oldltmod = this.chkLTMODContext(importURL); // LMT from http
        // header

        // String oldltmod = this.chkLMTContext(importURL); // LMT in owl
        // document

        if (oldltmod.isEmpty()) {
            oldLMT = true;
            return oldLMT;
        }
        String oltd = oldltmod.substring(0, 10) + " "
                + oldltmod.substring(11, 19);
        String ltmod = this.getLTMODContext(importURL);

        String ltd = ltmod.substring(0, 10) + " " + ltmod.substring(11, 19);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        Date ltdparseDate;
        try {
            ltdparseDate = dateFormat.parse(ltd);

            log.debug("lastmodified " + ltdparseDate.toString());
            Date oldltdparseDate = dateFormat.parse(oltd);
            log.debug("oldlastmodified " + oldltdparseDate.toString());

            if (ltdparseDate.compareTo(oldltdparseDate) > 0) {// if newer
                // context

                log.info("Import context is newer: " + importURL);
                oldLMT = true;
            }
        } catch (ParseException e) {
            log.error("Caught an ParseException! Msg: " + e.getMessage());

        }
        return oldLMT;
    }

    /**
     * Set last_modified_time of the URI in the repository.
     * 
     * @param importURL
     */
    public void setLTMODContext(String importURL, RepositoryConnection con) {
        String pred = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#last_modified";

        if (!this.imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            String ltmod = this.getLTMODContext(importURL);
            // log.debug("lastmodified " + ltmod);
            ValueFactory f = this.getValueFactory();
            URI s = f.createURI(importURL);
            URI p = f.createURI(pred);
            URI cont = f.createURI(importURL);
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

    /**
     * Delete last_modified_time of the URI in the repository.
     * 
     * @param importURL
     */
    public void deleteLTMODContext(String importURL, RepositoryConnection con) {
        String pred = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#last_modified";

        if (!this.imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            String ltmod = this.getLTMODContext(importURL);
            // log.debug("lastmodified " + ltmod);
            ValueFactory f = this.getValueFactory();
            URI s = f.createURI(importURL);
            URI p = f.createURI(pred);
            URI cont = f.createURI(importURL);
            URI sxd = f.createURI("http://www.w3.org/2001/XMLSchema#dateTime");
            Literal o = f.createLiteral(ltmod, sxd);

            try {

                con.remove((Resource) s, p, (Value) o, (Resource) cont);

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }

    /**
     * Set IsContainedBy statement for the importURI in the repository.
     * 
     * @param importURL
     * @param CollectionURL
     */
    public void setIsContainedBy(String importURL, String CollectionURL) {
        String pred = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#isContainedBy";

        if (!this.imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            String ltmod = this.getLTMODContext(importURL);
            // log.debug("lastmodified " + ltmod);
            ValueFactory f = this.getValueFactory();
            URI s = f.createURI(importURL);
            URI p = f.createURI(pred);
            URI cont = f.createURI(importURL);
            URI o = f.createURI(CollectionURL);

            try {
                RepositoryConnection con;

                con = this.getConnection();

                con.add((Resource) s, p, (Value) o, (Resource) cont);

                log.debug("Added to the repository " + "<" + s + "> " + "<" + p
                        + "> " + "<" + o + "> ");

                con.close();

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }

    /**
     * Delete last_modified_time of the URI in the repository.
     * 
     * @param importURL
     */
    public void deleteIsContainedBy(String importURL, String CollectionURL) {
        String pred = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#isContainedBy";

        if (!this.imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            String ltmod = this.getLTMODContext(importURL);
            // log.debug("lastmodified " + ltmod);
            ValueFactory f = this.getValueFactory();
            URI s = f.createURI(importURL);
            URI p = f.createURI(pred);
            URI cont = f.createURI(importURL);
            URI o = f.createURI(CollectionURL);

            try {
                RepositoryConnection con;

                con = this.getConnection();

                con.remove((Resource) s, p, (Value) o, (Resource) cont);

                log.debug("Deleted to the repository " + "<" + s + "> " + "<"
                        + p + "> " + "<" + o + "> ");
                con.close();

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }

    /**
     * Update repository. This method recursively import any imports and seealso
     * owl/rdf/xsd documents refered in an owl/rdf/xsd document. It returns true
     * if update successes. If the remote server is down, the file is not found,
     * or the file is not modified, no update is performed for this URL. If
     * newer update is available, delete the old statements associated with the
     * context. Note, this method should be called after adding a document into
     * the repository.
     * 
     * @return Boolean
     */
    public Boolean update() {
        Boolean update = false;
        URI uriaddress;
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();
        log.info("Finding imports/seeAlso first time ...");
        Vector<String> importID = this.findImports();
        String importURL = "";
        Vector<String> importInRepository = new Vector<String>();
        // retrieve context
        RepositoryResult<Resource> contextID;
        try {
            RepositoryConnection con = this.getConnection();
            contextID = con.getContextIDs();
            int contextTol = 0;
            if (!contextID.hasNext()) {
                log.warn("No Contexts found!");
            } else {
                while (contextID.hasNext()) {
                    String ctstr = contextID.next().toString();
                    // log.info("Context: " + ctstr);
                    importInRepository.add(ctstr);
                    printLTMODContext(ctstr);
                    contextTol++;
                }
            }
            contextID.close(); // needed to release resources
            // log.info("Found " + contextTol + " Contexts");

            int i = 0;
            int notimport = 0;

            while (importID.size() != 0
                    && importID.size() > (this.imports.size() + this.downService
                            .size())) {
                log.debug("importID.size=" + importID.size() + " imports.size="
                        + this.imports.size() + " downService.size="
                        + this.downService.size());
                notimport = 0;
                while (!importID.isEmpty()) {
                    importURL = importID.remove(0).toString();

                    log.debug("Checking import URL: " + importURL);
                    URL myurl = new URL(importURL);

                    HttpURLConnection hc = (HttpURLConnection) myurl
                            .openConnection();
                    log.debug("Connected to import URL: " + importURL);

                    int rsCode = -1;
                    try {
                        rsCode = hc.getResponseCode();
                    } catch (IOException e) {
                        log.error("Unable to get HTTP status code for "
                                + importURL + " Caught IOException! Msg: "
                                + e.getMessage());
                    }
                    log.debug("Got HTTP status code: " + rsCode);

                    if (this.downService.containsValue(importURL)
                            && this.downService.get(importURL)) {
                        log.error("Server error, Skip " + importURL);
                    } else if (rsCode == -1) {
                        log
                                .error("Unable to get an HTTP status code for resource "
                                        + importURL + " WILL NOT IMPORT!");
                        this.downService.put(importURL, true);

                    } else if (rsCode > 500) { // server error
                        if (rsCode == 503) {
                            log.error("Error 503 Skipping " + importURL);
                            this.downService.put(importURL, true);
                        } else
                            log
                                    .error("Server Error? Received HTTP Status code "
                                            + rsCode + " for URL: " + importURL);

                    } else if (rsCode == 304) {
                        log.info("Not modified " + importURL);
                        this.downService.put(importURL, true);
                    } else if (rsCode == 404) {
                        log.error("Received HTTP 404 status for resource: "
                                + importURL);
                        this.downService.put(importURL, true);
                    } else {

                        log.debug("Import URL appears valid ( " + importURL
                                + " )");
                        if (this.imports.contains(importURL)) {
                            log.debug("imports has: " + importURL);

                            if (olderContext(importURL)) {// if new update
                                // available delete
                                // old one

                                log
                                        .info("lastmodified is newer than oldlastmodified, deleting the old context!");
                                URI context2remove = new URIImpl(importURL);
                                con.clear((Resource) context2remove);
                                deleteLTMODContext(importURL, con); // delete
                                                                    // last
                                                                    // modified
                                                                    // time of
                                                                    // the
                                                                    // context

                                // deleteIsContainedBy(importURL,
                                // CollectionURL); //need some work here!!!
                                log.info("finished deleting " + importURL);
                                // con.commit(); //force transaction
                                /*
                                 * leave for adding in next turn URL url = new
                                 * URL(importURL); uriaddress = new
                                 * URIImpl(importURL); log.info("Importing
                                 * "+importURL); con.add(url, importURL,
                                 * RDFFormat.RDFXML, (Resource) uriaddress);
                                 * setLTMODContext(importURL, con); //set last
                                 * modified time for the context
                                 * //setIsContainedBy(importURL, CollectionURL);
                                 * //need some work here!!! log.info("Finished
                                 * Importing "+importURL); update = true;
                                 */
                                importID.add(importURL); // put back into the
                                // add list
                            } else {
                                log.info("Skip old URL: " + importURL);
                            }
                        }
                        if (!this.imports.contains(importURL)) { // not in
                                                                    // the
                                                                    // import
                                                                    // list yet

                            if (!importInRepository.contains(importURL)) {
                                log.debug("Repository does not have: "
                                        + importURL);

                                String urlsufix = importURL.substring(
                                        (importURL.length() - 4), importURL
                                                .length());

                                setLTMODContext(importURL, con);
                                if (urlsufix.equals(".owl")
                                        || urlsufix.equals(".rdf")) {

                                    uriaddress = new URIImpl(importURL);

                                    URL url = new URL(importURL);
                                    log.info("Importing URL " + url);
                                    con.add(url, importURL, RDFFormat.RDFXML,
                                            (Resource) uriaddress);
                                    setLTMODContext(importURL, con); // set
                                                                        // last
                                                                        // modified
                                                                        // time
                                                                        // the
                                                                        // context

                                    // setIsContainedBy(importURL,
                                    // CollectionURL); //need some work here!!!
                                    update = true;
                                    log.info("Finished importing URL " + url);

                                } else if (importURL.substring(
                                        (importURL.length() - 4),
                                        importURL.length()).equals(".xsd")) {

                                    try {
                                        uriaddress = new URIImpl(importURL);

                                        ByteArrayInputStream inStream;
                                        log.info("Transforming URL "
                                                + importURL);
                                        inStream = new ByteArrayInputStream(
                                                this.transformXSD(importURL)
                                                        .toByteArray());
                                        log.info("Finished transforming URL "
                                                + importURL);
                                        log.debug("Importing URL " + importURL);
                                        con.add(inStream, importURL,
                                                RDFFormat.RDFXML,
                                                (Resource) uriaddress);
                                        setLTMODContext(importURL, con); // set
                                                                            // last
                                                                            // modified
                                                                            // time
                                                                            // for
                                                                            // the
                                                                            // context

                                        // setIsContainedBy(importURL,
                                        // CollectionURL); //need some work
                                        // here!!!
                                        update = true;
                                        log.debug("Finished importing URL "
                                                + importURL);
                                    } catch (SaxonApiException e) {
                                        log
                                                .error("Caught an SaxsonException! Msg: "
                                                        + e.getMessage());
                                    }
                                } else {
                                    notimport++;
                                    log
                                            .info("Not importing URL = "
                                                    + importURL);
                                    log.info("Total not imported Nr = "
                                            + notimport);
                                }
                            } else {
                                log.info("Repository has: " + importURL);
                            }
                            this.imports.add(importURL); // Appends the
                            // import/seeAlso to
                            // the list of
                            // finished

                        } // if (! this.imports.contains(importURL))
                    } // if (this.downService.get(importURL))
                    // con.commit();
                }// while (!importID.empty()

                i++;
                int findimportNbr = i + 1;
                log.info("Finding imports/seeAlso " + findimportNbr
                        + "times ...");
                importID = this.findImports();

                log.debug("Update times = " + i);

            }// while (importID.size() != this.imports.size()

        } catch (RepositoryException e) {
            log.error("Caught RepositoyException! Msg: " + e.getMessage());

        } catch (MalformedURLException e) {
            log.error("Caught MalformedURLException! Msg: " + e.getMessage());

        } catch (IOException e) {
            log.error("update() - Failed to import " + importURL
                    + " Caught IOException! Msg: " + e.getMessage());

        } catch (RDFParseException e) {
            log.error("Caught RDFParseException! Msg: " + e.getMessage());
        }
        inferEndTime = new Date().getTime();
        double inferTime = (inferEndTime - inferStartTime) / 1000.0;
        log.debug("Import takes " + inferTime + " seconds");
        return update;
    } // public Boolean update

    /**
     * Updates repository from a given file and baseURI. This method takes a
     * local owl/rdf file containing rdf statements and imports as starting
     * point. It recursively add all imports and seealso find in import
     * documents. It returns true if update successes.
     */
    // File infile = new File(file);
    // URI uriaddress = new URIImpl(baseURI);
    public Boolean updateFromFile(File infile, URI uriaddress) {
        Boolean update = false;
        try {

            RepositoryConnection con = this.getConnection();

            con.add(infile, uriaddress.toString(), RDFFormat.RDFXML,
                    (Resource) uriaddress);

            update = true;

            con.close();
        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (RDFParseException e) {
            log.error("Caught RDFParseException! Msg: " + e.getMessage());
        } catch (IOException e) {
            log.error("updateFromFile() - Failed to add " + uriaddress
                    + " Caught IOException! Msg: " + e.getMessage());
        }

        Vector<String> importID = this.findImports();

        try {
            RepositoryConnection con = this.getConnection();

            while (importID.size() != this.imports.size()) {
                log.info("importID.size=" + importID.size()
                        + " owlse2.imports.size=" + this.imports.size());
                while (!importID.isEmpty()) {
                    String importURL = importID.remove(0).toString();

                    if (!this.imports.contains(importURL)) { // not in the
                                                                // repository
                                                                // yet

                        String urlsufix = importURL.substring((importURL
                                .length() - 4), importURL.length());
                        log.debug(importURL);

                        this.setLTMODContext(importURL, con);
                        if (urlsufix.equals(".owl") || urlsufix.equals(".rdf")) {
                            uriaddress = new URIImpl(importURL);

                            try {
                                URL url = new URL(importURL);
                                log.info("Importing URL " + url);
                                con.add(url, importURL, RDFFormat.RDFXML,
                                        (Resource) uriaddress);
                                log.info("Finished importing URL " + url);
                            } catch (Throwable e) {
                                log.error(e.getMessage());
                            }
                        } else if (importURL.substring(
                                (importURL.length() - 4), importURL.length())
                                .equals(".xsd")) {
                            log.info("XSD: " + importURL);
                            try {
                                uriaddress = new URIImpl(importURL);

                                ByteArrayInputStream inStream;
                                log.info("Transforming URL " + importURL);
                                inStream = new ByteArrayInputStream(this
                                        .transformXSD(importURL).toByteArray());
                                log.info("Finished transforming URL "
                                        + importURL);
                                log.debug("Importing URL " + importURL);
                                con.add(inStream, importURL, RDFFormat.RDFXML,
                                        (Resource) uriaddress);
                                update = true;
                                log
                                        .debug("Finished importing URL "
                                                + importURL);
                            } catch (SaxonApiException e) {
                                log.error("Caught an SaxsonException! Msg: "
                                        + e.getMessage());
                            } catch (IOException e) {
                                log.error("updateFromFile() - Failed to add "
                                        + uriaddress
                                        + " Caught IOException! Msg: "
                                        + e.getMessage());

                            } catch (RDFParseException e) {
                                log.error("Caught RDFParseException! Msg: "
                                        + e.getMessage());
                            }

                        } else {

                            log.warn("Not importing " + importURL);
                        }
                        this.imports.add(importURL); // Appends the specified
                        // element to the end

                    }// if (! owlse2.imports
                }// while (!importID.empty()

                // owlse2.printRDF ();
                // find all import and seealso
                importID = this.findImports();

            }

            log.info("importID.size=" + importID.size()
                    + " owlse2.imports.size=" + this.imports.size());

            con.close();
        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());

        }

        return update;
    } // public Boolean updateFromFile

    /**
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public HashMap<String, Vector<String>> getCoverageIDServerURL()
            throws RepositoryException, MalformedQueryException,
            QueryEvaluationException {
        TupleQueryResult result = null;
        HashMap<String, Vector<String>> coverageIDServer = new HashMap<String, Vector<String>>();

        String queryString = "SELECT coverageurl,coverageid "
                + "FROM "
                + "{} wcs:CoverageDescription {coverageurl} wcs:Identifier {coverageid} "
                + "USING NAMESPACE "
                + "wcs = <http://www.opengis.net/wcs/1.1#>";
        RepositoryConnection con = getConnection();
        log.debug("query coverage ID and server URL: \n" + queryString);
        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                queryString);

        result = tupleQuery.evaluate();
        log.debug("Qresult: " + result.hasNext());
        List<String> bindingNames = result.getBindingNames();
        // log.debug(bindingNames.probeServletContext());
        while (result.hasNext()) {
            BindingSet bindingSet = (BindingSet) result.next();
            // log.debug(bindingSet.probeServletContext());
            Vector<String> coverageURL = new Vector<String>();

            if (bindingSet.getValue("coverageid") != null
                    && bindingSet.getValue("coverageurl") != null) {

                Value valueOfcoverageid = (Value) bindingSet
                        .getValue("coverageid");
                Value valueOfcoverageurl = (Value) bindingSet
                        .getValue("coverageurl");
                coverageURL.addElement(valueOfcoverageurl.stringValue());
                // log.debug("coverageid:");
                // log.debug(valueOfcoverageid.stringValue());
                // log.debug("coverageurl:");
                log.debug(valueOfcoverageurl.stringValue());
                if (coverageIDServer.containsKey(valueOfcoverageid
                        .stringValue()))
                    coverageIDServer.get(valueOfcoverageid.stringValue())
                            .addElement(valueOfcoverageurl.stringValue());
                else
                    coverageIDServer.put(valueOfcoverageid.stringValue(),
                            coverageURL);

            }
        }
        con.close();
        return coverageIDServer;

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

        Pattern http = Pattern.compile("^http://");
        Pattern bnode = Pattern.compile("^_:");
        Pattern endlist = Pattern.compile("#nil");

        FunctionTypes functionTypeFlag = FunctionTypes.None;

        while (graphResult.hasNext()) {
            Statement st = graphResult.next();

            // log.debug("Current statement: " + st);

            Value obj = st.getObject();

            URI prd = st.getPredicate();
            Resource sbj = st.getSubject();

            URI objUri = null;
            URI targetPrd = prd;
            URI sbjUri = null;

            Matcher mobjhttp = http.matcher(obj.stringValue());
            if (mobjhttp.find()) {
                objUri = new URIImpl(obj.toString());

            }
            Matcher msbjhttp = http.matcher(sbj.stringValue());
            if (msbjhttp.find()) {
                sbjUri = new URIImpl(sbj.toString());

            }

            Matcher mbnode = bnode.matcher(sbj.toString());
            Resource targetSbj = null;
            Boolean isSbjBn = false;
            isSbjBn = mbnode.find();
            Boolean isObjBn = false;
            Matcher objbnode = bnode.matcher(obj.toString());
            isObjBn = objbnode.find();

            Method func = null;

            if (!isSbjBn && isObjBn) {

                targetSbj = sbj;

                String className; // class name
                String fnName; // function name

                Matcher mendlist = endlist.matcher(obj.stringValue());
                Boolean isEndList = false;
                isEndList = mendlist.find();
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
                    mbnode = bnode.matcher(sbj.toString());

                    isSbjBn = mbnode.find();
                    // log.debug("prd = " + prd.stringValue() );

                    // @todo This is supposed to be a full URI match and not a local name match!!!
                    if (prd.getLocalName().equals("myfn") && isSbjBn) {

                        String functionImport =  obj.stringValue();
                        int indexOfLastPoundSign = functionImport.lastIndexOf("#");
                        
                        className  =  functionImport.substring("import:".length(),indexOfLastPoundSign);
                        fnName = functionImport.substring(indexOfLastPoundSign + 1);

                        func = getMethodForFunction(className, fnName);

                    }

                    if (isSbjBn && prd.getLocalName().equals("first")) {
                        String elementValue = obj.stringValue();
                        rdfList.add(elementValue);
                    }

                    mendlist = endlist.matcher(obj.stringValue());
                    isEndList = mendlist.find();

                }

                if (func != null) {
                    Value stObj = null;
                    try {
                        stObj = (Value) func.invoke(this, rdfList, creatValue);
                        st = new StatementImpl(targetSbj, targetPrd, stObj);
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
                    log
                            .warn("Process Function failed: No processing function found.");
                }

            } else if (!isSbjBn && !isObjBn) {
                targetSbj = sbj;
                // log.debug("original st in Join = " + st.toString());
            }
            // log.debug("st to add = " + st.toString());
            toAdd.add(st);
            Added.add(st);
            con.add(st, context); // add fn created st
        } // while (graphResult.hasNext())
        log.debug("After processing fn: " + toAdd.size()
                + " statements are added.\n ");
    }

    /***************************************************************************
     * function join to concatenate strings
     * 
     * @param RDFList
     * @param createValue
     * @return
     */
    public static Value join(List<String> RDFList, ValueFactory createValue) {
        int i = 0;
        boolean joinStrIsURL = false;
        String targetObj = "";
        if (RDFList.get(1).startsWith("http://")) {
            joinStrIsURL = true;
        }
        for (i = 1; i < RDFList.size() - 1; i++) {
            targetObj += RDFList.get(i) + RDFList.get(0); // rdfList.get(0) +
            // separator
            // log.debug("Component("+i+")= " + RDFList.get(i));
        }

        targetObj += RDFList.get(i); // last component no separator

        Value stObjStr;
        if (joinStrIsURL) {
            stObjStr = createValue.createURI(targetObj);
        } else {
            stObjStr = createValue.createLiteral(targetObj);
        }

        return stObjStr;
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
                method = methodContext.getMethod(methodName, List.class,
                        ValueFactory.class);

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


    /***************************************************************************
     * function getWcsID
     *
     * @param RDFList
     * @param createValue
     * @return
     */
    /*
    public Value getWcsId(List<String> RDFList, ValueFactory createValue) {

        String targetObj = "";

        targetObj = RDFList.get(0); // rdf list has only one element
        targetObj = getWcsIdString(targetObj);
        Value stObjStr;
        stObjStr = createValue.createLiteral(targetObj);
        return stObjStr;
    }
    */

    /**
     * Build a wcs:Identifier for the coverage dataset described by the
     * datasetUrl.
     *
     * @param datasetUrl
     * @return A valid and unique to this service wcs:Identifier String for the
     *         coverage dataset
     */
    /*
    public String getWcsIdString(String datasetUrl) {

        String wcsID = "FAILED_TO_BUILD_WCS_ID";

        try {
            int i;
            String serverURL, serverID;
            URL dsu = new URL(datasetUrl);

            serverURL = getServerUrlString(dsu);

            log.debug("getWcsIdString(): serverURl is " + serverURL);

            if (serverIDs.containsKey(serverURL)) {
                // get server prefix
                serverID = serverIDs.get(serverURL);
                log
                        .debug("getWcsIdString(): serverURL already in use, will reuse serverID '"
                                + serverID + "'");
            } else {
                serverID = "S" + (serverIDs.size() + 1) + "";
                // Generate service prefix
                // Store service prefix.
                serverIDs.put(serverURL, serverID);
                log
                        .debug("getWcsIdString(): New serverURL! Created new serverID '"
                                + serverID + "'");

            }

            // Build wcsID
            if (!wcsIDs.containsKey(datasetUrl)) {
                // add wcs:Identifier to MAP
                wcsID = serverID
                        + datasetUrl.substring(serverURL.length(), datasetUrl
                                .length());
                log
                        .debug("getWcsIdString(): Dataset had no existing wcsID, adding wcsID: "
                                + wcsID + " for dataset: " + datasetUrl);
                wcsIDs.put(datasetUrl, wcsID);
            } else {
                wcsID = wcsIDs.get(datasetUrl);
                log
                        .debug("getWcsIdString(): Dataset already has a wcsID, returning wcsID: "
                                + wcsID + " for dataset: " + datasetUrl);
            }

        } catch (MalformedURLException e) {
            log.error("Cannot Build wcs:Identifier from URL " + datasetUrl
                    + " error msg: " + e.getMessage());
        }

        return wcsID;
    }
    */

    /*
    public void updateIdCaches() {

        HashMap<String, Vector<String>> coverageIDServer;

        log
                .debug("Updating datasetUrl/wcsID and datasetUrl/serverID HashMap objects.");
        try {

            coverageIDServer = getCoverageIDServerURL();

            String serverUrl, serviceID, localId;

            for (String coverageID : coverageIDServer.keySet()) {
                log.debug("CoverageID: " + coverageID);
                Vector<String> datasetUrls = coverageIDServer.get(coverageID);
                for (String datasetUrl : datasetUrls) {

                    log.debug("    datasetUrl: " + datasetUrl);

                    serverUrl = getServerUrlString(new URL(datasetUrl));
                    log.debug("    serverUrl:  " + serverUrl);

                    localId = datasetUrl.substring(serverUrl.length(),
                            datasetUrl.length());
                    log.debug("    localID:    " + localId);

                    serviceID = coverageID.substring(0, coverageID
                            .indexOf(localId));
                    log.debug("    serviceID:     " + serviceID);

                    if (!serverIDs.containsKey(serverUrl)) {
                        log.debug("Adding to ServiceIDs");
                        serverIDs.put(serverUrl, serviceID);
                    } else if (serviceID.equals(serverIDs.get(serverUrl))) {
                        log.info("The serverURL: " + serverUrl
                                + " is already mapped to " + "the serviceID: "
                                + serviceID + " No action taken.");
                    } else {
                        String msg = "\nOUCH! The semantic repository contains multiple serviceID strings "
                                + "for the same serverURL. This may lead to one of the serviceID's being "
                                + "reassigned. This would lead to resources being attributed to the "
                                + "wrong server/service.\n";
                        msg += "serverUrl: " + serverUrl + "\n";
                        msg += "  serviceID(repository) : " + serviceID + "\n";
                        msg += "  serviceID(in-memory):   "
                                + serverIDs.get(serverUrl) + "\n";

                        log.error(msg);

                    }

                    if (!wcsIDs.containsKey(datasetUrl)) {
                        log.debug("Adding to datasetUrl/coverageID to Map");
                        wcsIDs.put(datasetUrl, coverageID);
                    } else if (coverageID.equals(wcsIDs.get(datasetUrl))) {
                        log.info("The datasetUrl: " + datasetUrl
                                + " is already mapped to " + "the coverageID: "
                                + coverageID + " No action taken.");
                    } else {
                        String msg = "\nOUCH! The semantic repository contains multiple coverageID strings "
                                + "for the same datasetUrl. This may lead to one of the coverageID's being "
                                + "reassigned. This would lead to resources being attributed to the "
                                + "wrong server/service.\n";

                        msg += "datasetUrl: " + datasetUrl + "\n";
                        msg += "  coverageID(repository) : " + coverageID
                                + "\n";
                        msg += "  coverageID(in-memory):   "
                                + wcsIDs.get(datasetUrl) + "\n";

                        log.error(msg);
                    }

                    // private ConcurrentHashMap<String, String> serverIDs = new
                    // ConcurrentHashMap<String,String>();
                    // private ConcurrentHashMap<String, String> wcsIDs = new
                    // ConcurrentHashMap<String,String>();

                }
            }

        } catch (RepositoryException e) {

            log
                    .error("getCoverageIDServerURL(): Caught RepositoryException. msg: "
                            + e.getMessage());
        } catch (MalformedQueryException e) {
            log
                    .error("getCoverageIDServerURL(): Caught MalformedQueryException. msg: "
                            + e.getMessage());
        } catch (QueryEvaluationException e) {
            log
                    .error("getCoverageIDServerURL(): Caught QueryEvaluationException. msg: "
                            + e.getMessage());
        } catch (MalformedURLException e) {
            log
                    .error("getCoverageIDServerURL(): Caught MalformedURLException. msg: "
                            + e.getMessage());
        }

    }
    */

}