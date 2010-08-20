package opendap.semantics.IRISail;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 21, 2010
 * Time: 4:57:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryUtility {

    private static Logger log = LoggerFactory.getLogger(RepositoryUtility.class);

    public static final String internalStartingPoint         = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl";
    public static final String rdfCacheNamespace             = internalStartingPoint+"#";


    public static final String lastModifiedContext           = "last_modified";
    public static final String lastModifiedContextUri        = rdfCacheNamespace + lastModifiedContext;

    public static final String cacheContext                  = "cachecontext";
    public static final String cacheContextUri               = rdfCacheNamespace + cacheContext;

    public static final String contentTypeContext            = "contenttype";
    public static final String contentTypeContextUri         = rdfCacheNamespace + contentTypeContext;

    public static final String externalInferencingContext    = "externalInferencing";
    public static final String externalInferencingContextUri = rdfCacheNamespace + externalInferencingContext;

    public static final String startingPointsContext         = "startingPoints";
    public static final String startingPointsContextUri      = rdfCacheNamespace + startingPointsContext;

    public static final String startingPointContext          = "StartingPoint";
    public static final String startingPointContextUri       = rdfCacheNamespace + startingPointContext;

    public static final String functionsContext              = "myfn";
    public static final String functionsContextUri           = rdfCacheNamespace + functionsContext;

    public static final String listContext                   = "mylist";
    public static final String listContextUri                = rdfCacheNamespace + listContext;

    public static final String isContainedByContext          = "isContainedBy";
    public static final String isContainedByContextUri       = rdfCacheNamespace + isContainedByContext;

    public static final String reTypeToContext               = "reTypeTo";
    public static final String reTypeToContextUri            = rdfCacheNamespace + reTypeToContext;


    public static final String rdfType                       = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";




    public static void dropStartingPoints(SailRepository repo, Vector<String> startingPointUrls) {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            RepositoryUtility.dropStartingPoints(con, valueFactory, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }


    }

    /**
     * Set addStartingPoints statement for the importURI in the repository.
     *
     */
    public static void dropStartingPoints(RepositoryConnection con, ValueFactory valueFactory, Vector<String> startingPointUrls) {



        URI startingPointValue;
        URI isa = valueFactory.createURI(rdfType);
        URI startingPointsContext = valueFactory.createURI(startingPointsContextUri);
        URI startingPointType = valueFactory.createURI(startingPointContextUri);

        

        try {
            for (String startingPoint : startingPointUrls) {

                startingPointValue = valueFactory.createURI(startingPoint);
                con.remove(startingPointValue, isa, startingPointType, startingPointsContext);

                log.info("Removed starting point " + startingPoint + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                        + "> " + "<" + startingPointType + "> " + "<" + startingPointsContext + "> )");
            }
            con.commit();


        } catch (RepositoryException e) {
            log.error("In addStartingPoints, caught an RepositoryException! Msg: "
                    + e.getMessage());

        }

    }

    public static void addStartingPoints(SailRepository repo, Vector<String> startingPointUrls) {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            addStartingPoints(con, valueFactory, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }


    }

    /**
     * Adds the passed list of starting points to the repository.
     *
     *
     * @param con
     * @param startingPointUrls
     */
    public static void addStartingPoints(RepositoryConnection con, ValueFactory valueFactory, Vector<String> startingPointUrls) {

        log.debug("Adding StartingPoints...");

        for (String importURL : startingPointUrls) {
            addStartingPoint(con, valueFactory, importURL);
        }
    }

    public static boolean startingPointExists( RepositoryConnection con, String startingPointUrl) throws RepositoryException, MalformedQueryException, QueryEvaluationException{
        TupleQueryResult result = null;
        boolean hasInternalStaringPoint = false;

        String queryString = "SELECT doc "
            + "FROM {doc} rdf:type {rdfcache:StartingPoint} "
            + "WHERE doc = <" + startingPointUrl + "> "
            + "USING NAMESPACE "
            + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

        log.debug("queryStartingPoints: " + queryString);

        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,queryString);

        result = tupleQuery.evaluate();
        if (result.hasNext()){  //has internal starting point
        hasInternalStaringPoint = true;
        }
        return hasInternalStaringPoint;
    }



    public static void addStartingPoint(SailRepository repo, String startingPointUrl) {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            addStartingPoint(con, valueFactory, startingPointUrl);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }


    }

    /**
     * Adds the passed  starting point to the repository.
     *
     * @param con
     * @param startingPoint
     */
    public static void addStartingPoint(RepositoryConnection con, ValueFactory valueFactory, String startingPoint) {

        URI startingPointUri;
        URI isa = valueFactory.createURI(rdfType);
        URI startingPointsContext = valueFactory.createURI(RepositoryUtility.startingPointsContextUri);
        URI startingPointContext = valueFactory.createURI(RepositoryUtility.startingPointContextUri);


        try {

            if (startingPoint.startsWith("http://")) { //make sure it's a url and read it in
                startingPointUri = valueFactory.createURI(startingPoint);
                con.add(startingPointUri, isa, startingPointContext, startingPointsContext);


                log.info("addStartingPoint(): Added StartingPoint to the repository <" + startingPointUri + "> <" + isa
                        + "> " + "<" + startingPointContext + "> " + "<" + startingPointsContext + "> ");
            }
            else {
                log.error("addStartingPoint() - The startingPoint '"+startingPoint+"' does not appear to by a URL, skipping.");
            }


        } catch (RepositoryException e) {
            log.error("addStartingPoint(): Caught an RepositoryException! Msg: "
                    + e.getMessage());

        }


    }

    public static Vector<String> findChangedStartingPoints(SailRepository repo, Vector<String> startingPointUrls) {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findChangedStartingPoints(con, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();

    }


    /*
     * Add the old StartingPoint that is no longer a StartingPoint in this
     * update to the drop-list
     */
    public static   Vector<String> findChangedStartingPoints(RepositoryConnection con, Vector<String> startingPointsUrls) {
        Vector<String> result = null;
        Vector<String> changedStartingPoints = new Vector<String> ();
        log.debug("Checking if the old StartingPoint is still a StartingPoint ...");

        try {

            result = findAllStartingPoints(con);

                for (String startpoint : result) {

                    //log.debug("StartingPoints: " + startpoint);
                    if (!startingPointsUrls.contains(startpoint)
                            && !startpoint.equals(RepositoryUtility.internalStartingPoint)) {
                        changedStartingPoints.add(startpoint);
                        log.debug("Adding to droplist: " + startpoint);
                    }
                }

        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        log.info("Located " + changedStartingPoints.size()+" starting points that have been changed.");
        return changedStartingPoints;
    }


    public static Vector<String> findNewStartingPoints(SailRepository repo, Vector<String> startingPointUrls) {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findNewStartingPoints(con, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();


    }




    /**
     * Find new StartingPoints in the input file but not in the repository yet. If the internal starting point is not
     * present in the repository, then it will be added to the returned list of new starting points.
     *
     * @param con  A connection to the repository to search.
     * @param startingPointUrls A list of candidate starting points
     * @return All of the starting points in the passed startingPointUrls that are not already present in the repository.
     * If the internalStartingPoint is not present in the repository it will be returned too.
     */
    public static  Vector<String> findNewStartingPoints(RepositoryConnection con, Vector<String> startingPointUrls) {
        Vector<String> result = null;
        Vector<String> newStartingPoints = new Vector<String> ();
        log.debug("Checking for new starting points...");

        try {

            result = findAllStartingPoints(con);

            if(!result.contains(internalStartingPoint)){
                log.debug("Internal StartingPoint not present in repository, adding to list.");
                newStartingPoints.add(internalStartingPoint);
            }

            for (String startingPoint : startingPointUrls) {
                
                if (!result.contains(startingPoint) && !startingPoint.equals(internalStartingPoint)) {

                    newStartingPoints.add(startingPoint);

                    log.debug("Found New StartingPoint: " + startingPoint);
                }
            }

        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        log.info("Number of new StartingPoints: " + newStartingPoints.size());
        return newStartingPoints;
    }






    public static Vector<String> findAllStartingPoints(SailRepository repo) throws MalformedQueryException, QueryEvaluationException {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findAllStartingPoints(con);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();


    }



    /*
     * Find all starting points in the repository
     *
     */
    public static Vector<String> findAllStartingPoints(RepositoryConnection con) throws RepositoryException, MalformedQueryException, QueryEvaluationException {

        Vector<String> startingPoints = new Vector <String> ();


        TupleQueryResult result = queryForStartingPoints(con);
        if (result != null) {

            if (!result.hasNext()) {
                log.debug("NEW repository!");
            }

            while (result.hasNext()) {
                BindingSet bindingSet = (BindingSet) result.next();

                Value firstValue = bindingSet.getValue("doc");
                String startpoint = firstValue.stringValue();
                //log.debug("StartingPoints: " + startpoint);
                startingPoints.add(startpoint);

            }
        } else {
            log.debug("No query result!");

        }
        return startingPoints;

    }




    private static TupleQueryResult queryForStartingPoints(RepositoryConnection con) throws QueryEvaluationException, MalformedQueryException, RepositoryException {
        TupleQueryResult result = null;
        List<String> bindingNames;
        Vector<String> startingPoints = new Vector <String> ();

        log.debug("Finding StartingPoints in the repository ...");

        String queryString = "SELECT doc "
            + "FROM {doc} rdf:type {rdfcache:StartingPoint} "
            + "USING NAMESPACE "
            + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

        log.debug("queryStartingPoints: " + queryString);

        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,queryString);

        result = tupleQuery.evaluate();
        return result;

    }


    public static void dumpRepository(RepositoryConnection con, String filename) {

        // export repository to an n-triple file
        File outrps = new File(filename); // hard copy of repository
        try {
            log.info("Dumping repository to: '"+filename+"'");
            FileOutputStream myFileOutputStream = new FileOutputStream(outrps);
            if (filename.endsWith("nt")) {

                NTriplesWriter myNTRiplesWriter = new NTriplesWriter(
                        myFileOutputStream);
                
                con.export(myNTRiplesWriter);
                myNTRiplesWriter.startRDF();
                myNTRiplesWriter.endRDF();

            }
            if (filename.endsWith("trix")) {

                TriXWriter myTriXWriter = new TriXWriter(myFileOutputStream);
                
                con.export(myTriXWriter);
                myTriXWriter.startRDF();
                myTriXWriter.endRDF();

            }
            if (filename.endsWith("trig")) {

                TriGWriter myTriGWriter = new TriGWriter(myFileOutputStream);
                
                con.export(myTriGWriter);
                myTriGWriter.startRDF();
                myTriGWriter.endRDF();

            }
            log.info("Completed dumping explicit statements");

        } catch (Exception e) {
            log.error("Failed to dump repository! msg: "+e.getMessage());
        }

    }

    public static void dumpRepository(SailRepository owlse2, String filename) {

        RepositoryConnection con = null;

        try {
            con = owlse2.getConnection();
            dumpRepository(con, filename);
        }
        catch (RepositoryException e) {
            log.error("Failed to open repository connection. Msg: "+e.getMessage());
        } finally {
            log.debug("Closing repository connection.");

            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    log.error("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }


    }


    public static String showContexts(SailRepository repository){
        RepositoryConnection con = null;
        String msg;

        try {
            con = repository.getConnection();
            msg =  showContexts(con);
        }
        catch (RepositoryException e) {
            msg = "Failed to open repository connection. Msg: "+e.getMessage();
            log.error(msg);
        } finally {
            log.debug("Closing repository connection.");

            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    log.error("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }
        return msg;

    }
    public static String showContexts(RepositoryConnection con){

        String msg = "\nRepository ContextIDs:\n";
        try {
            RepositoryResult<Resource> contextIds = con.getContextIDs();

            for(Resource contextId : contextIds.asList()){
                msg += "    "+contextId+"\n";
            }

        } catch (RepositoryException e) {
            msg = "Failed to open repository connection. Msg: "+e.getMessage();
            log.error(msg);
        }

        return msg;
    }

    /**
     * Return true if import context is newer.
     *
     * @param con
     * @param importURL
     * @return Boolean
     */
    public static Boolean olderContext(RepositoryConnection con, String importURL) {
        Boolean oldLMT = false;

        String oldltmod = getLastModifiedTime(con, importURL); // LMT from repository

        if (oldltmod.isEmpty()) {
            oldLMT = true;
            return oldLMT;
        }
        String oltd = oldltmod.substring(0, 10) + " "
                + oldltmod.substring(11, 19);
        String ltmod = getLTMODContext(importURL);

        String ltd = ltmod.substring(0, 10) + " " + ltmod.substring(11, 19);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        Date ltdparseDate;
        try {
            ltdparseDate = dateFormat.parse(ltd);
            log.debug("In  olderContext ...");
            log.debug("URI " + importURL);
            log.debug("lastmodified    " + ltdparseDate.toString());
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
     * Checks and returns last modified time of a context (URI) via querying
     * against the repository on contexts.
     *
     * @param urlstring
     */
    public static String getLastModifiedTime(RepositoryConnection con, String urlstring) {
        TupleQueryResult result = null;
        String ltmodstr = "";
        URI uriaddress = new URIImpl(urlstring);
        Resource[] context = new Resource[1];
        context[0] = (Resource) uriaddress;


        String queryString = "SELECT doc,lastmod FROM CONTEXT "
                  + "rdfcache:cachecontext {doc} rdfcache:last_modified {lastmod} "
                  + "where doc=<" + uriaddress + ">"
                  + "USING NAMESPACE "
                  + "rdfcache = <"+RepositoryUtility.rdfCacheNamespace+">";
        try {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfY;

            while (result.hasNext()) { // should have only one value
                bindingSet =  result.next();
                //Set<String> names = bindingSet.getBindingNames();
                // for (String name : names) {
                // log.debug("BindingNames: " + name);
                // }
                valueOfY =  bindingSet.getValue("lastmod");
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
        }

        return ltmodstr;
    }
    public static String getLTMODContext(String urlstring) {
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

    public static String getLastModifiedTimeString(Date date) {
        return getLastModifiedTimeString(date.getTime());
    }


    public static String getLastModifiedTimeString(long epochTime) {
        String ltmodstr = "";
        Timestamp ltmodsql = new Timestamp(epochTime);
        String ltmodstrraw = ltmodsql.toString();
        ltmodstr = ltmodstrraw.substring(0, 10) + "T"
                + ltmodstrraw.substring(11, 19) + "Z";
        return ltmodstr;
    }

    /**
     * Returns a Hash containing last modified times of each context (URI) in the
     * repository, keyed by the context name.
     * @param repository The repository from which to harvest the contexts and their associated last
     * modified times.
     */
    public static HashMap<String, String> getLastModifiedTimesForContexts(IRISailRepository repository) {
        RepositoryConnection con = null;

        try{
            con = repository.getConnection();
            return getLastModifiedTimesForContexts(con);
        }
        catch (RepositoryException e) {
            log.error("Caught a RepositoryException! Msg: " + e.getMessage());
        }
        finally {
            if(con!=null){
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("Caught a RepositoryException! Msg: " + e.getMessage());
                }
            }
        }
        return new HashMap<String, String>();
    }



    /**
     * Returns a Hash containing last modified times of each context (URI) in the
     * repository, keyed by the context name.
     * @param con A connection to the repoistory from which to harvest the contexts and their associated last
     * modified times.
     */
    public static HashMap<String, String> getLastModifiedTimesForContexts(RepositoryConnection con) {
        TupleQueryResult result = null;
        String ltmodstr = "";
        String idstr = "";
        HashMap<String, String> idltm = new HashMap<String, String>();
        String queryString = "SELECT DISTINCT id, lmt "
                + "FROM "
                + "{cd} wcs:Identifier {id}; "
                + "rdfs:isDefinedBy {doc} rdfcache:last_modified {lmt} "
                + "using namespace "
                + "rdfcache = <"+RepositoryUtility.rdfCacheNamespace+">, "
                + "wcs= <http://www.opengis.net/wcs/1.1#>";

        try {
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
        } finally {
            try {
                if(result!=null)
                    result.close();
            } catch (QueryEvaluationException e) {
                log.error("Caught a QueryEvaluationException! Msg: "
                        + e.getMessage());
            }
        }

        return idltm;
    }



}
