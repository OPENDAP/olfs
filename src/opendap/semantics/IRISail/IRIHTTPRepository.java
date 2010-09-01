/**
 * 
 */
package opendap.semantics.IRISail;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A repository that serves as a proxy for a remote repository on a Sesame server
 * @author haibo
 *
 */

    public class IRIHTTPRepository extends HTTPRepository {
        private static Logger log ;


        private AtomicBoolean isRepositoryDown;

        public IRIHTTPRepository(String serverURL, String repositoryID) {

            super(serverURL, repositoryID);

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
