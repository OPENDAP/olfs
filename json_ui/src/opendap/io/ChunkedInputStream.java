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

import opendap.bes.DevNull;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * User: ndp
 * Date: Dec 19, 2007
 * Time: 9:14:01 AM
 */
public class ChunkedInputStream  {

    private Logger log;

    protected InputStream is;
    protected boolean isClosed;


    private byte[] currentChunkHeader;

    private int currentChunkDataSize;
    private int chunkReadPosition;

    private int currentChunkType;

    private ChunkProtocol chunkProtocol;


    //private byte[] transferBuffer;




/**
     * Wraps an input stream and interprets it as a chunked stream.
     * @param stream to wrap
     * @param chunkProtocol the chunking protocol
     */
    public ChunkedInputStream(InputStream stream, ChunkProtocol chunkProtocol){

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());


        this.chunkProtocol = chunkProtocol;
        is = stream;
        currentChunkHeader = new byte[Chunk.HEADER_SIZE];
        //transferBuffer     = new byte[Chunk.MAX_SIZE];
        currentChunkType   = Chunk.DATA;
        isClosed           = false;
    }


    /**
         * Wraps an input stream and interprets it as a chunked stream.
         * @param stream to wrap
         */
        public ChunkedInputStream(InputStream stream){
            super();

            log = org.slf4j.LoggerFactory.getLogger(getClass());

            is = stream;
            currentChunkHeader = new byte[Chunk.HEADER_SIZE];
            //transferBuffer     = new byte[Chunk.MAX_SIZE];
            currentChunkType   = Chunk.DATA;
            isClosed           = false;
        }



    /**
     * Reads the next chunk header.
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
        ret = Chunk.readChunkHeader(is,currentChunkHeader,0);

        if(ret==-1)
            return ret;

        // Cache the chunk size the header
        currentChunkDataSize = Chunk.getDataSize(currentChunkHeader);

        // Cache the Chunk Type.
        currentChunkType = Chunk.getType(currentChunkHeader);


        log.debug("Chunk Data Size: " + currentChunkDataSize +
                           "    Chunk Type: "+(char)currentChunkType);

        chunkReadPosition = 0;

        return currentChunkDataSize;


    }


    public int getCurrentChunkType() {
        return currentChunkType;
    }





    public int availableInChunk(){
        return currentChunkDataSize - chunkReadPosition;
    }


    public int available() throws IOException {


        if(availableInChunk()<=0){

            int ret = readChunkHeader();

            if(ret == -1 || isLastChunk()){
                return 0;
            }
        }

        return availableInChunk();
    }


    public void close() throws IOException {
        isClosed = true;
        currentChunkDataSize = 0;
        chunkReadPosition = 0;
        is.close();

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
     * @param dStream The stream into which to transfer the message data.
     * @param errStream The stream into which to transfer error content if the
     * message contains it.
     * @return False is the message contained an extension with staus equal to
     * error (Which is another way of saying that the source passed an error
     * message in the stream)
     * @throws IOException When there are problems reading from or interpreting
     * the message stream.
     */
    public boolean readChunkedMessage(OutputStream dStream,
                                      OutputStream errStream)
            throws IOException {

        int ret;
        int bytesReceived;
        boolean isError = false;
        boolean moreData = true;
        byte[] buffer=null;

        while(moreData && !isClosed){

            if(availableInChunk()<=0){

                ret = readChunkHeader();

                if(ret == -1 || isLastChunk()){
                    moreData = false;
                }
                else {
                    buffer = new byte[currentChunkDataSize];
                }

            }
            else {
                switch (getCurrentChunkType()){

                    case Chunk.DATA:

                        if(buffer==null)
                            throw new IOException("Illegal state in readChunkedMessage. The receive buffer is null.");


                        // read the chunk body
                        bytesReceived = Chunk.readFully(is,buffer,0, availableInChunk());

                        // write the data out to the appropriate stream,
                        // depending on the error status.
                        (isError?errStream:dStream).write(buffer,0,bytesReceived);
                        (isError?errStream:dStream).flush();

                        // update the read pointer.show veriosn
                        chunkReadPosition += bytesReceived;
                        break;

                    case Chunk.EXTENSION:

                        isError = processExtensionContent() || isError;

                        break;

                    default:
                        throw new IOException("Unknown Chunk Type.");

                }

            }

        }

        return !isError;
    }








    /**
     *
     *
     * @return Ture if the extension contains the "status=error;"
     * extension name value pair, false otherwise.
     *
     * @throws IOException When there are problems reading from or interpreting
     * the message stream.
     */
    private boolean processExtensionContent() throws IOException {

        boolean isError = false;


        // Get the extension content from the stream
        byte[] ext = new byte[currentChunkDataSize];
        int bytesReceived = Chunk.readFully(is,ext,0, availableInChunk());
        chunkReadPosition += bytesReceived;




        // Make a string out of it
        String e =  new String(ext,0,bytesReceived);

        String[] extensions = e.split(";");

        // Evaluate the extension information


        for(String extension : extensions){

            // Is it a "status" extension?
            if(extension.startsWith(Chunk.STATUS_EXTENSION)){

                String status = extension.substring(extension.indexOf('=')+1,extension.length());
                //log.debug("status: "+status);

                // Is the status an error?
                if(status.equalsIgnoreCase(Chunk.ERROR_STATUS)){
                    //log.error("status: error");
                    isError =  true;
                    if(status.equalsIgnoreCase(Chunk.EMERGENCY_EXIT_STATUS)){
                        log.error("Stream source requested an emergency exit! Closing connection immediately.");
                        isClosed = true;
                        is.close();
                    }

                }
                // Is the status a mandatory exit?
                else if(status.equalsIgnoreCase(Chunk.EXIT_STATUS)){
                    int ret = readChunkHeader();
                    if(ret == -1 || isLastChunk()){
                        isClosed = true;
                    }
                    log.debug("Stream closed by Source.");

                }
                else {
                    log.debug("Received status extension: "+extension);
                }
            }
            else {
                log.debug("Received extension: "+extension);
            }
        }
        return isError;

    }


    /*

    public boolean markSupported() {
        return false;
    }

    public void mark(int readLimit) {
    }

    public void reset() throws IOException {
        throw new IOException("This stream has not been marked, and dude, marking is not supported.");
    }


    public long skip(long n)
            throws IOException {

        throw new IOException("Skip not currently suported on ChunkedInputStream.");
    }

    public int read()
            throws IOException {

        int val;
        if(available()>0){
            val = is.read();
            if(val>=0)
                chunkReadPosition += val;
            return val;
        }
        else {
            return -1;
        }
    }



    public int read(byte[] b)
            throws IOException {


        return read(b,0,b.length);
    }



    public int read(byte[] buffer, int offset, int length) throws IOException {

        if (buffer == null)
            throw new IOException("Illegal state in readChunkedMessage. " +
                    "The read buffer is null.");

        int ret;
        int bytesReceived = 0;
        boolean moreData = true;

        while (moreData && !isClosed) {

            if (availableInChunk() <= 0) {
                ret = readChunkHeader();
                if (ret == -1 || isLastChunk()) {
                    moreData = false;
                }
            } else {

                if (availableInChunk() >= length) {
                    ret = Chunk.readFully(is, buffer, offset, length);
                    if (ret < 0 && bytesReceived == 0) {
                        bytesReceived = ret;
                    } else {
                        bytesReceived += ret;
                        chunkReadPosition += bytesReceived;

                    }
                    moreData = false;

                } else {
                    ret = Chunk.readFully(is, buffer, offset, availableInChunk());
                    if (ret < 0) {
                        moreData = false;
                        if (bytesReceived == 0)
                            bytesReceived = ret;
                    } else {
                        bytesReceived += ret;
                        chunkReadPosition += bytesReceived;
                        offset += bytesReceived;
                        length -= bytesReceived;
                    }

                }

            }

        }

        return bytesReceived;


    }


    */



}
