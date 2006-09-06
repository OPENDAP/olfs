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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

/**
 * User: ndp
 * Date: Sep 5, 2006
 * Time: 3:47:22 PM
 */
public class NioServlet extends HttpServlet {

    public void init() throws ServletException {
        System.out.println("NioServlet loaded.");


    }
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        byte[] crlfArray    = {0x0d,0x0a};
        ByteBuffer crlf     = ByteBuffer.wrap(crlfArray);

        byte[] chunkArray   = new byte[4];
        ByteBuffer chunk    = ByteBuffer.wrap(chunkArray);

        byte[] eolArray     = new byte[2];
        ByteBuffer eol      = ByteBuffer.wrap(eolArray);

        byte[] promptArray  = new byte[2];
        ByteBuffer prompt   = ByteBuffer.wrap(promptArray);

        byte[] dataArray    = new byte[4096];
        ByteBuffer data     = ByteBuffer.wrap(dataArray);


        response.setBufferSize(4096);

        ServletOutputStream os = response.getOutputStream();

        response.setContentType("image/jpeg");
        //response.setContentType("text/ascii");
        response.setHeader("Content-Description", "My Big Picture");

        SocketChannel sc = SocketChannel.open(new InetSocketAddress("localhost",10007));


        sc.configureBlocking(true);

        //sc.read(prompt);


        ByteBuffer send = ByteBuffer.wrap(("send\r\n").getBytes());




        System.out.println("sc.write(send) wrote: "+sc.write(send)+" bytes.");
        //System.out.println("sc.write(crlf) wrote: "+sc.write(crlf)+" bytes.");



        boolean moreData = true;
        while(moreData){
            chunk.clear();
            eol.clear();

            sc.read(chunk);
            sc.read(eol);

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

                data.flip();
                os.write(data.array(),0,chunkSize);

                eol.clear();

                sc.read(eol);




            }

        }

        System.out.println("Closing connections, flushing buffers, etc...");
        os.flush();
        sc.close();
        response.setStatus(200);




    }









}
