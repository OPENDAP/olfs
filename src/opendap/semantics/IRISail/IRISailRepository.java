package opendap.semantics.IRISail;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;

import org.openrdf.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A local SailRepository . 
 * 
 * @author Haibo liu, iri.columbia.edu
 * 
 */
public class IRISailRepository extends SailRepository {

    private static Logger log ;


    private AtomicBoolean isRepositoryDown;


    public IRISailRepository(Sail sail) {
        super(sail);
        log = LoggerFactory.getLogger(getClass());
        isRepositoryDown = new AtomicBoolean();
    }


    public void initialize() throws org.openrdf.repository.RepositoryException {
        super.initialize();
        isRepositoryDown.set(false);
    }

    public Boolean isRepositoryDown() {
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
        if (!isRepositoryDown.get()) {
            super.shutDown();
            isRepositoryDown.set(false);
            log.info("shutDown(): Semantic Repository Has Been Shutdown.");
        } else {
            log.info("shutDown(): Semantic Repository was already down.");
        }
        log.debug("shutDown(): Repository shutdown complete.");
    }



}