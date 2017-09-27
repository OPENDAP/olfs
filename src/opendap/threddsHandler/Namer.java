/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ndp on 4/18/15.
 */
public class Namer {
    Vector<NamerWorker> _workers;


    public Namer(Element namer, String parentCatalogPath) {
        _workers = new Vector<>();
        if (namer != null) {
            for (Object o : namer.getChildren()) {

                Element workerElement = (Element) o;

                NamerWorker nw = new NamerWorker(workerElement);

                _workers.add(nw);

            }
        }

    }


    public static class NamerWorker {

        private Logger _log;
        String regExp;
        String replaceString;

        boolean nameFromPathMatch;

        Pattern regExpPattern;

        public NamerWorker(Element worker) {

            _log = LoggerFactory.getLogger(this.getClass());

            if (worker == null ||
                    (!worker.getName().equals(THREDDS.REG_EXP_ON_NAME) && !worker.getName().equals(THREDDS.REG_EXP_ON_PATH)))
                return;

            nameFromPathMatch = worker.getName().equals(THREDDS.REG_EXP_ON_PATH);


            regExp = worker.getAttributeValue(THREDDS.REGEXP);
            if (regExp != null) {
                regExpPattern = Pattern.compile(regExp);
            } else {
                regExpPattern = Pattern.compile(".*$");

            }


            replaceString = worker.getAttributeValue(THREDDS.REPLACE_STRING);


        }


        String getName(String name) {

            if (name == null)
                return null;

            Matcher matcher;
            if (nameFromPathMatch) {
                _log.error("getPrefix() - {} not supported.", THREDDS.REG_EXP_ON_PATH);
                return null;
            } else {
                matcher = this.regExpPattern.matcher(name);
            }

            if (!matcher.find())
                return null;

            StringBuffer newName = new StringBuffer();
            matcher.appendReplacement(newName, this.replaceString);
            newName.delete(0, matcher.start());

            if (newName.length() == 0) return null;

            return newName.toString();

        }

    }

    public String getName(String name) {


        String newName = null;
        NamerWorker worker;
        Iterator<NamerWorker> i = _workers.iterator();

        while (newName == null && i.hasNext()) {
            worker = i.next();
            newName = worker.getName(name);
        }


        return newName;

    }

}
