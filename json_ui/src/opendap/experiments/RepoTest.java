package opendap.experiments;


import com.ontotext.trree.owlim_ext.SailImpl;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
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
import java.io.PrintStream;
import java.net.URL;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 18, 2010
 * Time: 1:39:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepoTest {


    static String repositoryStorage = "owlim-storage";
    static String startingPointsContext = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#startingPoints";

    
    //static String rdfInputFile = "http://scm.opendap.org/svn/trunk/olfs/src/opendap/experiments/test.trig";
    static String rdfInputFile = "/data/haibo/workspace/olfstrunk/repositorycache-2f-horst/owlimHorstRepository.trig";  
    static DualPrinter dp;



    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;

        String fileName;


        String workingDir = System.getProperty("user.dir");
        System.out.println("Current directory: " + workingDir);

        startTime = new Date().getTime();

        try {

            PrintStream logFile = new PrintStream(new FileOutputStream("RepoTest.log"));

            dp = new DualPrinter(System.out,logFile);


            dp.println("\n#######################################");
            purgeRepositoryCache(workingDir);

            dp.println("\n\n#######################################");
            Repository repo = setupRepository(workingDir);
            loadStatements(repo, rdfInputFile);
            dp.println("Loaded RDF statements from " + rdfInputFile);
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            repo.shutDown();

            dp.println("\n\n#######################################");
            repo = setupRepository(workingDir);
            dp.println("Loaded statements loaded from repository persistence...");
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            fileName = "UnchangedTestStatementsFromOwlim.trig";
            dumpRepository(repo, fileName);
            repo.shutDown();

            dp.println("\n\n#######################################");
            repo = setupRepository(workingDir);
            dp.println("Loaded statements loaded from repository persistence...");
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            dp.println("Dropping Statement.");
            dropStatement(repo);
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            fileName = "AfterDropFromMemory.trig";
            dumpRepository(repo, fileName);
            repo.shutDown();

            dp.println("\n\n#######################################");
            repo = setupRepository(workingDir);
            dp.println("Loaded statements loaded from repository persistence...");
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            fileName = "AfterDropFromOwlimPersistence.trig";
            dumpRepository(repo, fileName);
            repo.shutDown();

            dp.println("\n\n#######################################");
            dp.println("#######################################");
            purgeRepositoryCache(workingDir);

            dp.println("\n\n#######################################");
            repo = setupRepository(workingDir);
            loadStatements(repo, fileName);
            dp.println("Loaded RDF statements from " + fileName);
            dp.println(showContexts(repo));
            dp.println(showStatements(repo,startingPointsContext));
            repo.shutDown();


        } catch (Exception e) {
            dp.println("Caught " + e.getClass().getName() + " in main(): "
                    + e.getMessage());
            e.printStackTrace(System.err);
        }
        finally {
            endTime = new Date().getTime();
            elapsedTime = (endTime - startTime) / 1000.0;
            dp.println("Elapsed Time: " + elapsedTime + "s");
        }

    }

    private static Repository setupRepository(String cacheDir) throws RepositoryException {


        dp.println("Setting up Semantic Repository.");

        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        Repository repo = new SailRepository(owlimSail);

        dp.println("Configuring Semantic Repository.");
        File storageDir = new File(cacheDir);
        owlimSail.setDataDir(storageDir);
        dp.println("Semantic Repository Data directory set to: " + cacheDir);

        owlimSail.setParameter("storage-folder", repositoryStorage);
        dp.println("Semantic Repository 'storage-folder' set to: " + repositoryStorage);

        String ruleSet;
        ruleSet = "owl-horst";
        owlimSail.setParameter("ruleset", ruleSet);
        dp.println("Semantic Repository 'ruleset' set to: " + ruleSet);


        dp.println("Initializing Semantic Repository.");

        repo.initialize();

        dp.println("Semantic Repository Ready.");


        return repo;

    }

    public static void purgeRepositoryCache(String cacheDir) {

        if (!cacheDir.endsWith("/"))
            cacheDir += "/";

        dp.println("Purging repository cache...");
        File repoCache = new File(cacheDir + repositoryStorage);

        deleteDir(repoCache);


    }

    private static void loadStatements(Repository repo, String rdfFileName) throws RepositoryException, IOException, RDFParseException {


        RepositoryConnection con = null;

        try {
            con = repo.getConnection();

            if(rdfFileName.startsWith("http://")){
                URL rdfUrl = new URL(rdfFileName);
                con.add(rdfUrl, "http://someBaseURI#", RDFFormat.TRIG);
            }
            else {
                File rdfFile = new File(rdfFileName);
                con.add(rdfFile, "http://someBaseURI#", RDFFormat.TRIG);
            }
            con.commit();
        }
        finally {
            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    System.err.println("Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }


    }

    private static void dropStatement(Repository repo) throws RepositoryException {
        ValueFactory valueFactory = repo.getValueFactory();

        String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

        RepositoryConnection con = null;

        String startingPoint = "http://test.opendap.org:8090/opendap/ioos/ECMWF_ERA-40_subset.ncml.rdf";
        URI startingPointValue = valueFactory.createURI(startingPoint);
        URI isa = valueFactory.createURI(rdfType);
        URI startingPointsContext = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#startingPoints");
        URI startingPointType = valueFactory.createURI("http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#StartingPoint");


        try {
            con = repo.getConnection();
            con.remove(startingPointValue, isa, startingPointType, startingPointsContext);
            con.commit();

            dp.println("Removed starting point " + startingPoint + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                    + "> " + "<" + startingPointType + "> " + "<" + startingPointsContext + "> )");

        }
        finally {
            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    System.err.println("Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }

    }

    public static String showContexts(Repository repository) throws RepositoryException {
        RepositoryConnection con = null;
        String msg;

        try {
            con = repository.getConnection();
            msg = showContexts(con);
        } finally {

            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    System.err.println("Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }
        return msg;

    }

    public static String showContexts(RepositoryConnection con) throws RepositoryException {

        String msg;
        RepositoryResult<Resource> contextIds = con.getContextIDs();

        if(contextIds.hasNext()){
            msg = "\nRepository ContextIDs:\n";
            for (Resource contextId : contextIds.asList()) {
                msg += "    " + contextId + "\n";
            }
        }
        else {
            msg = "\nERROR - No context IDs. RepositoryConnection.getContextIDs() returned an empty RepositoryResult object.\n";

        }


        return msg;
    }

    public static String showStatements(Repository repository, String context) throws RepositoryException {
        RepositoryConnection con = null;
        String msg;
        URI contextUri = null;
        try {

            if(context!=null)
                contextUri = repository.getValueFactory().createURI(context);
            con = repository.getConnection();
            msg = showStatements(con, contextUri);
        } finally {

            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    System.err.println("Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }
        return msg;

    }

    public static String showStatements(RepositoryConnection con, Resource context) throws RepositoryException {


        String msg;

        if(context!=null)
            msg = "\nRepository Statements in context '"+context.stringValue()+"'\n";
        else
            msg = "\nALL Repository Statements:\n";


        RepositoryResult<Statement> statements = con.getStatements(null,null,null,true,context);

        for (Statement statement : statements.asList()) {
            msg += "    <" + statement.getSubject()+">";
            msg += "    <" + statement.getPredicate()+">";
            msg += "    <" + statement.getObject()+">";
            msg += "    <" + statement.getContext()+">";
            msg += "\n";
        }


        return msg;
    }


    public static void dumpRepository(RepositoryConnection con, String filename) {

        // export repository to an n-triple file
        File outrps = new File(filename); // hard copy of repository
        try {
            dp.println("\nDumping repository to: '" + filename + "' ");
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
            System.err.println("Failed to dump repository! msg: " + e.getMessage());
        }

    }

    public static void dumpRepository(Repository owlse2, String filename) throws RepositoryException {

        RepositoryConnection con = null;

        try {
            con = owlse2.getConnection();
            dumpRepository(con, filename);
        } finally {

            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    System.err.println("Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }


    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }


    static class DualPrinter  {

        PrintStream _ps1, _ps2;
        DualPrinter(PrintStream ps1, PrintStream ps2){
            _ps1 = ps1;
            _ps2 = ps2;

        }

        public void print(String msg){
            _ps1.print(msg);
            _ps2.print(msg);
        }

        public void println(String msg){
            _ps1.println(msg);
            _ps2.println(msg);
        }

        public void close(){
            _ps1.close();
            _ps2.close();
        }


    }


}
