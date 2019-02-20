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

package opendap.bes;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;



/**
 *
 *
 **/
public class CyclicGroup<E> {


    Logger log;
    private Vector<E> members;
    private AtomicInteger nextMemberIndex;

    private ConcurrentHashMap<String, E> membersNameIndex;

    private final ReentrantLock lock = new ReentrantLock();


    public CyclicGroup() {
        log = LoggerFactory.getLogger(getClass());

        members = new Vector<E>();
        membersNameIndex = new ConcurrentHashMap<String, E>();
        nextMemberIndex = new AtomicInteger();
        nextMemberIndex.set(0);

    }


    /*
    public void addBes(BESConfig config) throws Exception {

        if(!config.getPrefix().equals(prefix))
            throw new BadConfigurationException("Members of a BesGroup must all have the same prefix. " +
                    "This ring has prefix '"+prefix+"' the BESConfig  has a prefix of '"+config.getPrefix()+"'.");

        BES bes = new BES(config);
        add(bes);


    }
    */

    public boolean add(String name, E e) {
        lock.lock();
        try {

            membersNameIndex.put(name,e);
            members.add(e);


        } finally {
            lock.unlock();
        }
        return true;

    }

    /**
     * The cyclic behavior is encoded here.
     * @return The next BES in the group cycle.
     */
    public E getNext() {

        // Locking ensures that we don't accidentally cause a permutation of the members of the queue, since
        // the lock enforces that only one thread will be able to take and add at a time.
        lock.lock();
        try {
            int index = nextMemberIndex.getAndIncrement() % members.size();
            log.debug("getNext(): "+Thread.currentThread().getName()+" is retrieving index "+index);
            return members.get(index);     // Return it to the requester.
        } finally {
            lock.unlock();
        }
    }



    /**
     * The ring like behavior is encoded here. When you get the "next" BES in the ring the head of a queue is returned,
     * and the head is returned to the end of the queue. Thus each BES in the ring
     * @param name The nickName of the BES that is desired.
     * @return The request BES if found, null otherwise.
     */
    public E get(String name) {

        // Locking ensures that we don't accidentally cause a permutation of the members of the queue, since
        // the lock enforces that only one thread will be able to take and add at a time.
        lock.lock();
        try {

            return membersNameIndex.get(name);


        } finally {
            lock.unlock();
        }
    }



    public E get(int i) {
        lock.lock();
        try {
            return members.get(i);     // Return it to the requester.
        } finally {
            lock.unlock();
        }
    }


    public int size() {
        lock.lock();
        try {
            return members.size();     // Return it to the requester.
        } finally {
            lock.unlock();
        }
    }


