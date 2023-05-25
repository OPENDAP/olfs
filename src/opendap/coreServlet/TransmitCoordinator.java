package opendap.coreServlet;

public interface TransmitCoordinator {
    boolean isCommitted();
    void reset() throws IllegalStateException;
}

