package opendap.coreServlet;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * This class wraps the ServletResponse and exposes just its isCommited()
 * and rest() methods.
 */
public class ServletResponseTransmitCoordinator implements TransmitCoordinator {

    /**
     * The underlying ServletResponse which will be utilized
     */
    private ServletResponse d_sr;

    /**
     *  Create a new TransmitCoordinator instance using the passed
     *  ServletResponse instance.
     * @param sr
     * @throws IOException
     */
    public ServletResponseTransmitCoordinator(ServletResponse sr) throws IOException {
        d_sr = sr;
    }

    /**
     *
     * @return Returns true if unrecoverable bytes have already been written
     * to the underlying ServletResponse output stream.
     */
    @Override
    public boolean isCommitted() {
        return d_sr.isCommitted();
    }

    /**
     * Resets the underlying response output stream. If a reset is not possible
     * then an IllegalStateException state exception will be thrown.
     * @throws IllegalStateException If the response cannot be reset(), typically
     * because the response has already been committed to the client.
     */
    @Override
    public void reset() throws IllegalStateException{
        d_sr.reset();
    }
}
