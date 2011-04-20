/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-OPeNDAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.experiments;

import java.io.*;

import org.apache.commons.cli.*;
import opendap.ppt.OPeNDAPClient;
import opendap.ppt.PPTException;

public class TestClient {
    public static void main(String[] args) {
        String hostStr = null;
        String portStr;
        int portVal = 0;

        String cmd = null;
        String inputStr = null;
        File inputFile = null;
        String outputStr = null;
        File outputFile;
        String timeoutStr;

        OutputStream out = System.out;
        OutputStream err = System.err;

        // jhrg int timeoutVal = 0;
        // jhrg boolean debug = false;

        PosixParser pp = new PosixParser();

        Option opt_h = new Option("h", "host", true, "");
        Option opt_p = new Option("p", "port", true, "");
        Option opt_x = new Option("x", "command", true, "");
        Option opt_i = new Option("i", "inputFile", true, "");
        Option opt_f = new Option("f", "outputFile", true, "");
        Option opt_t = new Option("t", "timeout", true, "");
        // jhrg Option opt_d = new Option("d", "debug", false, "");

        Options opts = new Options();
        opts.addOption(opt_h);
        opts.addOption(opt_p);
        opts.addOption(opt_x);
        opts.addOption(opt_i);
        opts.addOption(opt_f);
        opts.addOption(opt_t);

        boolean badUsage = false;
        // jhrg String msg = "";

        try {
            CommandLine cl = pp.parse(opts, args);
            hostStr = cl.getOptionValue("h");
            portStr = cl.getOptionValue("p");
            cmd = cl.getOptionValue("x");
            inputStr = cl.getOptionValue("i");
            outputStr = cl.getOptionValue("f");
            timeoutStr = cl.getOptionValue("t");
            // jhrg debug = cl.hasOption("d");

            if (hostStr == null) {
                System.out.println("host must be specified");
                badUsage = true;
            }

            if (portStr == null) {
                System.out.println("port must be specified");
                badUsage = true;
            } else {
                portVal = Integer.valueOf(portStr);
            }

            if (timeoutStr != null) {
                // jhrg timeoutVal = Integer.valueOf(timeoutStr).intValue();
            }
        }
        catch (ParseException pe) {
            System.out.println("error parsing the command line");
            System.out.println(pe.getMessage());
            badUsage = true;
        }
        catch (NumberFormatException nfe) {
            System.out.println("error converting port number");
            System.out.println(nfe.getMessage());
            badUsage = true;
        }

        if (inputStr != null) {
            inputFile = new File(inputStr);
            if (!inputFile.exists()) {
                System.out.println("input file does not exist");
                badUsage = true;
            }
        }

        if (outputStr != null) {
            outputFile = new File(outputStr);
            if (outputFile.exists()) {
                if (!outputFile.canWrite()) {
                    System.out.println("output file " + outputStr + " already exists, can not write to it");
                    badUsage = true;
                }
            } else {
                try {
                    if (!outputFile.createNewFile()) {
                        System.out.println("unable to create output file " + outputStr);
                        badUsage = true;
                    }
                    out = new FileOutputStream(outputFile);

                }
                catch (IOException e) {
                    System.out.println("unable to create output file " + outputStr);
                    System.out.println(e.getMessage());
                    badUsage = true;
                }
            }
        }

        if (badUsage) {
            showUsage();
            System.exit(1);
        }




        OPeNDAPClient client = null;
        try {
            client = new OPeNDAPClient();
            client.startClient(hostStr, portVal);
        }
        catch (PPTException e) {
            System.err.println("error starting the client");
            System.err.println(e);
            System.exit(1);
        }

        try {
            if (cmd != null) {
                client.executeCommands(cmd,out,err);
            } else if (inputFile != null) {
                client.executeCommands(inputFile,out,err);
            } else {
                client.interact(out,err);
            }
        }
        catch (PPTException e) {
            System.err.println("error processing commands");
            System.err.println(e);
            System.exit(1);
        }

        try {
            if (client != null) {
                client.shutdownClient();
            }
        }
        catch (PPTException e) {
            System.err.println("error closing the client");
            System.err.println(e);
            System.exit(1);
        }
    }

    private static void showUsage() {
        System.out.println("");
        System.out.println("the following flags are availableInChunk:");
        System.out.println("    -h <host> - specifies a host for TCP/IP connection");
        System.out.println("    -p <port> - specifies a port for TCP/IP connection");
        System.out.println("    -x <command> - specifies a command for the server to execute");
        System.out.println("    -i <inputFile> - specifies a file name for a sequence of input commands");
        System.out.println("    -f <outputFile> - specifies a file name to output the results of the input");
        System.out.println("    -t <timeoutVal> - specifies an optional timeout value in seconds");
        System.out.println("    -d - sets the optional debug flag for the client session");
    }
}

