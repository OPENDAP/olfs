/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.niotest;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Vector;

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
        int count = 0;

        FileInputStream fis = new FileInputStream(args[0]);

        FileChannel fc = fis.getChannel();


        ByteBuffer imageData = ByteBuffer.allocate((int)fc.size());

        byte prmpt[] = {(byte)'%',(byte)' '};
        ByteBuffer prompt = ByteBuffer.wrap(prmpt);

        byte cmd[] = new byte[10];
        ByteBuffer command = ByteBuffer.wrap(cmd);

        count = fc.read(imageData);
        fc.close();

        System.out.println("Read image data. ("+count+" bytes)");

        imageData.flip();

        ServerSocketChannel ssc =  ServerSocketChannel.open();

        ssc.socket().bind(new InetSocketAddress(10007));
        ssc.configureBlocking(true);

        boolean done = false;


        while(!done){

            System.out.print("Waiting for connection ... ");
            SocketChannel sc = ssc.accept();

            System.out.println("connected.");
            sc.configureBlocking(true);



            boolean closed = false;

            while(!closed){


                //sc.write(prompt);
                //prompt.rewind();

                count  = sc.read(command);

                if(count>0){

                    String cmdString = new String(cmd,0,count-2);

                    System.out.println("Got Client Command: \""+cmdString+"\" ("+count+ " bytes)");
                    command.clear();

                    if(cmdString.equalsIgnoreCase("send")){

                        //sc.write(imageData);
                        //imageData.rewind();

                        Jbes.sendChunkedData(sc,imageData,4096);
                    }
                    else if(cmdString.equalsIgnoreCase("close")){
                        System.out.println("Client requested closed connection...");
                        closed = true;

                    }
                    else if(cmdString.equalsIgnoreCase("exit")){
                        System.out.println("Client requested termination, exiting...");
                        closed = true;
                        done = true;

                    }
                }
                else if(count <0){
                    System.out.println("Client closed connection...");
                    closed = true;
                }
                else if(count ==0){
                    System.out.print(".");
                    Thread.sleep(10);
                }

            }

        }




    }

    public static void sendChunkedData(SocketChannel sc, ByteBuffer data, int blockSize) throws IOException {

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





}
