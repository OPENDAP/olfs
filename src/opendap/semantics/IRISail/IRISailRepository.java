package opendap.semantics.IRISail;

import java.util.concurrent.atomic.AtomicBoolean;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;

import org.openrdf.sail.Sail;
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

    private static Logger log ;


    private AtomicBoolean isRepositoryDown;


    public IRISailRepository(Sail sail) {
        super(sail);
        log = LoggerFactory.getLogger(getClass());
        isRepositoryDown = new AtomicBoolean();
    }


    public void initialize() throws org.openrdf.repository.RepositoryException {
        super.initialize();
        setRepositoryDown(false);
    }

    private void setRepositoryDown(Boolean repositoryState) {
        isRepositoryDown.set(repositoryState);
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
        if (!isRepositoryDown()) {
            super.shutDown();
            setRepositoryDown(true);
            log.info("shutDown(): Semantic Repository Has Been Shutdown.");
        } else {
            log.info("shutDown(): Semantic Repository was already down.");
        }
        log.debug("shutDown(): Repository shutdown complete.");
    }



}