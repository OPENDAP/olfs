/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.experiments;


import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.Util;
import opendap.coreServlet.ServletUtil;


/**
 * User: ndp
 * Date: Sep 5, 2006
 * Time: 3:47:22 PM
 */
public class ExperimentSandboxServlet extends HttpServlet {


    private int maxChunkSize = 8192;

    private AtomicInteger hitCounter;
    private AtomicInteger unitHitCounter;

    private Random rand;

    private Logger log;

    public void init() throws ServletException {


        hitCounter = new AtomicInteger(0);
        unitHitCounter =  new AtomicInteger(0);

        rand = new Random();

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        log.info("ExperimentSandboxServlet loaded.");



    }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {


        try {

            String path = request.getPathInfo();


            int reqno = hitCounter.incrementAndGet();




            String msg = "NOOP";


            Date startTime = new Date();
            Date endTime   = startTime;


            if(path == null){

            }else if(path.equals("/nio") || path.equals("/nio/")){

                msg = ("NIOREAD");
                startTime = new Date();
                doNIO(request,response);
                endTime = new Date();
            }
            else if(path.equals("/block") || path.equals("/block/")){

                msg = ("BLOCKREAD");
                startTime = new Date();
                doBLOCK(request,response);
                endTime = new Date();
            }

            else if(path.equals("/byte") || path.equals("/byte/")){

                msg = ("BYTEREAD");
                startTime = new Date();
                doBYTE(request,response);
                endTime = new Date();
            }


            else if(path.equals("/loadTHREDDS") || path.equals("/byte/")){

                msg = ("LOAD_THREDDS");
                startTime = new Date();
                loadTHREDDS(request,response);
                endTime = new Date();
            }


            else if(path.equals("/memory") || path.equals("/byte/")){

                msg = ("MEMORY");
                startTime = new Date();
                memory(request,response);
                endTime = new Date();
            }


            else if(path.equals("/hitcount") || path.equals("/hitcount/")){

                msg = ("HitCounter");
                startTime = new Date();

                synchronized(this){
                    int unitreqno = unitHitCounter.incrementAndGet();
                }

                try {
                    Thread.sleep(rand.nextInt(200));
                } catch (InterruptedException e) {
                    log.error("Hmmmm, my thread got interrupted.",e);
                }

                doHitCount(request,response);
                endTime = new Date();
            }




            long elapsed = endTime.getTime() - startTime.getTime();

            log.debug(msg + "_Elapsed_Time: "+elapsed+" ms");

        }
        catch(Throwable t){
            OPeNDAPException.anyExceptionHandler(t, response);

        }




    }

    private void loadTHREDDS(HttpServletRequest request,
                             HttpServletResponse response) {


        String contextPath = ServletUtil.getContextPath(this);
        String contentPath = ServletUtil.getContentPath(this);
        Date startTime, endTime;

        String filename=contentPath + "CEOP/ReferenceSite/catalog.xml";

        try {
            PrintWriter pw = response.getWriter();

            response.setContentType("text/html");
            response.setHeader("Content-Description", "dods_status");
            response.setStatus(HttpServletResponse.SC_OK);


            pw.println("<hr/><h3>Start:</h3>");
            Util.printMemoryReport(pw);
            pw.flush();


            startTime = new Date();
            long snTime = System.nanoTime();
            // ... the code being measured ...
            ThreddsCatalog tc = new ThreddsCatalog(filename);


            long estimatedTime = System.nanoTime() - snTime;


            endTime = new Date();


            long elapsed = endTime.getTime() - startTime.getTime();
            pw.println("<h3>"+"Loaded "+tc.size()+" THREDDS datasets in an " +
                    "elpased time of "+elapsed+" ms  (" + estimatedTime +
                    " ns)</h3>");
            Util.printMemoryReport(pw);

            pw.println("<h3>Running Garbage Collector...</h3>");

            Runtime r = Runtime.getRuntime();
            r.gc();


            Util.printMemoryReport(pw);
            pw.println("<h3>End</h3><hr/>");





        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }

    }


    private void memory(HttpServletRequest request,
                             HttpServletResponse response) {

        try {
            PrintWriter pw = response.getWriter();

            response.setContentType("text/html");
            response.setHeader("Content-Description", "dods_status");
            response.setStatus(HttpServletResponse.SC_OK);


            pw.println("<hr/><pre><h3>Start:</h3>");
            Util.printMemoryReport(pw);
            pw.flush();

            pw.println("<h3>Running Garbage Collector...</h3>");

            Runtime r = Runtime.getRuntime();
            r.gc();


            Util.printMemoryReport(pw);
            pw.println("<h3>End</h3></pre><hr/>");


        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }

    }







    public void doHitCount(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/ascii");
        response.setHeader("Content-Description", "My Hit Counter");



        ServletOutputStream os = response.getOutputStream();


        os.println("Total Hits:       "+hitCounter);
        os.println("Hit Counter Hits: "+unitHitCounter);
        os.println("\n\n");

    }










