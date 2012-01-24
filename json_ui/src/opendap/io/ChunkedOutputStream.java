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
package opendap.io;

import org.slf4j.Logger;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 *
 * Implements Hyarx chunking support (which might be considered a protoype
 * implementation of DAP4 chunked transmission protocol).
 *
 * Writes are buffered to an internal buffer (default size is 4096 bytes).
 * Chunks are guaranteed to be at least as large as the buffer size (except
 * for the last chunk).
 *
 *
 *
 * User: ndp
 * Date: Dec 19, 2007
 * Time: 9:14:18 AM
 */
public class ChunkedOutputStream  extends OutputStream {

    private Logger log;


    protected OutputStream _rawOS;

    private int minimumChunkSize;

    private byte[] cache;
    private int cacheSize;


    private int currentChunkType;

    protected boolean isOpen;
    protected String closedMsg = "ChunkedOutputStream has been closed.";



    /**
     * Wraps a stream and chunks the output. Default minimum chunk size is 4096.
     * @param stream to wrap
     */
    public ChunkedOutputStream(OutputStream stream){
        super();
        init(stream,Chunk.DEFAULT_SIZE);

    }

    /**
     * Wraps a stream and chunks the output.
     * @param stream to wrap
     * @param minChunkSize minimum chunk size (excluding last chunk)
     * @throws Exception if the minimum chunk size is larger than 65535 (0xFFFF)
     * bytes.
     */
    public ChunkedOutputStream(OutputStream stream, int minChunkSize) throws Exception {

        super();


        if(minChunkSize > Chunk.MAX_SIZE)
            throw new Exception("Minimum chunk size must be less than "+
                    Chunk.MAX_SIZE + " bytes. You asked for "+minChunkSize
                    + " bytes.");

        init(stream,minChunkSize);

    }

    private void init(OutputStream stream, int minChunkSize){

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _rawOS = stream;

        minimumChunkSize = minChunkSize;
        cache = new byte[minimumChunkSize];
        cacheSize = 0;
        currentChunkType = Chunk.DATA;
        isOpen = true;

    }


    public void setChunkType(int type) throws Exception {


        switch(type) {
            case Chunk.DATA:
                currentChunkType = type;
                break;

            case Chunk.EXTENSION:
                currentChunkType = type;
                break;

            default:
                throw new Exception("Unknown Chunk Type");
        }



    }

    public void setChunkTypeToDATA()  {
        currentChunkType = Chunk.DATA;
    }

    public void setChunkTypeToEXTENSION()  {
        currentChunkType = Chunk.EXTENSION;
    }



    public int getChunkType(){
        return currentChunkType;

    }




