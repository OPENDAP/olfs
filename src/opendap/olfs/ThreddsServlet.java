/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;

import thredds.servlet.CatalogRootHandler;
import thredds.servlet.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

import opendap.util.Debug;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 29, 2006
 * Time: 7:36:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ThreddsServlet extends HttpServlet {

   /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     */

    protected String rootPath, contentPath;
    protected CatalogRootHandler catHandler;
    protected org.slf4j.Logger log;

    public void init() throws ServletException {

        super.init();

        ServletUtil.initDebugging(this);
        ServletUtil.initLogging(this);


        // debug actions
        //makeDebugActions();   //Does the same thing as ServletUtil.initDebugging();

        rootPath = ServletUtil.getRootPath(this);
        contentPath = ServletUtil.getContentPath(this) + getPath();
        // handles all catalogs, including ones with DatasetScan elements, ie dynamic
        CatalogRootHandler.init(contentPath);
        catHandler = CatalogRootHandler.getInstance();
        List catList = getExtraCatalogs();
        catList.add(0, "catalog.xml"); // always first
        for (int i = 0; i < catList.size(); i++) {
          String catFilename = (String) catList.get(i);
          try {
            catHandler.initCatalog(catFilename);
          } catch (Throwable e) {
            log.error( "Error initializing catalog "+catFilename+"; "+e.getMessage(), e);
          }
        }

        //catHandler.makeDebugActions();
        //DatasetHandler.makeDebugActions();


    }

    protected void makeDebugActions() {
        // debuggering
        String debugOn = getInitParameter("DebugOn");
        if (debugOn != null) {
            System.out.println("++ DebugOn ++");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }
    }

    protected String getPath() { return ""; }

    private List getExtraCatalogs() {
      ArrayList extraList = new ArrayList();
      try {
        FileInputStream fin = new FileInputStream(contentPath + "extraCatalogs.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        while (true) {
          String line = reader.readLine();
          if (line == null) break;
          line = line.trim();
          if (line.length() == 0) continue;
          if ( line.startsWith( "#") ) continue; // Skip comment lines.
          extraList.add( line);
        }
        fin.close();

      } catch (FileNotFoundException e) {
        // its ok
      } catch (IOException e) {
        log.error("Error on getExtraCatalogs ",e);
      }

      return extraList;
    }







}
