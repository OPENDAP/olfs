package opendap.coreServlet;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface TransmitCoordinator {
    boolean isCommitted();
    void reset();
}

