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

package opendap.experiments;

import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 9, 2010
 * Time: 4:46:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class JwsTest {


    private Logger log;


    JwsTest() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    public static void main(String[] args) {
        JwsTest jt = new JwsTest();
        jt.run(args);
    }

    private void run(String[] args) {


        Vector<String> msg = new Vector<String>();

        int i = 0;
        for (String s : args) {
            msg.add("Commandline argument "+ i++ + ": '"+s + "'\n");
        }

        localFileAccessTest(msg);

        String[] messages = new String[msg.size()];
        messages = msg.toArray(messages);
        popMeUp(messages);

    }

    private void localFileAccessTest(Vector<String> msg){
        String filename = getClass().getSimpleName();
        File f = new File(filename);
        msg.add("#################################################################################################\n");
        msg.add("Local File Access Testing: START\n");

        String currentDir = System.getProperty("user.dir");
        msg.add("Current Local Directory: '" + currentDir + "'\n");

        msg.add("Checking file: '" + f.getAbsolutePath() + "'\n");
        if (!f.exists()) {
            msg.add("No Such File.\n");
            try {
                msg.add("Created File:" + f.createNewFile() + "\n");
            } catch (IOException e) {
                msg.add("Created File: FAILED (" + e.getMessage() + ")\n");
            }

        } else {
            msg.add("The file '" + f.getAbsolutePath() + "' exists.\n");
        }


        msg.add("canRead(): " + f.canRead() + "\n");
        msg.add("canWrite(): " + f.canWrite() + "\n");
        msg.add("canExecute(): " + f.canExecute() + "\n");
        msg.add("isDirectory(): " + f.isDirectory() + "\n");
        msg.add("isFile(): " + f.isFile() + "\n");

        try {
            FileOutputStream os = new FileOutputStream(f);

            os.write("The quick brown fox jumped over the lazy dog.".getBytes());
            os.close();
            msg.add("File write test succeeded.\n");


        } catch (IOException e) {
            msg.add("FAILED to write to file.  (" + e.getMessage() + ")\n");
        }


        try {
            FileInputStream fis = new FileInputStream(f);

            byte[] buf = new byte[(int) f.length()];
            fis.read(buf);
            fis.close();

            msg.add("File read test succeeded. \nThe file containes this: '" + new String(buf) + "' \n");

        } catch (IOException e) {
            msg.add("FAILED To read file.  (" + e.getMessage() + ")\n");
        }
        msg.add("Local File Access Testing: COMPLETE\n");
        msg.add("#################################################################################################\n");
    }

    private JTextPane createTextPane(String[] initString) {

        JTextPane textPane = new JTextPane();
        StyledDocument doc = textPane.getStyledDocument();
        addStylesToDocument(doc);

        try {
            doc.insertString(doc.getLength(), "<logo>\n", doc.getStyle("logo"));
            doc.insertString(doc.getLength(), "Java Web Start Test\n\n", doc.getStyle("largebold"));
            for (int i = 0; i < initString.length; i++) {
                doc.insertString(doc.getLength(), initString[i],
                        doc.getStyle("regular"));
            }
        } catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }

        return textPane;
    }

    protected void addStylesToDocument(StyledDocument doc) {
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");

        Style s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("small", regular);
        StyleConstants.setFontSize(s, 10);

        s = doc.addStyle("large", regular);
        StyleConstants.setFontSize(s, 16);

        s = doc.addStyle("largebold", regular);
        StyleConstants.setFontSize(s, 16);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("logo", regular);
        StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER);
        ImageIcon logo = createImageIcon("images/logo.png",
                "OPeNDAP Logo");
        if (logo != null) {
            StyleConstants.setIcon(s, logo);
        }


    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected ImageIcon createImageIcon(String path,
                                        String description) {

        ClassLoader cl = this.getClass().getClassLoader();

        java.net.URL imgURL = cl.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }



    private void popMeUp(String[] msg) {
        JFrame frame = new JFrame("JwsTest");

        JTextPane tPane = createTextPane(msg);

        frame.getContentPane().add(tPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }




}
