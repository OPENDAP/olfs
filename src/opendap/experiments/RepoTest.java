package opendap.experiments;

import com.ontotext.trree.owlim_ext.SailImpl;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 18, 2010
 * Time: 1:39:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepoTest {



    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;

        String workingDir =  System.getProperty("user.dir");
        System.out.println("Current directory: "+workingDir);

        startTime = new Date().getTime();

        try {

            SailRepository repo = setupRepository(workingDir);

            loadStatements(repo,"test.trig");

            System.out.println(showContexts(repo));
                          
            dropStatement(repo);

            dumpRepository(repo,"test_drop.trig");

            System.out.println(showContexts(repo));




        } catch (Exception e) {
            System.err.println("Caught " + e.getClass().getName() + " in main(): "
                    + e.getMessage());
            e.printStackTrace(System.err);
        }
        finally {
            endTime = new Date().getTime();
            elapsedTime = (endTime - startTime) / 1000.0;
            System.out.println("Elapsed Time: "+elapsedTime+"s");
        }

    }
    private static SailRepository setupRepository(String cacheDir) throws RepositoryException {


        String repositoryStorage = "owlim-storage";

        System.out.println("Setting up Semantic Repository.");

        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        SailRepository repo = new SailRepository(owlimSail);

        System.out.println("Configuring Semantic Repository.");
        File storageDir = new File(cacheDir);
        owlimSail.setDataDir(storageDir);
        System.out.println("Semantic Repository Data directory set to: " + cacheDir);

        owlimSail.setParameter("storage-folder", repositoryStorage);
        System.out.println("Semantic Repository 'storage-folder' set to: " + repositoryStorage);

        String ruleSet;
        ruleSet = "owl-horst";
        owlimSail.setParameter("ruleset", ruleSet);
        System.out.println("Semantic Repository 'ruleset' set to: " + ruleSet);


        System.out.println("Initializing Semantic Repository.");

        repo.initialize();

        System.out.println("Semantic Repository Ready.");


        return repo;

    }


    private static void loadStatements(SailRepository repo, String rdfFileName) throws RepositoryException, IOException, RDFParseException {


        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            File rdfFile = new File(rdfFileName);
            con.add(rdfFile,"http://someURL#",RDFFormat.TRIG);
            con.commit();
        }
        finally {
            if(con!=null) {
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    System.err.println("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }


    }

    private static void dropStatement(SailRepository repo) throws RepositoryException {
        ValueFactory valueFactory = repo.getValueFactory();

        String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

        RepositoryConnection con = null;

        String startingPoint = "http://test.opendap.org:8090/opendap/ioos/ECMWF_ERA-40_subset.ncml.rdf" ;
        URI startingPointValue = valueFactory.createURI(startingPoint);
        URI isa = valueFactory.createURI(rdfType);
        URI startingPointsContext = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#startingPoints");
        URI startingPointType = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#StartingPoint");


        try {
            con = repo.getConnection();
            con.remove(startingPointValue, isa, startingPointType, startingPointsContext);
            con.commit();
            
            System.out.println("Removed starting point " + startingPoint + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                    + "> " + "<" + startingPointType + "> " + "<" + startingPointsContext + "> )");

        }
        finally {
            if(con!=null) {
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    System.err.println("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }

    }
    public static String showContexts(SailRepository repository) throws RepositoryException {
        RepositoryConnection con = null;
        String msg;

        try {
            con = repository.getConnection();
            msg =  showContexts(con);
        } finally {

            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    System.err.println("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }
        return msg;

    }
    public static String showContexts(RepositoryConnection con) throws RepositoryException {

        String msg = "\nRepository ContextIDs:\n";
            RepositoryResult<Resource> contextIds = con.getContextIDs();

            for(Resource contextId : contextIds.asList()){
                msg += "    "+contextId+"\n";
            }


        return msg;
    }


    public static void dumpRepository(RepositoryConnection con, String filename) {

        // export repository to an n-triple file
        File outrps = new File(filename); // hard copy of repository
        try {
            System.out.println("\nDumping repository to: '"+filename+"' ");
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

        } catch (Exception e) {
            System.err.println("Failed to dump repository! msg: "+e.getMessage());
        }

    }

    public static void dumpRepository(SailRepository owlse2, String filename) throws RepositoryException {

        RepositoryConnection con = null;

        try {
            con = owlse2.getConnection();
            dumpRepository(con, filename);
        } finally {

            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    System.err.println("Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }


    }


}
