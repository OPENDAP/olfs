package opendap.logging;

/**
 * A Simple helper class for the Timer. Because Timer has static methods Procedure
 * could not be an inner class without becoming a singlton.
 *
 * Created by ndp on 5/18/15.
 *
 *
 */
public class Procedure {
    String name;
    long start;
    long end;

    /**
     * Records System.nanoTime() as the start value and sets end to same value as start;
     */
    public void start(){
        start = System.nanoTime();
        end = start;
    }

    /**
     *  Records System.nanoTime() as the end value
     */
    public void end(){
        end = System.nanoTime();
    }

    /**
     *
     * @return   Elapsed time for this procedure in milliseconds.
     * If the returned time is zero chances are extremely good that the end() method was not called.
     */
    public double elapsedTime(){
        return (end - start)/1000000.00;
    }


}
