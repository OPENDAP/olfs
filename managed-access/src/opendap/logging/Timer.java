/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.logging;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * This Timer is NOT thread safe.
 */
public class Timer {

    private static HashMap<String, Long> startTimes = new HashMap<String, Long>();
    private static HashMap<String, Long> endTimes = new HashMap<String, Long>();

    private static boolean enabled = false;

    public static void enable(){
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static String start(){
        if(!enabled)
            return null;

        StackTraceElement st[] = Thread.currentThread().getStackTrace();
        boolean done = false;

        int callingMethodStackIndex = 2;
        int timesCalled = 0;

        while(!done){
            StringBuilder key = new StringBuilder();
            key.append(st[callingMethodStackIndex].getClassName()).append(".")
                    .append(st[callingMethodStackIndex].getMethodName()).append("_").append(timesCalled++);

            if(!startTimes.containsKey(key.toString())){
                startTimes.put(key.toString(), System.nanoTime());
                return key.toString();
            }

        }

        return null;

    }

    public static void stop(String key){
        if(!enabled)
            return;

        endTimes.put(key, System.nanoTime());


    }

    public static void report(PrintStream pw){
        if(!enabled)
            return;
        pw.print(report());
    }

    public static String report(){
        if(!enabled)
            return null;

        StringBuilder rprt = new StringBuilder();

        for(String key: startTimes.keySet()){

            rprt.append(key);

            long startTime = startTimes.get(key);
            rprt.append(" START: ").append(startTime);

            if(endTimes.containsKey(key)){
                long endTime = endTimes.get(key);

                rprt.append(" END: ").append(endTime);
                rprt.append(" elapsed: ").append((endTime - startTime)/1000000.00).append(" ms");


            }
            else {
                rprt.append(" END: ERROR_-_Timer.stop()_was_never_called.");

            }
            rprt.append("\n");


        }


        return rprt.toString();
    }

    public static void reset() {
        if(!enabled)
            return;
        startTimes.clear();
        endTimes.clear();
    }

}
