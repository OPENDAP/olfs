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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This Timer is thread safe.
 */
public class Timer {

    private static Logger log = LoggerFactory.getLogger(Timer.class);
    private static ConcurrentHashMap<String, StringBuilder> threadLogs = new ConcurrentHashMap<>();
    private static AtomicBoolean enabled;
    static {
        enabled = new AtomicBoolean(false);
    }

    private static final String mrk = "][";


    /**
     * Private because this is a singleton
     */
    private Timer(){}

    /**
     * Turn on the Timer.
     */
    public static boolean enable(){
        return enabled.getAndSet(true);
    }

    /**
     * Turn off the Timer.
     */
    public static boolean disable() {
        return  enabled.getAndSet(false);
    }

    /**
     * Is it on?
     * @return True if Timer is enabled.
     */
    public static boolean isEnabled(){
        return enabled.get();
    }


    /**
     * @return A StringBuilder for logging the current thread's timers
     */
    private static StringBuilder getThreadLog(){
        String threadName = Thread.currentThread().getName();
        threadLogs.putIfAbsent(threadName, new StringBuilder());
        return threadLogs.get(threadName);
    }


    /**
     * Starts (and logs) a timer associated with the current thread. The time will back up the call stack and
     * name the Timer for the calling class and method.
     * @return A Procedure object to hand the Timer.stop() method.
     */
    public static Procedure start(){
        if(!enabled.get())
            return null;

        StringBuilder threadLog = getThreadLog();
        String threadName = Thread.currentThread().getName();
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int callingMethodStackIndex = 2;

        StringBuilder key = new StringBuilder();
        key.append(st[callingMethodStackIndex].getClassName()).append(".")
                .append(st[callingMethodStackIndex].getMethodName());

        Procedure p = new Procedure();
        p.name = key.toString();
        p.start();

        // BES
        //# 1601642669|&|2122|&|timing|&|TimingFields

        Date now = new Date();
        threadLog.append("[");
        threadLog.append(now.getTime()).append(mrk);
        threadLog.append(threadName).append(mrk);
        threadLog.append("timing").append(mrk);
        threadLog.append(key).append(mrk);
        threadLog.append("STARTED: ").append(p.start).append(" ns").append(mrk);
        log.info("start() - {} started:  {} ", key, p.start);
        return p;
    }


    /**
     * Stops and logs the timing of the associated procedure.
     *
     * @param procedure  The Procedure to stop timing.
     */
    public static void stop(Procedure procedure){
        if(!enabled.get())
            return;

        if(procedure != null) {
            procedure.end();

            StringBuilder threadLog = getThreadLog();
            threadLog.append("STOPPED: ").append(procedure.end);
            threadLog.append(" ns").append(mrk);
            threadLog.append("ELAPSED: ").append(procedure.elapsedTime());
            threadLog.append(" ms").append("]");
            threadLog.append("\n");
            log.info(threadLog.toString());
        }
        else {
            StringBuilder threadLog = getThreadLog();
            threadLog.append("ERROR: Attempted to stop a null valued procedure.\n");
            log.error(threadLog.toString());
        }
    }


    /**
     * Writes the report() to the pass PrintStream.
     * @param pw  A PrintStream to which to print the report String.
     */
    public static void report(PrintStream pw){
        if(!enabled.get()) {
            pw.print("Timer is NOT enabled");
            return;
        }
        pw.print(report());
    }


    /**
     * Resets the Timer for the current thread.
     */
    public static void reset() {
        if(!enabled.get())
            return;
        String threadName = Thread.currentThread().getName();
        threadLogs.remove(threadName);
    }


    /**
     *
     * @return A summary of the current timing activities since the last call to Timer.reset()
     */
    public static String report() {
        if (!enabled.get()) {
            Date now = new Date();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(now.getTime()).append(mrk);
            sb.append(Thread.currentThread().getName()).append(mrk);
            sb.append("Timer is NOT enabled for this Thread.");
            sb.append("]\n");
            return sb.toString();
        }
        return getThreadLog().toString();
    }
}
