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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentRing<E> extends ArrayBlockingQueue<E>  {

    private final ReentrantLock lock = new ReentrantLock();
    private Logger log;


    public ConcurrentRing(int capacity) {
        super(capacity);
        log = LoggerFactory.getLogger(this.getClass());
    }

    public ConcurrentRing(int capacity, boolean fair) {
        super(capacity, fair);
    }

    public ConcurrentRing(int capacity, boolean fair,  Collection<? extends E> c) {
        super(capacity, fair, c);
    }

    @Override
    public boolean contains(Object o) {
        lock.lock();
        try {
            return super.contains(o);   // Check for containment when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean containsAll(Collection<?> f) {
        lock.lock();
        try {
            return super.containsAll(f);   // Check for containment when when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public int drainTo(Collection<? super E> f) {
        lock.lock();
        try {
            return super.drainTo(f);   // Drain the ring when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public int drainTo(Collection<? super E> f, int i) {
        lock.lock();
        try {
            return super.drainTo(f,i);   // Drain the ring when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return super.isEmpty();   // Return isEmpty when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public Iterator<E> iterator() {
        lock.lock();
        try {
            return super.iterator();   // Return the iterator when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }



    @Override
    public int remainingCapacity() {
        lock.lock();
        try {
            return super.remainingCapacity();   // Return the remainingCapacity when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean remove(Object o) {
        lock.lock();
        try {
            return super.remove(o);   // Remove the object when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public E remove() {
        lock.lock();
        try {
            return super.remove();   // Remove the head of the Ring when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean removeAll(Collection<?> c) {
        lock.lock();
        try {
            return super.removeAll(c);   // Remove everything  when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean retainAll(Collection<?> c) {
        lock.lock();
        try {
            return super.retainAll(c);   // Retain the passed stuff and remove everything else when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public int size() {
        lock.lock();
        try {
            return super.size();   // Return the size when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public E take() throws InterruptedException {
        lock.lock();
        try {
            return super.take();   // Take the head when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public Object[] toArray() {
        lock.lock();
        try {
            return super.toArray();   // Return the array when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }


    @Override
    public <E> E[] toArray(E[] a) {
        lock.lock();
        try {
            return super.toArray(a);   // Return the array when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }



    @Override
    public String toString() {
        lock.lock();
        try {
            return super.toString();   // Return the array when nobody else is messing with it...
        } finally {
            lock.unlock();
        }

    }

    @Override
    public boolean add(E e){
        lock.lock();
        try {
            return super.add(e);   // Add a member when nobody else is messing with it...
        } finally {
            lock.unlock();
        }
    }



    @Override
    public boolean addAll(Collection<? extends E> e){
        lock.lock();
        try {
            return super.addAll(e);   // Add members when nobody else is messing with it...
        } finally {
            lock.unlock();
        }
    }





    /**
     * The ring like behavior is encoded here. When you get the "next" BES in the ring the head of a queue is returned,
     * and the head is returned to the end of the queue. Thus each BES in the ring
     * @return
     */
    public E getNext() {
        E t;

        // Locking ensures that we don't accidentally cause a permutation of the members of the queue, since
        // the lock enforces that only one thread will be able to take and add at a time.
        lock.lock();
        try {
            t = super.take();   // Take the head from the queue.
            add(t);       // Now put it right back on the tail of the queue.
            return t;     // Return it to the requester.
        } catch (InterruptedException e) {
            log.error("getNext() - Caught Interrupted exception returning 'null'. Msg: "+e.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }


    public static void main(String[] args) {


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

            ConcurrentRing<TestThing> _cr;
            String _name;
            CountDownLatch _startSignal, _doneSignal;

            public TestThread(String name, ConcurrentRing<TestThing> cr, CountDownLatch startSignal, CountDownLatch doneSignal){
                _cr = cr;
                _name = name;
                _startSignal = startSignal;
                _doneSignal  = doneSignal;
            }

            public void run() {

                try{
                    _startSignal.await();

                    int strokes_to_give = _cr.size()*1000;


                    for(int i=0; i<strokes_to_give ;i++){
                        TestThing tt = _cr.getNext();
                        int strokesReceived = tt.stroke();
                        System.out.println(_name+ " stroked "+tt.getName()+" which now has received "+ strokesReceived +" strokes.");
                    }




                } catch (InterruptedException e) {
                    System.err.println("run() - Caught Interrupted exception. Bailing... Msg: " + e.getMessage());
                } finally {
                    _doneSignal.countDown();
                }

            }

        }

        int testThreadCount = 17;
        int ringSize = 13;


        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(testThreadCount);

        ConcurrentRing<TestThing> cr = new ConcurrentRing<TestThing>(ringSize);
        for(int i=0; i<ringSize ;i++){
            cr.add(new TestThing("thing-"+(i<10?"0"+i:i)));
        }


        TestThread[] testThreads = new TestThread[testThreadCount];

        for(int i=0; i<testThreadCount ;i++){
            testThreads[i] = new TestThread("thread_"+ (i<10?"0"+i:i),cr, startSignal, doneSignal);
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

            /*
            last = -1;
            for(int i=0; i<ringSize ;i++){
                TestThing thisThing = cr.getNext();
                name = thisThing.getName();
                int thisVal = Integer.parseInt(name.substring(name.length() - 2), name.length());
                System.out.println("TestThing(" + thisVal + ") has " + thisThing.strokes() + " strokes.");
                if(thisVal<=last)
                    System.err.println("Order of ring members has been permutated!");
                last = thisVal;
            }
            System.out.println("");
             */


            last = -1;
            for(int i=0; i<ringSize ;i++){
                TestThing thisThing = cr.getNext();
                name = thisThing.getName();
                number = name.substring(name.length() - 2, name.length());
                int thisVal = Integer.parseInt(number);
                System.out.println(name +" has " + thisThing.strokes() + " strokes.");
                if(thisVal<=last)
                    System.err.println("Order of ring members has been permutated!");


                int j=0;
                for(String stroker : thisThing._strokers){
                    String stroke = thisThing._strokes.get(j++);
                    System.out.println(stroker +" applied the " + stroke + " stroke.");
                }


                last = thisVal;
            }
            System.out.println("");






        } catch (InterruptedException e) {
            System.err.println("main() - Caught Interrupted exception. Bailing... Msg: " + e.getMessage());
        }


    }


}
