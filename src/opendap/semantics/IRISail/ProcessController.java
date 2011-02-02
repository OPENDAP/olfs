/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2011 OPeNDAP, Inc.
//
// Authors:
//     Nathan David Potter  <ndp@opendap.org>
//     
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.semantics.IRISail;

import opendap.wcs.v1_1_2.CatalogWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is used to track the state of a thread updating the repository.
 * It makes sure that the clients get current information from the repository.
 */
public class ProcessController {

    static Logger log = LoggerFactory.getLogger(ProcessController.class);

    static AtomicBoolean currentlyProcessing;
    static AtomicLong lastProcessingStart;
    static AtomicLong lastProcessingEnd;

    static {
        currentlyProcessing = new AtomicBoolean();
        currentlyProcessing.set(false);

        lastProcessingStart = new AtomicLong();
        lastProcessingStart.set(new Date().getTime());

        lastProcessingEnd = new AtomicLong();
        lastProcessingEnd.set(new Date().getTime());

    }
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
        if(thread.isInterrupted() || stopWorking.get()){
            interruptProcessing();
        }
        //log.debug("checkState(): "+thread.getName()+ " has not been interrupted.");

    }

    public static long getLastProcessingElapsedTime(){
        return lastProcessingEnd.get() - lastProcessingStart.get();
    }


    public static boolean isCurrentlyProcessing(){
        return currentlyProcessing.get();
    }

    public static void setProcessingState(boolean running){

        if(running)
            lastProcessingStart.set(new Date().getTime());
        else
            lastProcessingEnd.set(new Date().getTime());

        currentlyProcessing.set(running);
    }

}
