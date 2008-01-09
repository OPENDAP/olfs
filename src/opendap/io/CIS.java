/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
package opendap.io;

import opendap.bes.DevNull;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * User: ndp
 * Date: Dec 19, 2007
 * Time: 9:14:01 AM
 */
//@todo Add functionality for looking at chunk types.
//@todo close() method and isClosed checking.
//@todo add Stream to Stream transfer of Chunked message.

public class CIS  {

    private Logger log;

    protected InputStream is;
    protected boolean isClosed;


    private byte[] currentChunkHeader = new byte[Chunk.HEADER_SIZE];

    private int currentChunkDataSize;
    private int chunkReadPosition;

    private int currentChunkType;


    private byte[] transferBuffer;



    /**
     * Wraps a stream and chunks the output. Default minimum chunk size is 4096.
     * @param stream to wrap
     */
    public CIS(InputStream stream){
        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        is = stream;
        transferBuffer = new byte[Chunk.MAX_SIZE];
        currentChunkType = Chunk.DATA;
        isClosed = false;

    }

    /**
     * Reads the next chunk of data into the buffer. Method assumes the next
     * bytesn in the stream are the chunk header.
     *
     * @return The number of bytes in the chunk, or -1 if the underlying stream
     * has no more bytes to read.
     * @throws java.io.IOException When the underlying stream does, or if the header is bogus
     *
     */
    public int readChunkHeader() throws IOException {
        if(isClosed) throw new IOException("Cannot read from a closed stream.");

        int ret;


        // Read the header
        ret = Chunk.readFully(is, currentChunkHeader,0, currentChunkHeader.length);

        if(ret==-1)
            return ret;


        // Interpret the header
        int dataSize = Chunk.getDataSize(currentChunkHeader);



        // Cache the Chunk Type.
        currentChunkType = Chunk.getType(currentChunkHeader);


        System.out.println("Chunk Data Size: " + dataSize +
                           "    Chunk Type: "+(char)currentChunkType);


        // If the header contained the final chunk header (0000d) then bail out.
        //if(dataSize == -1){
        //   return -1;
        //}


        currentChunkDataSize = dataSize;
        chunkReadPosition = 0;

        return dataSize;


    }


    public int getCurrentChunkType() {
        return currentChunkType;
    }








    public int read() throws IOException {
        if(isClosed) throw new IOException("Cannot read from a closed stream.");

        int val = is.read();
        chunkReadPosition++;

        if(chunkReadPosition == currentChunkDataSize){
            readChunkHeader();
        }

        return val;

    }



    public int read(byte[] b, int off, int len) throws IOException {
        if(isClosed) throw new IOException("Cannot read from a closed stream.");


        if( (off+len) > b.length)
            throw new IOException("Passed array bounds inconsistent with array " +
                    "size. You cannot put "+len+" bytes starting at location" +
                    " "+off+" into an array whose total length is "+b.length);

        int count, totalBytes=0;
        boolean done = false;


        while(!done){

            if(available() >= len){
                //System.out.println("Reading From Current Chunk");
                count = Chunk.readFully(is,b,off,len);
                if(count==-1){
                    if(totalBytes==0)
                        totalBytes = -1;
                    done =true;
                }
                if(count==len){
                    chunkReadPosition += count;
                    totalBytes+=count;
                    done =true;
                }
            }
            else {
                //System.out.println("Draining Current Chunk");
                count = Chunk.readFully(is,b,off,available());
                if(count==-1){
                    if(totalBytes==0)
                        totalBytes = -1;
                    done =true;
                }
                else {
                    chunkReadPosition += count;
                    off += count;
                    len -= count;
                    totalBytes+=count;
                }

                if(available()==0) {
                    readChunkHeader();
                }
            }
        }

        return totalBytes;

    }


    public int read(byte[] b) throws IOException{

        if(isClosed) throw new IOException("Cannot read from a closed stream.");

        return read(b,0,b.length);

    }


    public int available(){
        return currentChunkDataSize - chunkReadPosition;
    }


    public boolean markSupported(){
        return false;
    }

    public void mark(int readlimit){
    }


    public void reset() throws IOException {
        throw new IOException("Stream marking and reset not supported.");
    }

    public long skip(long n )throws IOException {
        return is.skip(n);
    }


    public void close() throws IOException {
        isClosed = true;
        currentChunkDataSize = 0;
        chunkReadPosition = 0;
        is.close();

    }


    /**
     *
     * @throws IOException
     */
    public void drain() throws IOException {

        if(available()>0){

            DevNull devnull = new DevNull();

            try{
                transferChunkedMessage(devnull);
            } catch (ChunkTransferException e) {
                log.debug("Probelm draining underlying stream: "+e.getMessage());
            }


        }

    }







