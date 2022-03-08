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


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 5:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class Animal {
    public static void staticMethod() {
        System.out.println("The staticMethod in Animal.");
    }
    public void testInstanceMethod() {
        System.out.println("The instance method in Animal.");
    }



    public static void main(String[] args) {

        String s = "///////";
            // Condition applicationID.
            if (s != null)
            {
                while (s.startsWith("/")) { // Strip leading slashes
                    s = s.substring(1, s.length());
                    System.out.println("s="+s);

                }
                if (s.equals("")){
                    System.out.println("s is empty string so we are making it null.");
                    s = null;
                }
            }

            if(s == null){
                System.out.println("s is now null.");
                return;
            }


    }



}