    public boolean isEmpty() {
        lock.lock();
        try {
            return members.isEmpty();     // Return it to the requester.
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(E o) {
        lock.lock();
        try {
            return members.contains(o);     // Return it to the requester.
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            members.clear();
            membersNameIndex.clear();
        } finally {
            lock.unlock();
        }
    }


    public Object[] drain(){
        lock.lock();
        try {
            Object[] o = members.toArray();
            clear();
            return o;         // Return it to the requester.
        } finally {
            lock.unlock();
        }

    }

    public E[] drain(E[] e){
        lock.lock();
        try {
            E[] o = members.toArray(e);
            clear();
            return o;         // Return it to the requester.
        } finally {
            lock.unlock();
        }

    }

    public Object[] toArray(){
        lock.lock();
        try {
            return members.toArray();         // Return it to the requester.
        } finally {
            lock.unlock();
        }

    }

    public E[] toArray(E[] e){
        lock.lock();
        try {
            return members.toArray(e);         // Return it to the requester.
        } finally {
            lock.unlock();
        }

    }





    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(CyclicGroup.class);

        int testThreadCount = 17;
        int groupSize = 13;
        final int  strokesToGive = 11;

         class TestThing {
             private final ReentrantLock lock = new ReentrantLock();

             private String _name;
             private AtomicInteger _touched;
             private Vector<String> _strokers;
             private Vector<String> _strokes;


            public TestThing(String name){
                _name = name;
                _touched = new AtomicInteger();
                _touched.set(0);
                _strokers = new Vector<>();
                _strokes = new Vector<>();
            }

            public int stroke(){

                lock.lock();
                try {
                    int stroke =  _touched.incrementAndGet();
                    _strokers.add(Thread.currentThread().getName());
                    _strokes.add(stroke+"");
                    return stroke;
                }
                finally {
                    lock.unlock();
                }
            }

            public int strokes(){
                return _touched.get();
            }

            public String getName(){
                return _name;
            }

        }


        class TestThread extends Thread {

            private Logger log;
            private CyclicGroup<TestThing> _cg;
            private String _name;
            private CountDownLatch _startSignal;
            private CountDownLatch _doneSignal;

            public TestThread(String name, CyclicGroup<TestThing> cg, CountDownLatch startSignal, CountDownLatch doneSignal){
                log = LoggerFactory.getLogger(this.getClass());
                _cg = cg;
                _name = name;
                _startSignal = startSignal;
                _doneSignal  = doneSignal;
            }

            public void run() {
                try{
                    log.info("{} - Waiting for green light...",_name);
                    _startSignal.await();

                    String msg = _name + " - Running. Delivering {}  strokes to {} objects.";
                    log.info(msg,strokesToGive,_cg.size());

                    for(int i=0; i<strokesToGive ;i++){
                        TestThing tt = _cg.getNext();
                        int strokesReceived = tt.stroke();
                        msg = _name+ " stroked {} which now has received {} strokes.";
                        log.info(msg,tt.getName(),strokesReceived );
                    }

                } catch (InterruptedException e) {
                    log.warn("Caught Interrupted exception. Bailing... Msg: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                } finally {
                    log.info("{} - Finished",_name);
                    _doneSignal.countDown();
                }

            }

        }



        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(testThreadCount);

        CyclicGroup<TestThing> cyclicGroup = new CyclicGroup<TestThing>();
        for(int i=0; i<groupSize ;i++){
            String name = "thing-"+(i<10?"0"+i:i);
            cyclicGroup.add(name, new TestThing(name));
        }


        TestThread[] testThreads = new TestThread[testThreadCount];

        for(int i=0; i<testThreadCount ;i++){
            testThreads[i] = new TestThread("thread_"+ (i<10?"0"+i:i),cyclicGroup, startSignal, doneSignal);
        }

        for(int i=0; i<testThreadCount ;i++){
            testThreads[i].start();
        }


        startSignal.countDown();

        try {
            doneSignal.await();
            String name, number;
            log.info("Final ordering:");
            int last;
            int totalStrokesGiven = 0;

            last = -1;
            for(TestThing thisThing: cyclicGroup.members){
                name = thisThing.getName();
                number = name.substring(name.length() - 2, name.length());
                int thisVal = Integer.parseInt(number);

                totalStrokesGiven += thisThing.strokes();

                log.info("{} has {} strokes.",name,thisThing.strokes());

                int j=0;
                for(String stroker : thisThing._strokers){
                    String stroke = thisThing._strokes.get(j++);
                    log.info("{} applied the {} stroke.",stroker,stroke);
                }


                last = thisVal;
            }

            log.info("last: {}", last);
            log.info("threadCount: {}", testThreadCount);
            log.info("strokesToGive: {}",strokesToGive);
            log.info("totalStrokesGiven: {} {}",totalStrokesGiven, (((strokesToGive*testThreadCount)!=totalStrokesGiven)?"FAIL":"SUCCESS"));
            log.info("Strokes distributed across {} TestThings.", groupSize);

        } catch (InterruptedException e) {
            log.warn("Caught Interrupted exception. Bailing... Msg: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }





}
