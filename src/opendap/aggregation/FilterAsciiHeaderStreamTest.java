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

import org.apache.commons.io.FileUtils;

// This uses JUnit 4; the first set of tests I made used JUnit 3. Not sure it matters
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilterAsciiHeaderStreamTest {

    private FileInputStream _in;
    private FilterAsciiHeaderStream _out;

    /**
     * Use the single character version of write() to output one char at
     * a time.
     *
     * @param closeOutput if true, close the output stream;
     */
    private void grind(boolean closeOutput) {
        try {
            int c;

            while ((c = _in.read()) != -1)
                _out.write(c);

            _in.close();
            if (closeOutput)
                _out.close();
        }
        catch (Exception e) {
            Assert.fail("Caught exception: " + e.getMessage());
        }
    }

    @SuppressWarnings("EmptyMethod")
    @Before
    public void setUp() throws Exception {

    }

    @SuppressWarnings("EmptyMethod")
    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testWrite() throws Exception {
        _in = new FileInputStream("resources/aggregation/unit-tests/source_1.txt");
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        _out.set(true);
        grind(true);

        // validate dest.txt here
        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_2.txt")));
    }

    @Test
    public void testWrite_1() throws Exception {
        _in = new FileInputStream("resources/aggregation/unit-tests/source_1.txt");
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        _out.set(false);
        grind(false);

        _out.set(true);
        _in = new FileInputStream("resources/aggregation/unit-tests/source_2.txt");
        grind(true);

        // validate dest_2.txt here
        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_3.txt")));
    }

    @Test
    public void testWrite_2() throws Exception {
        _in = new FileInputStream("resources/aggregation/unit-tests/source_1.txt");
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        _out.set(false);
        grind(false);

        _out.set(true);
        _in = new FileInputStream("resources/aggregation/unit-tests/source_2.txt");
        grind(false);

        _out.set(true);
        _in = new FileInputStream("resources/aggregation/unit-tests/source_3.txt");
        grind(true);

        // validate dest_2.txt here
        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_4.txt")));
    }

    @Test
    public void testWriteArray() throws Exception {
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        Path path = Paths.get("resources/aggregation/unit-tests/source_1.txt");
        byte[] data = Files.readAllBytes(path);

        _out.set(false);
        _out.write(data);

        _out.close();

        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_1.txt")));
    }

    @Test
    public void testWriteArray_1() throws Exception {
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        Path path = Paths.get("resources/aggregation/unit-tests/source_1.txt");
        byte[] data = Files.readAllBytes(path);

        _out.set(false);
        _out.write(data);

        data = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_2.txt"));
        _out.set(true);
        _out.write(data);

        _out.close();

        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_3.txt")));
    }

    @Test
    public void testWriteArray_2() throws Exception {
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        Path path = Paths.get("resources/aggregation/unit-tests/source_1.txt");
        byte[] data = Files.readAllBytes(path);

        _out.set(false);
        _out.write(data);

        data = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_2.txt"));
        _out.set(true);
        _out.write(data);

        data = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_3.txt"));
        _out.set(true);
        _out.write(data);

        _out.close();

        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_4.txt")));
    }

    // This test uses the third form of write() where the offset and length
    // are explicitly passed in. We've probably been calling those in the
    // above tests, but I wanted to make it explicit.
    @Test
    public void testWrite_3() throws Exception {
        _out = new FilterAsciiHeaderStream(new FileOutputStream("src/opendap/aggregation/dest.txt"));

        byte[] data1 = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_1.txt"));
        byte[] data2 = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_2.txt"));
        byte[] data3 = Files.readAllBytes(Paths.get("resources/aggregation/unit-tests/source_3.txt"));

        byte[] data = new byte[data1.length + data2.length + data3.length];
        System.arraycopy(data1, 0, data, 0, data1.length);
        System.arraycopy(data2, 0, data, data1.length, data2.length);
        System.arraycopy(data3, 0, data, data1.length + data2.length, data3.length);

        _out.set(false);
        _out.write(data, 0, data1.length);

        _out.set(true);
        _out.write(data, data1.length, data2.length);

        _out.set(true);
        _out.write(data, data1.length + data2.length, data3.length);

        _out.close();

        Assert.assertTrue(FileUtils.contentEquals(new File("src/opendap/aggregation/dest.txt"),
                new File("resources/aggregation/unit-tests/baseline_4.txt")));
    }
}