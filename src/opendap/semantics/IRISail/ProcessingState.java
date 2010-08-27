package opendap.semantics.IRISail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 26, 2010
 * Time: 5:44:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessingState {

    static Logger log = LoggerFactory.getLogger(ProcessingState.class);

    static AtomicBoolean stopWorking;
    static{
        stopWorking = new AtomicBoolean();
        stopWorking.set(false);
    }

    public static boolean continueProcessing() {
        Thread thread = Thread.currentThread();
        if(thread.isInterrupted()){
            stopWorking.set(true);
            log.warn("check(): WARNING! Thread "+thread.getName()+" was interrupted!");
        }
        return !stopWorking.get();

    }

    public static void enableProcessing(){
        stopWorking.set(false);
    }


    public static void stopProcessing() {
        log.warn("stopProcessing(): WARNING! Stopping background processing!");
        stopWorking.set(true);
    }

    public static void interruptProcessing() throws InterruptedException{
        stopProcessing();
        log.warn("interruptProcessing(): Interrupting current Thread.");
        throw new InterruptedException("Stopping Processing!");

    }

    public static void checkState() throws InterruptedException{
        Thread thread = Thread.currentThread();
        if(thread.isInterrupted()){
            interruptProcessing();
        }
    }


}
