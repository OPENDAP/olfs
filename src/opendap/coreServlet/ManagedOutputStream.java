package opendap.coreServlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface ManagedOutputStream {
    boolean isCommitted();
    void reset();
}

class ManagedServletOutputStream  implements ManagedOutputStream {

    HttpServletResponse d_hsr;

    ManagedServletOutputStream(HttpServletResponse hsr) throws IOException {
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

class ManagedByteArrayOutputStream implements ManagedOutputStream {
    ByteArrayOutputStream d_baos;
    ManagedByteArrayOutputStream(ByteArrayOutputStream baos){
        d_baos = baos;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        d_baos.reset();
    }

}

class foo {
    void test(ManagedOutputStream mo){
        mo.isCommitted();
    }
}