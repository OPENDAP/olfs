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
package opendap.semantics.IRISail;


import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Comma Separated Values (CSV) Splitter.
 * quotes are preserved
 */

public class CSVSplitter {

    private Logger log;


    /**
     * The field-separator character. The default is ','.
     */
    public final char fieldSeperator;

    /**
     * The quotation character. The default is '"'. Eg: 12345,"Builder, Bob",BUILDER,07 389 3896.
     */
    public final char quoteCharacter;

    /**
     * if true then "cat,,,dog" is equivalent to "cat,dog". The default is false.
     */
    public final boolean treatConsecutiveSeperatorsAsOne;

    /**
     * if true then " cat, dog  " is equivalent to "cat,dog". The default is true.
     */
    public final boolean trimFields;

    /**
     * Initialise the CSVSplitter with default values
     * field-seperator=','
     * quoteCharacter='"'
     * treatConsecutiveSeperatorsAsOne=false
     * trimFields=true
     */
    public CSVSplitter() {
        this(',', '"', false, true);
    }

    /**
     * Initialise the CSVSplitter.
     *
     * @param fieldSeperator char - the field-seperator character. The default is ','.
     * @param quoteCharacter char - The quotation character. The default is '"'.
     * @param treatConsecutiveSeperatorsAsOne
     *                       boolean - if true then "cat,,,dog" is equivalent to "cat,dog".
     * @param trimFields     boolean - if true then " cat, dog  " is equivalent to "cat,dog".
     */
    public CSVSplitter(char fieldSeperator, char quoteCharacter, boolean treatConsecutiveSeperatorsAsOne, boolean trimFields) {

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        this.fieldSeperator = fieldSeperator;
        this.treatConsecutiveSeperatorsAsOne = treatConsecutiveSeperatorsAsOne;
        this.quoteCharacter = quoteCharacter;
        this.trimFields = trimFields;
    }

    public String[] split(String line) throws InterruptedException {
        List<String> tokens = new ArrayList<String>();
        char[] characters = line.toCharArray(); // an array allows for lookaround
        int n = characters.length; // (my one micro-optimization;-)
        boolean quoted = false;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < n; i++) { // for each character in the line.
            char character = characters[i]; // the current character
            char next = i + 1 == n ? '\0' : characters[i + 1]; // the next character
            boolean discard = false; // if discard then throw this character away
            if (character == quoteCharacter) {
                if (!quoted) {
                    quoted = true;
                    discard = false;
                } else {
                    if (next == quoteCharacter) {
                        token.append(quoteCharacter);
                        i++; // and skip the next character
                        continue; // leaving quoted=true
                    } else {
                        quoted = false;
                        discard = false;
                    }
                }
            } else if (character == fieldSeperator) {
                if (!quoted) {
                    if (treatConsecutiveSeperatorsAsOne && token.length() == 0) {
                        discard = true; // chuck subsequent fieldSeperator(s) ,,,
                    } else {
                        // encountered an active field separator
                        tokens.add(asString(token));
                        token.setLength(0); // clear the token
                        discard = true;
                    }
                }
            }
            if (!discard) {
                token.append(character);
            }
        }
        // append the final token to the result dealing with the special case of
        // an empty input line, which should return an empty list. The caller
        // should filter out empty lines if that is the required behaviour.
        String strtok = asString(token);
        if (!(tokens.isEmpty() && strtok.length() == 0)) {
            tokens.add(strtok);
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    private String asString(StringBuilder token) throws InterruptedException {
        String strtok = token.toString();
        return (trimFields ? strtok.trim() : strtok);
    }

    public static void main(String[] args) {
            usageExample();
    }

    private static void usageExample() {
        // "line" would normally be read from an input file.
        //String line = "  10101179664,,\"SMITH, JOHN D.\",  \"\"\"SINGLE\"\"\",ACTIVE,\"JOHN \"\"THE DODGER\"\" SMITH\",19/06/1997,\"\"\"\"\"\",\"\"\"    \"\"\"";
        String line = "test,\",\",test";
        // something to verify the result against.
        String[] expected = new String[]{"10101179664", "", "SMITH, JOHN D.", "\"SINGLE\"", "ACTIVE", "JOHN \"THE DODGER\" SMITH", "19/06/1997", "\"\"", "\"    \""};
        // Get a splitter with default configuration. See the javadoc.
        CSVSplitter splitter = new CSVSplitter();
        // Split the given line into fields
        try {
            String[] fields = splitter.split(line);
            for (int i = 0; i < fields.length; i++) {
                System.out.println("element " + i + " = " + fields[i]);
            }
        } catch (InterruptedException e) {
            splitter.log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}