    public void doNIO(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {




        byte[] chunkArray   = new byte[4];
        ByteBuffer chunk    = ByteBuffer.wrap(chunkArray);

        byte[] crlfArray     = new byte[2];
        ByteBuffer crlf      = ByteBuffer.wrap(crlfArray);


        byte[] dataArray    = new byte[maxChunkSize];
        ByteBuffer data     = ByteBuffer.wrap(dataArray);

        ByteBuffer send = ByteBuffer.wrap(("send\r\n").getBytes());




        ServletOutputStream os = response.getOutputStream();

        response.setContentType("image/jpeg");
        //response.setContentType("text/ascii");
        response.setHeader("Content-Description", "My Big Picture");

        SocketChannel sc = SocketChannel.open(new InetSocketAddress("localhost",10007));


        sc.configureBlocking(true);

        sc.write(send);

        boolean moreData = true;
        while(moreData){
            chunk.clear();
            crlf.clear();

            sc.read(chunk);
            sc.read(crlf);

            int chunkSize = Integer.valueOf(new String(chunkArray),16);

            //System.out.println("chunkSize: "+chunkSize);

            if(chunkSize == 0) {
                moreData = false;
            }
            else {

                data.clear();
                data.limit(chunkSize);

                int count=0;

                boolean done = false;
                while(!done){
                    count += sc.read(data);
                    if(count == chunkSize)
                       done = true;
                    //System.out.println("count: "+count);
                }

                os.write(data.array(),0,chunkSize);
                crlf.clear();
                sc.read(crlf);
            }
        }
        //System.out.println("Closing connections, flushing buffers, etc...");
        os.flush();
        sc.close();
        response.setStatus(200);
    }




    public void doBLOCK(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {




        byte[] chunk  = new byte[   4];
        byte[] crlf   = new byte[   2];
        byte[] data   = new byte[maxChunkSize];

        byte[] send = ("send\r\n").getBytes();


        ServletOutputStream os = response.getOutputStream();

        response.setContentType("image/jpeg");
        //response.setContentType("text/ascii");
        response.setHeader("Content-Description", "My Big Picture");
        response.setStatus(200);

        Socket sc = new Socket();
        sc.connect(new InetSocketAddress("localhost",10007));

        OutputStream sourceOS = sc.getOutputStream();
        InputStream  sourceIS = sc.getInputStream();




        //System.out.println("sc.write(send) wrote: "+sc.write(send)+" bytes.");
        sourceOS.write(send);


        boolean moreData = true;
        while(moreData){
            completeRead(sourceIS,chunk);
            completeRead(sourceIS,crlf);

            int chunkSize = Integer.valueOf(new String(chunk),16);

            //System.out.println("chunkSize: "+chunkSize);

            if(chunkSize == 0) {
                moreData = false;
            }
            else {


                completeRead(sourceIS,data,0,chunkSize);

                os.write(data,0,chunkSize);

                completeRead(sourceIS,crlf);


            }

        }

        //System.out.println("Closing connections, flushing buffers, etc...");
        os.flush();
        sourceOS.close();
        sourceIS.close();
        sc.close();





    }
    public void doBYTE(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {




        byte[] chunk  = new byte[   4];
        byte[] crlf   = new byte[   2];

        byte[] send = ("send\r\n").getBytes();


        ServletOutputStream os = response.getOutputStream();

        response.setContentType("image/jpeg");
        //response.setContentType("text/ascii");
        response.setHeader("Content-Description", "My Big Picture");
        response.setStatus(200);

        Socket sc = new Socket();
        sc.connect(new InetSocketAddress("localhost",10007));

        OutputStream sourceOS = sc.getOutputStream();
        InputStream  sourceIS = sc.getInputStream();




        //System.out.println("sc.write(send) wrote: "+sc.write(send)+" bytes.");
        sourceOS.write(send);


        int i;
        boolean moreData = true;
        while(moreData){
            byteRead(sourceIS,chunk);
            byteRead(sourceIS,crlf);

            int chunkSize = Integer.valueOf(new String(chunk),16);

            //System.out.println("chunkSize: "+chunkSize);

            if(chunkSize == 0) {
                moreData = false;
            }
            else {


                for(i=0; i<chunkSize; i++){

                    os.write(sourceIS.read());

                }

                byteRead(sourceIS,crlf);


            }

        }

        //System.out.println("Closing connections, flushing buffers, etc...");
        os.flush();
        sourceOS.close();
        sourceIS.close();
        sc.close();




    }



    public void byteRead(InputStream is, byte[] data) throws IOException {

        for(int i=0; i<data.length; i++){
            data[i] = (byte) is.read();
        }



    }

    public void completeRead(InputStream is, byte[] data) throws IOException {

        completeRead(is,data,0,data.length);



    }


    public void completeRead(InputStream is, byte[] data, int offset, int length) throws IOException {

        int readCount=0;


        while(readCount < length){
            readCount += is.read(data,offset+readCount,length-readCount);
        }



    }



}