    /**
     *
     * @param buf
     * @param off
     * @return
     * @throws IOException
     * @throws ChunkTransferException
     */
    public int readChunkedMessage(byte[] buf, int off)
            throws IOException, ChunkTransferException {



        if(available()>0){
            throw new IOException("Data pending in stream. Current message " +
                    "has not been consumed. Use \"drain()\" to dispose of " +
                    "exisiting data.");
        }



        int ret, bytesReceived;
        int totalBytesRead = 0;
        boolean moreData = true;
        while(moreData){


            if(available()>0){

                if((buf.length-off) < available()){
                    throw new IOException("Passed buffer not large enough " +
                            "to accomodate the data remaining in the " +
                            "current Chunk.");
                }

                switch (getCurrentChunkType()){

                    case Chunk.DATA:
                        bytesReceived = Chunk.readFully(is,buf,off,available());
                        off += bytesReceived;
                        totalBytesRead += bytesReceived;
                        chunkReadPosition += bytesReceived;
                        break;

                    case Chunk.EXTENSION:

                        processExtensionChunk();

                        break;

                    default:
                        throw new ChunkTransferException("Unknown Chunk Type.");

                }

            }
            else {

                ret = readChunkHeader();

                if(ret == -1 || isLastChunk()){
                    moreData = false;
                }

            }
        }
        return totalBytesRead;

    }






    /**
     *
     * @param buf
     * @return
     * @throws IOException
     * @throws ChunkTransferException
     */
    public int readChunkedMessage(byte[] buf)
            throws IOException, ChunkTransferException  {

        return readChunkedMessage( buf, 0);

    }





    /**
     *
     * @return True if current chunk is the last chunk in the message.
     */
    public boolean isLastChunk(){
        return currentChunkDataSize==0 && currentChunkType==Chunk.DATA;
    }





    /**
     *
     * @param os
     * @return
     * @throws IOException
     * @throws ChunkTransferException
     */
    public int transferChunkedMessage(OutputStream os) throws IOException,
            ChunkTransferException {

        int ret;
        int totalBytesSent = 0;
        int bytesReceived;


        boolean moreData = true;
        while(moreData){

            if(available()>0){

                switch (getCurrentChunkType()){

                    case Chunk.DATA:
                        bytesReceived = Chunk.readFully(is,transferBuffer,0,available());
                        os.write(transferBuffer,0,bytesReceived);
                        totalBytesSent += bytesReceived;
                        chunkReadPosition += bytesReceived;
                        break;

                    case Chunk.EXTENSION:

                        processExtensionChunk();

                        break;

                    default:
                        throw new ChunkTransferException("Unknown Chunk Type.");

                }

            }
            else {

                ret = readChunkHeader();

                if(ret == -1 || isLastChunk()){
                    moreData = false;
                }

            }

        }

        return totalBytesSent;
    }


    /**
     *
     * @throws ChunkTransferException
     * @throws java.io.IOException
     */
    private void processExtensionChunk()
            throws IOException, ChunkTransferException {


        // Get the extension content from the stream
        byte[] ext = new byte[Chunk.MAX_SIZE];
        int bytesReceived = Chunk.readFully(is,ext,0,available());
        chunkReadPosition += bytesReceived;


        // Make a string out of it
        String extension =  new String(ext,0,bytesReceived);

        // Evaluate the extension information
        if(extension.startsWith(Chunk.STATUS_EXTENSION)){
            String status = extension.substring(extension.indexOf('=')+1,extension.length()-1);
            System.out.println("status: "+status);
            if(status.equalsIgnoreCase(Chunk.ERROR_STATUS)){


                try {
                    String msg;
                    byte[] msgbuf = new byte[Chunk.MAX_SIZE];
                    int count = readChunkedMessage(msgbuf);
                    if(count >0)
                        msg = new String(msgbuf,0,count);
                    else
                        msg = "Unable to read error message associated with " +
                                "chunk transfer extension  with status=error.";

                    throw new ChunkTransferException(msg);
                }
                catch(IOException e){
                    System.out.println("Unable to read error message from chunked " +
                            "stream."+e);
                }



            }
            else if(status.equalsIgnoreCase(Chunk.EXIT_STATUS)){
                throw new ChunkTransferException("Source order connection closed.");

            }
            else {
                throw new ChunkTransferException("Chunk Extension contains an " +
                        "unrecognized status value of: "+status);
            }
        }
        else {
            System.out.println(extension);
        }

    }










    public static void main(String[] args) throws Exception{

//        String test01s = "0011dThisIsATest!0000d";
        String test02s = "001ddThis Is The First Chunk!001edThis Is The Second Chunk!0000d";


        ByteArrayInputStream bais = new ByteArrayInputStream(test02s.getBytes());

        CIS cis = new CIS(bais);



        byte[] buf = new byte[10];
        readbuftest(cis,buf);
        bais.reset();

        buf = new byte[24];
        readbuftest(cis,buf);
        bais.reset();

        buf = new byte[25];
        readbuftest(cis,buf);
        bais.reset();

        buf = new byte[2048];
        readbuftest(cis,buf);
        bais.reset();

    }

    private static void readbuftest(CIS cis, byte[] buf) throws IOException {
        boolean done;
        String buffer;

        System.out.println("\nReading into "+buf.length+" byte Buffer:");

        int cnt;
        done = false;
        while(!done){
            cnt = cis.read(buf,0,buf.length);
            if(cnt==-1){
                done = true;
            }
            else {
                buffer = new String(buf,0,cnt);
                System.out.println("Buffer: "+ buffer);
            }
        }

    }



}
