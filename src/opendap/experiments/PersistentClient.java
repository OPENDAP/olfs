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

import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * User: ndp
 * Date: Sep 20, 2006
 * Time: 9:24:43 AM
 */
public class PersistentClient {




    public static void main(String[] args) throws Exception{

        Socket sc = new Socket();
        sc.connect(new InetSocketAddress("localhost",3001));

        PrintStream ps = new PrintStream(sc.getOutputStream());
        InputStream  sourceIS = sc.getInputStream();


        String product = "hitcount";

/*
        ps.println(getCmd(product));
        get(sourceIS);
*/
        Requestor req = new Requestor(ps,product);
        Reciever rec  = new Reciever(sourceIS);



        Thread requestor = new Thread(req);
        Thread reciever  = new Thread(rec);
        requestor.start();
        reciever.start();




    }

    private static void get(InputStream is){

        byte[] buf = new byte[8192];
        int cnt;

        while(true){

            try {
                if(is.available()>0){
                    cnt = is.read(buf);
                    System.out.println(new String(buf,0,cnt));
                }
                else
                    Thread.sleep(0,10);

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return;
            }



        }

    }



    private static class Requestor implements Runnable{
        PrintStream myps;
        String myproduct;
        Random myrand;

        Requestor(PrintStream ps,String product){
            myps = ps;
            myproduct = product;
            myrand = new Random();
        }


        public void run() {



            while(true){

                System.out.print(".");


                try {
                    Thread.sleep(myrand.nextInt(1000));

                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    return;
                }

                myps.print(getCmd(myproduct));
                myps.flush();

            }


        }
    }


    private static class Reciever implements Runnable{

        private InputStream is;

        Reciever(InputStream is){
            this.is = is;
        }


        public void run() {

            get(is);

        }
    }




    private static String getCmd(String product){
        return "GET /opendap/nio/" + product + " HTTP/1.1\n" +
                "Accept: */*\n" +
                "Accept-Language: en\n" +
                "Connection: keep-alive\n" + "Host: 127.0.0.1:3001\n" +
                "\n" ;
    }





}


