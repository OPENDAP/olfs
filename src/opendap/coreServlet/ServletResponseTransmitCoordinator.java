package opendap.coreServlet;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServletResponseTransmitCoordinator implements TransmitCoordinator {

    private ServletResponse d_hsr;

    public ServletResponseTransmitCoordinator(ServletResponse hsr) throws IOException {
        d_hsr = hsr;
    }

    @Override
    public boolean isCommitted() {
        return d_hsr.isCommitted();
    }

    @Override
    public void reset() {
        d_hsr.reset();
    }
}
