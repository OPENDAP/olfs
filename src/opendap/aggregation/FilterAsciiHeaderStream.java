/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
 * // Author: James Gallagher <jgallagher@opendap.org>
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

package opendap.aggregation;

import com.sun.istack.internal.NotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Filter the BES/DAP ASCII response for a Sequence so that the initial
 * <filename> and column names are optionally filtered out. This stream
 * will be used to concatenate N ASCII Sequence responses into one big
 * table of CSV data.
 *
 * In DAP, a Sequence is transformed into ASCII by flattening it and
 * making each of the resulting variables a column. Two header lines are
 * written out, followed by a sequence of rows where each row is identical
 * in format. Here's a short example:
 *
 * function_result_MOD04_L2.A2015021.0020.051.NRT.hdf
 * table.Latitude, table.Longitude, table.Solar_Zenith
 * 57.5152, 146.06, 8191
 * 57.5197, 146.821, 8171
 * 57.5198, 147.53, 8153
 * 57.5162, 148.192, 8136
 *
 * If we call the BES several times, we can return N tables or, if the
 * tables all share the same columns, we can group them together and
 * return one table given that the headers for the second, ..., Nth
 * table are removed. That's what this class does.
 *
 * Created by jimg on 3/5/15.
 */
class FilterAsciiHeaderStream extends FilterOutputStream {

    // Track if the header has been found.
    private boolean _found_header;
    private boolean _found_first_newline;

    // Write stuff to this stream.
    private final OutputStream _out;

    public FilterAsciiHeaderStream(OutputStream out) {
        super(out);

        _out = out;

        _found_header = false;
        _found_first_newline = false;
    }

    /**
     * Should the stream filter out the next instance of the two header lines?
     *
     * @param filter True: filter the next instance of the header lines;
     *               False: let them pass through to the output.
     */
    public void set(boolean filter) {
        _found_first_newline = !filter;
        _found_header = !filter;
    }

    /**
     * Write out the byte, making sure first to remove the first two
     * lines of characters (aka the header). Note that this is the method
     * that actually filters the output, the other methods simply call
     * this until the header is found, and then they revert to their normal
     * behavior.
     *
     * @param b Write out this byte, or remove it from the output if the
     *          two line header has not yet been seen.
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        if (_found_header) {
            _out.write(b);
        }
        else {
            // Each header consists of two lines (terminated by a newline
            // character). If b is a newline, then if the first newline
            // has already been found, the second has just been found and
            // that is the complete header. Otherwise, note that the first
            // line has been found.
            if (b == '\n') {
                if (_found_first_newline)
                    _found_header = true;
                else
                    _found_first_newline = true;
            }
         }
    }

    /**
     * Write out the byte array, first stripping a two line header
     * if it has not already been found and removed.
     * @param b The byte array to send
     * @throws IOException
     */
    @Override
    public void write(@NotNull byte[] b) throws IOException {
        if (_found_header) {
            _out.write(b, 0, b.length);
        }
        else {
            int i = 0;
            while (!_found_header && i < b.length) {
                write(b[i++]);
            }
            _out.write(b, i, b.length - i);
        }
    }

    /**
     * Write out the byte array, first stripping a two line header
     * if it has not already been found and removed.
     * @param b The byte array to send
     * @param off send bytes starting at this offset
     * @param len send this many bytes
     * @throws IOException
     */
    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
        if (_found_header) {
            _out.write(b, off, len);
        }
        else {
            int end = off+len;
            while (!_found_header && off < end) {
                write(b[off++]);
            }
            _out.write(b, off, end - off);
        }
    }
}

