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

import opendap.ppt.PPTSessionProtocol;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: ndp
 * Date: Jan 3, 2008
 * Time: 11:42:08 AM
 */
public class Chunk {

    public static final int HEADER_SIZE_ENCODING_BYTES = 7;
    public static final int HEADER_TYPE_ENCODING_BYTES = 1;
    public static final int HEADER_SIZE = HEADER_SIZE_ENCODING_BYTES + HEADER_TYPE_ENCODING_BYTES;

    /**
     * The CHUNK_SIZE_BIT_MASK is calculated based on the number of bytes
     * specified by the HEADER_SIZE_ENCODING_BYTES.
     */
    private static int BIT_MASK;
    static  {
        BIT_MASK = 0;
        for(int i=0; i< HEADER_SIZE_ENCODING_BYTES;i++){
            BIT_MASK = (BIT_MASK<<4) + 0x000f;
            //System.out.println("BIT_MASK:  0x"+ Integer.toHexString(BIT_MASK));
        }
    }


    public static final int SIZE_BIT_MASK = BIT_MASK;

    public static final int MAX_SIZE = BIT_MASK;



    public static final int DATA = 'd';
    public static final int EXTENSION = 'x';


    public static final int DEFAULT_SIZE = 65535;


    public static final byte[] closingChunk = new byte[HEADER_SIZE];
    static  {
        int i;
        for(i=0; i<HEADER_SIZE_ENCODING_BYTES; i++){
            closingChunk[i] = '0';
        }
        closingChunk[i] = DATA;
    }


    public static final String STATUS_EXTENSION = "status=";
    public static final String ERROR_STATUS = "error";
    public static final String EMERGENCY_EXIT_STATUS = "exit";
    public static final String EXIT_STATUS = PPTSessionProtocol.PPT_EXIT_NOW;

