package opendap.coreServlet;

import java.io.FileOutputStream;

public class FileOutputStreamTransmitCoordinator implements TransmitCoordinator {

    FileOutputStream d_fos;
    public FileOutputStreamTransmitCoordinator(FileOutputStream fos){
        d_fos = fos;
    }
    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() throws IllegalStateException {
        if(isCommitted()){
            throw new IllegalStateException("Ouch! Attempted reset of" +
                    " FileOutputStream failed because a FileOutputStream cannot " +
                    "be reset.");
        }
    }
}