    /**
     * Finishes writing to the underlying stream, but does NOT close the
     * underlying stream.
     *
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void close() throws IOException {


        finish();


        isOpen = false;

    }


    /**
     * Completes a chunked transmission. Must be called to ensure the
     * internal cache is flushed and the closing chunk is written. The stream
     * is not closed and is left ready to begin a new chunked transmission.
     *
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void finish() throws IOException {

        if(!isOpen) throw new IOException(closedMsg);
        log.debug("finish()");

        flushCache();
        Chunk.writeClosingChunkHeader(_rawOS);
        _rawOS.flush();

    }

    /**
     * Flushes the underlying stream, but leaves the internal buffer alone.
     *
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void flush() throws IOException {
        if(!isOpen) throw new IOException(closedMsg);
        log.debug("flush(): Flushing underlying output stream.");

        _rawOS.flush();

    }

    /**
     * Writes the cache out onto the underlying stream.
     *
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    protected void flushCache() throws IOException {
        if(!isOpen) throw new IOException(closedMsg);


        if(cacheSize>0){
            Chunk.writeChunkHeader(_rawOS,cacheSize,currentChunkType);
            StringBuilder msg = new StringBuilder();
            msg.append("flushCache() - cache contains: \"").append(new String(cache,0,cacheSize)+"\"");
            log.debug(msg.toString());
            _rawOS.write(cache,0,cacheSize);
            cacheSize = 0;
        }
    }


    /**
     * Writes the cache and bufferToAppend to the underlying stream as one
     * large chunk. If combined cache and bufferToAppend are to large to
     * stringToHex as a single chunk then it will be sent as multiple chunks. The
     * last chunk in the series MIGHT NOT be a full size chunk.
     *
     * @param bufferToAppend The buffer to append to the cache.
     * @param off Offset within the buffer.
     * @param len Number of bytes to write from the buffer
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    protected void flushCacheWithAppend(byte[] bufferToAppend, int off, int len) throws IOException {
        if(!isOpen) throw new IOException(closedMsg);

        int chunkSize = cacheSize + len;


        // Too big?
        if(chunkSize > Chunk.MAX_SIZE) {

            int bytesToWrite = len;

            len = Chunk.MAX_SIZE -  cacheSize;

            // Push it out in managable chunks.
            while(len>0){
                flushCacheWithAppend(bufferToAppend,  off,  len);
                bytesToWrite = bytesToWrite - len;
                off += len;
                if(bytesToWrite > Chunk.MAX_SIZE){
                    len = Chunk.MAX_SIZE;
                }
                else {
                    len = bytesToWrite;
                }
            }

        }
        else {
            Chunk.writeChunkHeader(_rawOS,chunkSize,currentChunkType);
            _rawOS.write(cache,0,cacheSize);
            _rawOS.write(bufferToAppend,off,len);
            cacheSize = 0;
        }
    }



    /**
     * Writes the array. If the array does not fit within the buffer,
     * it is not split, but rather written out as one large chunk.
     *
     * @param b The buffer to write.
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void write(byte[] b) throws IOException {

        write(b,0,b.length);

    }

    /**
     *
     * @param b The buffer to write.
     * @param off Offset within the buffer.
     * @param len Number of bytes to write from the buffer
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        if(!isOpen) throw new IOException(closedMsg);

        if( (off+len) > b.length)
            throw new IOException("Passed array bounds inconsistent with array " +
                    "size. You cannot read "+len+" bytes starting at location" +
                    " "+off+" from an array whose total length is "+b.length);


        int chunkSize = cacheSize + len;

        if(chunkSize < cache.length){
            System.arraycopy(b,off,cache,cacheSize,len);
            cacheSize += len;
        }
        else {
            flushCacheWithAppend(b,off,len);
        }


    }

    /**
     * Write the specified byte to our output stream.
     *
     * @param b The byte to be written.
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public void write(int b) throws IOException {
        if(!isOpen) throw new IOException(closedMsg);

        cache[cacheSize] = (byte) b;
        cacheSize++;

        log.debug("write(byte) - cacheSize: "+cacheSize);
        if(cacheSize >= cache.length){
            flushCache();
        }


    }




    public static void main(String[] args) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ChunkedOutputStream cs = new ChunkedOutputStream(baos,50);

        System.out.println(cs);

        String s = "I'm in ur code, testin' ur methods.";

        cs.write(s.getBytes());

        s = "Another value: ";

        String val = "";
        for(int i=1; i<10 ;i++){
            cs.write(s.getBytes());
            val += i;
            for (byte b : val.getBytes()) cs.write(b);
        }

        cs.finish();
        String buffer = new String(baos.toByteArray());
        System.out.println("Buffer: "+buffer);


        /*

        // This was a viable crawlTest when we used 4 bytes to encode the chunk size.
        // That meant that the MAX_SIZE was only 2^16. Now that it's 2^28 it's
        // not feasible to crawlTest the boundary condition of a chunk larger than
        // the maximum chunk size.
        baos.reset();

        byte[] buf = new byte[Chunk.MAX_SIZE +10];

        for(int i=0; i< Chunk.MAX_SIZE +10 ; i++)
            buf[i] = (byte) i;

        cs.write(buf);

        cs.close();

        buffer = new String(baos.toByteArray());



        System.out.println("Buffer: "+buffer);

        */


    }






    public String toString(){

        String msg ="ChunkedOutputStream: \n";

        msg += "    Minimum Chunk Size:           " + minimumChunkSize +"\n";
        msg += "    Cache Capacity:               " + cache.length+"\n";
        msg += "\n";
        msg += "    Current Cache Size:           " + cacheSize+"\n";
        msg += "    Current Chunk Type:           '" + (char)currentChunkType+"'\n";
        msg += "    isOpen:                       " + isOpen+"\n";

        return msg;

    }




}
