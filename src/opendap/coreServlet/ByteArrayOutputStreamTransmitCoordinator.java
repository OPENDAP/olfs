package opendap.coreServlet;

import java.io.ByteArrayOutputStream;

public class ByteArrayOutputStreamTransmitCoordinator implements TransmitCoordinator {
    private ByteArrayOutputStream d_baos;

    public ByteArrayOutputStreamTransmitCoordinator(ByteArrayOutputStream baos) {
        d_baos = baos;
    }

    /**
     * Is a ByteArrayOutputStream ever committed? I don't see how...
     *
     * @return False, always
     */
    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() throws IllegalStateException {
        if(isCommitted()){
            throw new IllegalStateException("Ouch! Attempted reset of" +
                    " ByteArrayOutputStream failed because bytes have already " +
                    "been written to the stream.");
        }

        d_baos.reset();
    }

}
