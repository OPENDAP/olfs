/*
 * Copyright (C) 2014, Alexey Kuznetsov <axet@me.com>
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package opendap.http;

import opendap.io.HyraxStringEncoding;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * NetRC file parser.
 *
 * @since 3.5
 */
public class NetRC {
    private static final Pattern NETRC = Pattern.compile("(\\S+)"); //$NON-NLS-1$

    /**
     * 'default' netrc entry. This is the same as machine name except that
     * default matches any name. There can be only one default token, and it
     * must be after all machine tokens.
     */
    private static final String DEFAULT_ENTRY = "default"; //$NON-NLS-1$

    /**
     * .netrc file entry
     */
    public static class NetRCEntry {
        /**
         * login netrc entry
         */
        private  String login;
        public String getLogin(){ return login; }

        /**
         * password netrc entry
         */
        private char[] password;
        public String getPassword() { return String.valueOf(password); }

        /**
         * machine netrc entry
         */
        private String machine;
        public String getMachine(){ return machine; }

        /**
         * account netrc entry
         */
        private String account;
        public String getAccount(){ return account; }

        /**
         * macdef netrc entry. Defines a macro. This token functions like the
         * ftp macdef command functions. A macro is defined with the specified
         * name; its contents begins with the next .netrc line and continues
         * until a null line (consecutive new-line characters) is encountered.
         * If a macro named init is defined, it is automatically executed as the
         * last step in the auto-login process.
         */
        private String macdef;
        public String getMacdef() { return macdef; }

        /**
         * macro script body of macdef entry.
         */
        private String macbody;
        public String getMacbody() { return macbody; }

        /**
         * Default constructor
         */
        public NetRCEntry() {
        }

        boolean complete() {
            return login != null && password != null && machine != null;
        }
    }

    private File netrc;

    private long lastModified;

    private Map<String, NetRCEntry> hosts = new HashMap<>();

    private static final TreeMap<String, State> STATE = new TreeMap<String, NetRC.State>() {
        private static final long serialVersionUID = -4285910831814853334L;
        {
            put("machine", State.MACHINE); //$NON-NLS-1$
            put("login", State.LOGIN); //$NON-NLS-1$
            put("password", State.PASSWORD); //$NON-NLS-1$
            put(DEFAULT_ENTRY, State.DEFAULT);
            put("account", State.ACCOUNT); //$NON-NLS-1$
            put("macdef", State.MACDEF); //$NON-NLS-1$
        }
    };

    enum State {
        COMMAND, MACHINE, LOGIN, PASSWORD, DEFAULT, ACCOUNT, MACDEF
    }

    /** */
    public NetRC(String filename) throws IOException {

        File f = new File(filename);
        String msg;
        if(!f.exists()){
            msg = "Cannot find file: "+ f.getAbsoluteFile();
            throw new IOException(msg);
        }

        if(!f.canRead()){
            msg = "Cannot read file: "+ f.getAbsoluteFile();
            throw new IOException(msg);
        }
        if(!f.isFile()){
            msg = "The file " + f.getAbsoluteFile() +" is not actually a file.";
            throw new IOException(msg);
        }

        netrc = f;
        if (netrc != null)
            parse();
    }

    /**
     * @param netrc
     *            the .netrc file
     */
    public NetRC(File netrc) throws IOException {
        this.netrc = netrc;
        parse();
    }


    /**
     * Get entry by host name
     *
     * @param host
     * @return entry associated with host name or null
     */
    public NetRCEntry getEntry(String host) throws IOException {
        if (netrc == null)
            return null;

        if (this.lastModified != this.netrc.lastModified())
            parse();

        NetRCEntry entry = this.hosts.get(host);

        if (entry == null)
            entry = this.hosts.get(DEFAULT_ENTRY);

        return entry;
    }

    /**
     * @return all entries collected from .netrc file
     */
    public Collection<NetRCEntry> getEntries() {
        return hosts.values();
    }

    private void parse() throws IOException {
        this.hosts.clear();
        this.lastModified = this.netrc.lastModified();

        BufferedReader bufRdr = null;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(netrc), HyraxStringEncoding.getCharset());
            bufRdr = new BufferedReader(isr);
            String line = null;

            NetRCEntry entry = new NetRCEntry();

            State state = State.COMMAND;

            String macbody = ""; //$NON-NLS-1$

            Matcher matcher = NETRC.matcher(""); //$NON-NLS-1$
            while ((line = bufRdr.readLine()) != null) {

                // reading macbody
                if (entry.macdef != null && entry.macbody == null) {
                    if (line.length() == 0) {
                        entry.macbody = macbody;
                        macbody = ""; //$NON-NLS-1$
                        continue;
                    }
                    macbody += line + "\n"; //$NON-NLS-1$;
                    continue;
                }

                matcher.reset(line);
                while (matcher.find()) {
                    String command = matcher.group().toLowerCase(Locale.ROOT);
                    if (command.startsWith("#")) { //$NON-NLS-1$
                        matcher.reset(""); //$NON-NLS-1$
                        continue;
                    }
                    state = STATE.get(command);
                    if (state == null)
                        state = State.COMMAND;

                    switch (state) {
                        case COMMAND:
                            break;
                        case ACCOUNT:
                            if (entry.account != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            if (matcher.find())
                                entry.account = matcher.group();
                            state = State.COMMAND;
                            break;
                        case LOGIN:
                            if (entry.login != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            if (matcher.find())
                                entry.login = matcher.group();
                            state = State.COMMAND;
                            break;
                        case PASSWORD:
                            if (entry.password != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            if (matcher.find())
                                entry.password = matcher.group().toCharArray();
                            state = State.COMMAND;
                            break;
                        case DEFAULT:
                            if (entry.machine != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            entry.machine = DEFAULT_ENTRY;
                            state = State.COMMAND;
                            break;
                        case MACDEF:
                            if (entry.macdef != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            if (matcher.find())
                                entry.macdef = matcher.group();
                            state = State.COMMAND;
                            break;
                        case MACHINE:
                            if (entry.machine != null && entry.complete()) {
                                hosts.put(entry.machine, entry);
                                entry = new NetRCEntry();
                            }
                            if (matcher.find())
                                entry.machine = matcher.group();
                            state = State.COMMAND;
                            break;
                    }
                }
            }

            // reading macbody on EOF
            if (entry.macdef != null && entry.macbody == null)
                entry.macbody = macbody;

            if (entry.complete())
                hosts.put(entry.machine, entry);
        } finally {
            if (bufRdr != null)
                bufRdr.close();
        }
    }
}