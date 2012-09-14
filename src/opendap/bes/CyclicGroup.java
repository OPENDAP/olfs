/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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
                _strokers = new Vector<String>();
                _strokes = new Vector<String>();
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

            CyclicGroup<TestThing> _cg;
            String _name;
            CountDownLatch _startSignal, _doneSignal;

            public TestThread(String name, CyclicGroup<TestThing> cg, CountDownLatch startSignal, CountDownLatch doneSignal){
                _cg = cg;
                _name = name;
                _startSignal = startSignal;
                _doneSignal  = doneSignal;
            }

            public void run() {


                try{
                    System.out.println(_name+" - Waiting for green light...");
                    _startSignal.await();

                    System.out.println(_name+" - Running: Delivering "+strokesToGive+" strokes to "+_cg.size()+" objects...");


                    for(int i=0; i<strokesToGive ;i++){
                        TestThing tt = _cg.getNext();
                        int strokesReceived = tt.stroke();
                        System.out.println(_name+ " stroked "+tt.getName()+" which now has received "+ strokesReceived +" strokes.");
                    }




                } catch (InterruptedException e) {
                    System.err.println("run() - Caught Interrupted exception. Bailing... Msg: " + e.getMessage());
                } finally {
                    System.out.println(_name+" - Finished");
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
            System.out.println("Final ordering:");
            int last;
            int totalStrokesGiven = 0;

            last = -1;
            for(TestThing thisThing: cyclicGroup.members){
                name = thisThing.getName();
                number = name.substring(name.length() - 2, name.length());
                int thisVal = Integer.parseInt(number);

                totalStrokesGiven += thisThing.strokes();

                System.out.println(name +" has " + thisThing.strokes() + " strokes.");

                int j=0;
                for(String stroker : thisThing._strokers){
                    String stroke = thisThing._strokes.get(j++);
                    //System.out.println(stroker +" applied the " + stroke + " stroke.");
                }


                last = thisVal;
            }
            System.out.println("");

            System.out.println("");
            System.out.println("threadCount: "+testThreadCount);
            System.out.println("strokesToGive: "+strokesToGive);
            System.out.println("totalStrokesGiven: "+totalStrokesGiven + " "+((strokesToGive*testThreadCount)!=totalStrokesGiven?"FAIL":"SUCCESS"));
            System.out.println("Strokes distributed across "+groupSize+" TestThings.");








        } catch (InterruptedException e) {
            System.err.println("main() - Caught Interrupted exception. Bailing... Msg: " + e.getMessage());
        }


    }





}
