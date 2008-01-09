/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2007 OPeNDAP, Inc.
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

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * User: ndp
 * Date: Dec 19, 2007
 * Time: 9:14:01 AM
 */
public class ChunkedInputStream extends InputStream {


    protected InputStream is;
    protected boolean isOpen;


    private byte[] currentChunkHeader = new byte[Chunk.HEADER_SIZE];


    private byte[] cache;
    private int cacheSize;
    private int cacheReadPosition;





    private int currentChunkSize;
    private int chunkReadPosition;






    private int currentChunkType;

    /**
     * Wraps a stream and chunks the output. Default minimum chunk size is 4096.
     * @param stream to wrap
     */
    public ChunkedInputStream(InputStream stream){
        super();

        is = stream;

        cache = new byte[Chunk.MAX_SIZE];
        cacheSize = 0;
        cacheReadPosition = 0;
        currentChunkType = Chunk.DATA;
        isOpen = true;

    }

    /**
     * Reads the next chunk of data into the buffer. Method assumes the next
     * bytesn in the stream are the chunk header.
     *
     * @param is The InputStream containing the chunk to read.
     * @param buf The buffer into which to put the chunk data.
     * @return The number of bytes in the chunk, or -1 if the underlying stream
     * has no more bytes to read.
     * @throws java.io.IOException When the underlying stream does, or if the header is bogus
     *
     */
    public int readChunk(InputStream is, byte[] buf) throws IOException {

        int ret;


        // Read the  the header
        ret = readFully(is, currentChunkHeader,0, currentChunkHeader.length);

        if(ret==-1)
            return ret;

        // Interpret the header
        int dataSize = Chunk.getDataSize(currentChunkHeader);

        // If the header contained the final chunk header (0000d) then bail out.
        if(dataSize == -1){
            cacheSize = 0;
            cacheReadPosition = 0;
            return -1;
        }


        // Cache the Chunbk Type.
        currentChunkType = Chunk.getType(currentChunkHeader);



        System.out.println("Chunk Data Size: " + dataSize +
                           "    Chunk Type: "+(char)currentChunkType);

        // Read the data protion of the chunk.
        ret = readFully(is,cache,0,dataSize);


        if(ret!=-1)
            cacheSize = ret;
        else
            cacheSize = 0;

        cacheReadPosition = 0;

        return ret;


    }


    public int getCurrentChunkType() {
        return currentChunkType;
    }





    private int readFully(InputStream is, byte[] buf, int off, int len) throws IOException{

        boolean done = false;
        int bytesToRead = len;
        int totalBytesRead =0;
        int bytesRead;



        while(!done){
            bytesRead = is.read(buf,off,len);
            if(bytesRead == -1){
                done = true;
            }
            else {
                totalBytesRead += bytesRead;
                if(totalBytesRead == bytesToRead){
                    done = true;
                }
                else {
                    len = bytesToRead - totalBytesRead;
                    off += bytesRead;
                }
            }
        }



        return totalBytesRead;
    }



    public int read() throws IOException {

        int val = cache[cacheReadPosition++];

        if(cacheReadPosition == cacheSize){
            readChunk(is,cache);
        }

        return val;

    }



    public int read(byte[] b, int off, int len) throws IOException {


        if( (off+len) > b.length)
            throw new IOException("Passed array bounds inconsistent with array " +
                    "size. You cannot put "+len+" bytes starting at location" +
                    " "+off+" into an array whose total length is "+b.length);

        int totalBytes=0;
        boolean done = false;
        while(!done){

            if(available() >= len){
                System.arraycopy(cache,cacheReadPosition,b,off,len);
                cacheReadPosition += len;
                totalBytes += len;
                done = true;
            }
            else {
                System.arraycopy(cache,cacheReadPosition,b,off,available());
                len -= available();
                totalBytes += len;
                readChunk(is,cache);
            }
        }

        return totalBytes;
    }


    public int read(byte[] b) throws IOException{
        return read(b,0,b.length);

    }


    public int available(){
        return cacheSize - cacheReadPosition;
    }


    public boolean markSupported(){
        return false;
    }

    public static void main(String[] args) throws Exception{


        String test01s = "0011dThisIsATest!0000d";
        String test02s = "001ddThis Is The First Chunk!001edThis Is The Second Chunk!0000d";


        ByteArrayInputStream bais = new ByteArrayInputStream(test02s.getBytes());

        ChunkedInputStream cis = new ChunkedInputStream(bais);

        cis.readChunk(cis.is,cis.cache);

        String buffer = new String(cis.cache,0,cis.cacheSize);
        System.out.println("Buffer: "+ buffer);

        cis.readChunk(cis.is,cis.cache);
        buffer = new String(cis.cache,0,cis.cacheSize);
        System.out.println("Buffer: "+ buffer);

        cis.readChunk(cis.is,cis.cache);
        buffer = new String(cis.cache,0,cis.cacheSize);
        System.out.println("Buffer: "+ buffer);




    }


}
