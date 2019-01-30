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
package opendap.io;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: ndp
 * Date: Dec 19, 2007
 * Time: 9:14:01 AM
 */
public class ChunkedInputStream  {

    private Logger log;

    private static int defaultBufferSize = 10240;
    private static int MaxBufferSize = 16777216;

    protected InputStream is;
    protected boolean isClosed;


    private byte[] currentChunkHeader;

    private byte[] chunkBuffer;
    private int largestChunkDataSize;

    private int currentChunkDataSize;
    private int chunkReadPosition;

    private int currentChunkType;

    //private ChunkProtocol chunkProtocol;


    
    /**
         * Wraps an input stream and interprets it as a chunked stream.
         * @param stream to wrap
         */
        public ChunkedInputStream(InputStream stream){
            super();

            log = org.slf4j.LoggerFactory.getLogger(getClass());

            is = stream;
            currentChunkHeader = new byte[Chunk.HEADER_SIZE];
            currentChunkType   = Chunk.DATA;
            isClosed           = false;
            largestChunkDataSize = 0;
            chunkBuffer = new byte[defaultBufferSize];
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

        //log.info(NewPPTClient.showConnectionProperties(_mySock));

        // Read the header
        currentChunkDataSize = Chunk.readChunkHeader(is,currentChunkHeader,0);

        if(currentChunkDataSize==-1)
            return currentChunkDataSize;

        // Cache the chunk size the header
        //currentChunkDataSize = Chunk.getDataSize(currentChunkHeader);

        // Cache the Chunk Type.
        currentChunkType = Chunk.getType(currentChunkHeader);


        log.debug("Chunk Data Size: " + currentChunkDataSize +
                           "    Chunk Type: "+ (char)currentChunkType);

         chunkReadPosition = 0;

        if(largestChunkDataSize< currentChunkDataSize)
            largestChunkDataSize = currentChunkDataSize;


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
     * Reads a chunked message from the underlying InputStream and transmits to the passed OutputStream,
     * <code>dstream</code>. If an error condition is encountered in the chunked message then the error content
     * will be written to the OutputStream <code>errStream</code>.
     *
     * @param dStream The stream into which to transfer the message data.
     * @param errStream The stream into which to transfer error content if the
     * message contains it.
     * @return False if the chunked message contained an extension with status equal to
     * error (Which is another way of saying that the source passed an error
     * message in the stream). True otherwise.
     * @throws IOException When there are problems reading from or interpreting
     * the chunked message stream.
     */
    public boolean readChunkedMessage(OutputStream dStream, OutputStream errStream) throws IOException {

        int ret;
        int bytesReceived;
        boolean isError = false;
        boolean moreData = true;
        String extensionContent;
        long bytesRead = 0;

        while(moreData && !isClosed){

            if(availableInChunk()<=0){

                ret = readChunkHeader();

                if(ret == -1 || isLastChunk()){
                    moreData = false;
                }
                else {

                    // Check to see if the chunk size is bigger than the buffer
                    if(chunkBuffer.length < currentChunkDataSize){

                        // If the new chunk size is too big, bail.
                        if(currentChunkDataSize > MaxBufferSize){
                            String msg = "Found a chunk size larger than I support. My max size " +
                                         MaxBufferSize + " bytes, currentChunkDataSize: "+currentChunkDataSize;
                            log.error(msg);
                            throw new IOException(msg);
                        }
                        else {

                            // Since it's safe to do so, upgrade the chunk buffer.
                            log.debug("Increasing chunkBuffer size to: {} bytes.",currentChunkDataSize);
                            chunkBuffer = new byte[currentChunkDataSize];
                        }

                    }

                }

            }
            else {

                // read the chunk body
                bytesReceived = Chunk.readFully(is,chunkBuffer,0, availableInChunk());

                log.debug("CurrentChunksize: "+ currentChunkDataSize+ " bytesReceived: "+ bytesReceived);
                
                // update the read pointer.
                chunkReadPosition += bytesReceived;

                // And keep the books...
                bytesRead += bytesReceived;

                switch (getCurrentChunkType()){

                    case Chunk.DATA:
                        // write the data out to the appropriate stream,
                        // depending on the error status.
                        (isError?errStream:dStream).write(chunkBuffer,0,bytesReceived);
                        (isError?errStream:dStream).flush();
                        break;

                    case Chunk.EXTENSION:

                        // Build a String from the content of the chunk extension.
                        extensionContent =  new String(chunkBuffer,0,bytesReceived,HyraxStringEncoding.getCharset());

                        // Process the extension content & preserve any previously encountered errors found in the message.
                        isError = processExtensionContent(extensionContent) || isError;
                        break;

                    default:
                        throw new IOException("Unknown Chunk Type.");

                }

            }

        }

        log.info("readChunkedMessage() - Message contained {} bytes.",bytesRead);
        return !isError;
    }








    /**
     *
     *
     * @param e  The content of the chunk extension held in a String.
     * @return True if the extension contains the "status=error;"
     * extension name value pair, false otherwise.
     *
     * @throws IOException When there are problems reading from or interpreting
     * the message stream.
     */
    private boolean processExtensionContent(String e) throws IOException {

        boolean isError = false;

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
                }
                // Is the status an emergency exit?
                else if(status.equalsIgnoreCase(Chunk.EMERGENCY_EXIT_STATUS)){
                    log.error("Stream source requested an emergency exit! Closing connection immediately.");
                    isClosed = true;
                    is.close();
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



    public int getChunkedReadBufferSize(){
        return chunkBuffer.length;
    }


}