    public static final String COUNT_EXTENSION = "count=";



    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Chunk.class);


    /**
     *
     * @param chunkHeader
     * @return The size, in bytes, of the data section of this chunk. If the
     * passed header is the closing chunk (Size is all zeros) this is taken to
     * indicate that the transmission is at an end and a -1 is returned.
     * @throws IOException
     */
    public static int getDataSize(byte[] chunkHeader) throws IOException{
        String sizestr = "";
        for(int i=0; i<HEADER_SIZE_ENCODING_BYTES; i++){
            sizestr += (char)chunkHeader[i];
        }


        //System.out.println("ChunkSizeString: "+sizestr);

        int chunkSize = Integer.valueOf(sizestr,16);

        //System.out.println("ChunkSize:       "+chunkSize);

        if(chunkSize==0){
            return -1;
        }

        return chunkSize;
    }





    public static boolean isLastChunk(byte[] chunkHeader){

        String sizestr = "";
        for(int i=0; i<HEADER_SIZE_ENCODING_BYTES; i++){
            sizestr += (char)chunkHeader[i];
        }


        //System.out.println("ChunkSizeString: "+sizestr);

        int chunkSize = Integer.valueOf(sizestr,16);

        //System.out.println("ChunkSize:       "+chunkSize);

        return chunkSize == 0;

    }





    public static int getType(byte[] chunkHeader) throws IOException {



        byte[] type = new byte[HEADER_TYPE_ENCODING_BYTES];
        int j=0;
        for(int i=HEADER_SIZE_ENCODING_BYTES; i<HEADER_SIZE; i++){
            type[j++] = chunkHeader[i];
        }

        if(type.length == 1){
            return type[0];
        }
        else {
            throw new IOException("Size of Chunk Type Encoding has changed." +
                    "The implmentation of  opendap.io.Chunk is not compatible" +
                    "with this change. Reimplment Chunk!");
        }


    }


    static int readFully(InputStream is, byte[] buf, int off, int len) throws IOException{


        if(     buf!=null &&         // Make sure the buffer is not null
                len>=0 &&            // Make sure they want a positive number of bytes
                off>=0 &&            // Make sure the offset is positive
                buf.length<=(off+len) // Guard against buffer overflow
            ){

            boolean done = false;
            int bytesToRead = len;
            int totalBytesRead =0;
            int bytesRead;

            while(!done){
                bytesRead = is.read(buf,off,len);
                if(bytesRead == -1){
                    if(totalBytesRead==0)
                        totalBytesRead=-1;
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
        else {
            String msg = "Attempted to read "+len+" bytes starting " +
                    "at "+off;
            if(buf==null)
                msg += " into a null reference. ";
            else
                msg += " into a buffer of length "+buf.length+"  ";
            msg += "I'm afraid I can't allow that...";
            throw new IOException(msg);
        }




    }

    /**
     *
     * Send closing chunk. This is a chunk header where the size is zero and
     * the chunk type is always "DATA". <p>
     * Example: {'0','0','0','0','d'}
     *
     * @param os The stream to which to write the closing chunk header.
     * @throws IOException When the wrapped OutputStream encounters a problem.
     */
    public static  void writeClosingChunkHeader(OutputStream os) throws IOException {

        if(os==null)
            throw new IOException("Chunk.writeClosingChunkHeader() - Passed " +
                    "OutputStream reference is null.");

        log.debug("writeClosingChunkHeader(): "+new String(closingChunk));
        os.write(closingChunk);

    }


    /**
     *
     * Writes a chunk header to the underlying stream. The passed <code>
     * dataSize<code> parameter specifies the size of the data about to be sent,
     * not including the size of the chunk header. The chunk header size is
     * added by this method prior writing the size to the stream.
     *
     * @param os The stream to which to write the chunk header.
     * @param dataSize The size of the data portion of the chunk.
     * @param type The type of the chunk
     * @throws IOException When things go wrong.
     */
    public static void writeChunkHeader(OutputStream os, int dataSize, int type) throws IOException {


        if(os==null)
            throw new IOException("Chunk.writeChunkHeader() - Passed " +
                    "OutputStream reference is null.");


        if( dataSize > Chunk.MAX_SIZE)
            throw new IOException("Chunk size of "+dataSize+ " bytes is to " +
                    "large to be encoded.");


        byte[] header =  new byte[Chunk.HEADER_SIZE];

        String size = Integer.toHexString(Chunk.SIZE_BIT_MASK & dataSize);

        while(size.length() < Chunk.HEADER_SIZE_ENCODING_BYTES){
            size = "0" + size;
        }

        log.debug("writeChunkHeader() - size: "+size+" size.length: "+size.length()+" type: "+(char)type);

        System.arraycopy(size.getBytes(),0,header,0,Chunk.HEADER_SIZE-1);

        header[Chunk.HEADER_SIZE-1] = (byte) type;

        os.write(header);

    }





    public String toString(){

        String msg ="Chunk: \n";

        msg += "    DEFAULT_SIZE:                 "+ DEFAULT_SIZE +"\n";
        msg += "    HEADER_SIZE_ENCODING_BYTES:   "+ HEADER_SIZE_ENCODING_BYTES +"\n";
        msg += "    HEADER_TYPE_ENCODING_BYTES:   "+ HEADER_TYPE_ENCODING_BYTES +"\n";
        msg += "    CHUNK_HEADER_SIZE:            "+ HEADER_SIZE +"\n";
        msg += "    CHUNK_SIZE_BIT_MASK:          0x"+ Integer.toHexString(SIZE_BIT_MASK) +"\n";
        msg += "    MAX_CHUNK_SIZE:               "+ MAX_SIZE +"\n";

        return msg;

    }




    /**
     * Reads a chunk header into the passed byte buffer begining at location
     * off. The number of bytes is determined by the value of the <code>
     * HEADER_SIZE<code> constant
     *
     * @param is The InputStream from which to read the header.
     * @param header The array into which to read the chunk header.
     * @param off The offset within the array header at which to start placing
     * the bytes of header.
     * @return The number of bytes in the chunk, or -1 if the underlying stream
     * has no more bytes to read.
     * @throws java.io.IOException When the underlying stream does.
     * @see #HEADER_SIZE
     *
     */
    public static int readChunkHeader(InputStream is,
                                      byte[] header,
                                      int off)
            throws IOException {

        if(header.length-off<HEADER_SIZE)
            throw new IOException("Header will exceed bounds of passed array.");

        int ret;


        // Read the header
        ret = Chunk.readFully(is, header,off, HEADER_SIZE);

        if(ret==-1)
            return ret;

        return getDataSize(header);


    }








}
