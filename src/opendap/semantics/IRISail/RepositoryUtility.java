package opendap.semantics.IRISail;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 21, 2010
 * Time: 4:57:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryUtility {

    private static Logger log = LoggerFactory.getLogger(RepositoryUtility.class);
    public static final String internalStartingPoint = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl";
    public static final String rdfCacheNamespace = internalStartingPoint+"#";







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

        String pred = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";


        URI startingPointValue = null;
        URI isa = valueFactory.createURI(pred);
        URI context = valueFactory.createURI(rdfCacheNamespace+"startingPoints");
        URI startingPointType = valueFactory.createURI(rdfCacheNamespace+"StartingPoint");
        URL url;

        try {


            for (String importURL : startingPointUrls) {

                url = new URL(importURL);
                startingPointValue = valueFactory.createURI(importURL);
                con.remove((Resource) startingPointValue, isa, (Value) startingPointType, (Resource) context);

                log.info("Removed starting point " + importURL + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                        + "> " + "<" + startingPointType + "> " + "<" + context + "> )");
            }


        } catch (RepositoryException e) {
            log.error("In addStartingPoints, caught an RepositoryException! Msg: "
                    + e.getMessage());

        } catch (MalformedURLException e) {

            log.error("In addStartingPoints, caught an MalformedURLException! Msg: "
                    + e.getMessage());
            //} catch (RDFParseException e) {
            //    log.error("In addStartingPoints, caught an RDFParseException! Msg: "
            //            + e.getMessage());
        } catch (IOException e) {
            log.error("In addStartingPoints, caught an IOException! Msg: "
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

            for (String importURL : startingPointUrls) {
                addStartingPoint(con, valueFactory, importURL);
            }
    }

    private static boolean startingPointExists( RepositoryConnection con, String staringPointUrl) throws RepositoryException, MalformedQueryException, QueryEvaluationException{
        TupleQueryResult result = null;
        boolean hasInternalStaringPoint = false;

        String queryString = "SELECT doc "
            + "FROM {doc} rdf:type {rdfcache:StartingPoint} "
            + "WHERE doc = "+internalStartingPoint
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

    private static void addInternalStartingPoint(RepositoryConnection con, ValueFactory valueFactory) throws RepositoryException, MalformedQueryException, QueryEvaluationException{
       
        if(!startingPointExists(con,internalStartingPoint)){
            addStartingPoint(con, valueFactory, internalStartingPoint);
        }
    }
    public static void addInternalStartingPoint(SailRepository repo) {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            addInternalStartingPoint(con, valueFactory);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error(e.getClass().getName()+": Malformed query. Msg: "
                    + e.getMessage()); 
        } catch (QueryEvaluationException e) {
            log.error(e.getClass().getName()+": QueryEvaluationException. Msg: "
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
     * @param importURL
     */
    public static void addStartingPoint(RepositoryConnection con, ValueFactory valueFactory, String importURL) {
        String pred = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

            URI s = valueFactory.createURI(importURL);
            URI isa = valueFactory.createURI(pred);
            URI cont = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#startingPoints");
            URI o = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#StartingPoint");
            URL url;


            try {

                if (importURL.startsWith("http://")) { //make sure an url and read it in
                url = new URL(importURL);
                s = valueFactory.createURI(importURL);
                con.add((Resource) s, isa, (Value) o, (Resource) cont);


                log.info("Added to the repository <" + s + "> <" + isa
                        + "> " + "<" + o + "> " + "<" + cont + "> ");
                }


            } catch (RepositoryException e) {
                log.error("In addStartingPoints, caught an RepositoryException! Msg: "
                        + e.getMessage());

            } catch (MalformedURLException e) {

                log.error("In addStartingPoints, caught an MalformedURLException! Msg: "
                        + e.getMessage());
            //} catch (RDFParseException e) {
            //    log.error("In addStartingPoints, caught an RDFParseException! Msg: "
            //            + e.getMessage());
            } catch (IOException e) {
                log.error("In addStartingPoints, caught an IOException! Msg: "
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



    /*
     * Find new StartingPoints in the input file but not in the repository yet
     *
     */
    public static  Vector<String> findNewStartingPoints(RepositoryConnection con, Vector<String> startingPointUrls) {
        Vector<String> result = null;
        Vector<String> newStartingPoints = new Vector<String> ();
        log.debug("Checking for new starting points...");

        try {

            result = findAllStartingPoints(con);

            for (String startpoint : startingPointUrls) {

                //log.debug("StartingPoints: " + startpoint);
                
                if (!result.contains(startpoint) &&
                        !startpoint.equals(RepositoryUtility.internalStartingPoint)) {

                    newStartingPoints.add(startpoint);

                    log.debug("Adding to New StartingPints list: " + startpoint);
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
                if (!startpoint.equals(RepositoryUtility.internalStartingPoint)) {
                    startingPoints.add(startpoint);

                    //log.debug("Starting point in the repository: " + startpoint);
                }
            }
        } else {
            log.debug("No query result!");

        }
        return startingPoints;

    }


    public static boolean isNewRepository(SailRepository repo) {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return isNewRepository(con);
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
        return true;


    }

    public static boolean isNewRepository(RepositoryConnection con){
        try{
            TupleQueryResult result = queryForStartingPoints(con);
            if (result != null && !result.hasNext()) {
                return true;
            }

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("Caught QueryEvaluationException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }
        return false;

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



}
