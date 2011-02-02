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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Vector;
import java.util.Date;

/**
 * User: ndp
 * Date: Sep 5, 2006
 * Time: 1:47:35 PM
 */
public class Jbes {




    public static void main(String[] args) throws Exception{


        if(args.length!=1){
            System.out.println("Usage: Jbes imageFileName");
            System.exit(-1);
        }
        int count;

        FileInputStream fis = new FileInputStream(args[0]);
        FileChannel fc = fis.getChannel();
        byte[] data = new byte[(int)fc.size()];
        ByteBuffer imageData = ByteBuffer.wrap(data);

        try {
            count = fc.read(imageData);
            imageData.flip();
        }
        finally {
            fc.close();
            fis.close();
        }

        double sizeinMB = count/(1024.0*1024.0);
        System.out.println("Read image data. ("+sizeinMB+" MB)");


        Date startTime;
        long elapsed;

        ServerSocketChannel ssc =  ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(10007));
        ssc.configureBlocking(true);


        byte cmd[] = new byte[10];
        ByteBuffer command = ByteBuffer.wrap(cmd);

        boolean done = false;
        while(!done){

            System.out.print("Waiting for connection ... ");
            SocketChannel sc = ssc.accept();

            System.out.println("connected.");
            sc.configureBlocking(true);




            boolean closed = false;

            while(!closed){


                count  = sc.read(command);

                if(count>0){

                    String cmdString = new String(cmd,0,count-2);

                    System.out.println("Got Client Command: \""+cmdString+"\" ("+count+ " bytes)");
                    command.clear();

                    if(cmdString.equalsIgnoreCase("send")){

                        //sc.write(imageData);
                        imageData.rewind();

                        startTime = new Date();

                        //Jbes.sendChunkedDataBlockWrite(sc,imageData.array(),4096);
                        Jbes.sendChunkedDataNIO(sc,imageData,8192);

                        elapsed  = (new Date()).getTime() - startTime.getTime();
                        System.out.println("Sent "+sizeinMB+" MB in "+elapsed+" ms ("+(sizeinMB/(elapsed/1000.0))+" MB/sec)");
                    }
                    else if(cmdString.equalsIgnoreCase("close")){
                        System.out.println("Client requested closed connection...");
                        sc.close();
                        closed = true;

                    }
                    else if(cmdString.equalsIgnoreCase("exit")){
                        System.out.println("Client requested termination, exiting...");
                        sc.close();
                        closed = true;
                        done = true;

                    }
                }
                else if(count <0){
                    System.out.println("Client closed connection...");
                    sc.close();
                    closed = true;
                }
                else if(count ==0){
                    System.out.print(".");
                    Thread.sleep(10);
                }

            }

        }




    }


    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * @param sc
     * @param data
     * @param blockSize
     * @throws IOException
     */
    public static void sendChunkedDataNIO(SocketChannel sc, ByteBuffer data, int blockSize) throws IOException {


        System.out.println("Sending data using NIO.");


        byte[] crlfArray = {0x0d,0x0a};
        ByteBuffer  crlf = ByteBuffer.wrap(crlfArray);

        ByteBuffer chunkSize = ByteBuffer.wrap((StringUtil.toHexString(blockSize,4)).getBytes());


        int remaining = data.remaining();
        int start = 0;
        int end = 0;

        while(remaining>0){


            if(remaining>=blockSize){


                end += blockSize;
                //System.out.println("start: "+start+" end: "+end+"  remaining: "+remaining+"  blockSize: "+blockSize);

                data.position(start);
                data.limit(end);

                start += blockSize;
                remaining -= blockSize;
            }
            else {
                end += remaining;
                System.out.println("Last Chunk  -  start: "+start+" end: "+end+"  remaining: "+remaining+"  blockSize: "+blockSize);

                data.position(start);
                data.limit(end);

                chunkSize = ByteBuffer.wrap((StringUtil.toHexString(remaining,4)).getBytes());


                remaining =0;

            }

            sc.write(chunkSize); chunkSize.rewind();
            sc.write(crlf); crlf.rewind();
            sc.write(data);
            sc.write(crlf); crlf.rewind();


            //data.clear();

        }
        chunkSize = ByteBuffer.wrap((StringUtil.toHexString(0,4)).getBytes());
        sc.write(chunkSize); chunkSize.rewind();
        sc.write(crlf); crlf.rewind();


    }


    /**
     *
     *
     *
     *
     *
     *
     *
     * @param sc
     * @param data
     * @param blockSize
     * @throws IOException
     */
    public static void sendChunkedDataNIO_OLD(SocketChannel sc, ByteBuffer data, int blockSize) throws IOException {

        System.out.println("Sending data using NIO Gathering que.");

        byte[] crlfArray = {0x0d,0x0a};
        ByteBuffer  crlf = ByteBuffer.wrap(crlfArray);

        ByteBuffer chunkSize = ByteBuffer.wrap((StringUtil.toHexString(blockSize,4)).getBytes());

        Vector<ByteBuffer> que = new Vector<ByteBuffer>();

        int remaining = data.remaining();
        int start = 0;
        int end = 0;

        while(remaining>0){


            if(remaining>=blockSize){


                end += blockSize;
                //System.out.println("start: "+start+" end: "+end+"  remaining: "+remaining+"  blockSize: "+blockSize);

                data.position(start);
                data.limit(end);

                start += blockSize;
                remaining -= blockSize;
            }
            else {
                end += remaining;
                System.out.println("Last Chunk  -  start: "+start+" end: "+end+"  remaining: "+remaining+"  blockSize: "+blockSize);

                data.position(start);
                data.limit(end);

                chunkSize = ByteBuffer.wrap((StringUtil.toHexString(remaining,4)).getBytes());

                remaining =0;

            }

            que.add(chunkSize.asReadOnlyBuffer());
            que.add(crlf.asReadOnlyBuffer());
            que.add(data.asReadOnlyBuffer());
            que.add(crlf.asReadOnlyBuffer());

            data.clear();

        }
        chunkSize = ByteBuffer.wrap((StringUtil.toHexString(0,4)).getBytes());
        que.add(chunkSize);
        que.add(crlf);



        ByteBuffer[] bb = new ByteBuffer[que.size()];

        bb = que.toArray(bb);

        //System.out.println("Que Length: "+que.size());
        //System.out.println("bb Length:  "+bb.length);


        int count = 0;
        boolean done = false;
        while(!done){

            if(sc.write(bb) == 0)
                done = true;
            else
                count++;
        }


        System.out.println("It took "+count+" passes to send the que.");



    }


    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * @param sc
     * @param data
     * @param blockSize
     * @throws IOException
     */
    public static void sendChunkedDataBlockWrite(SocketChannel sc, byte[] data, int blockSize) throws IOException {


        System.out.println("Sending data using traditional block writes.");

        byte[] crlf = {0x0d,0x0a};



        int remaining = data.length;
        int start = 0;
        int length = blockSize;
        byte[] chunkSize = (StringUtil.toHexString(length,4)).getBytes();


        OutputStream os = sc.socket().getOutputStream();

        while(remaining>0){

            if(remaining < blockSize){
                length = remaining;
                chunkSize = (StringUtil.toHexString(length,4)).getBytes();
                System.out.println("Last Chunk  -  start: "+start+" length: "+length+"  remaining: "+remaining+"  blockSize: "+blockSize);

            }

            os.write(chunkSize);
            os.write(crlf);
            os.write(data,start,length);
            os.write(crlf);

            start += length;
            remaining -= length;


        }
        chunkSize = (StringUtil.toHexString(0,4)).getBytes();
        os.write(chunkSize);
        os.write(crlf);
        os.close();



    }




}
