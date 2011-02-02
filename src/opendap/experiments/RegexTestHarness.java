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


package opendap.experiments;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class RegexTestHarness {


    public static void main(String[] args) throws Exception {
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

        String pString = null;
        String input = null;
        String k;

        while (true) {

            System.out.print("\nEnter your regex ["+pString+"]: ");
            k = kybrd.readLine();
            if(!k.equals(""))
                pString = k;
            Pattern pattern =
            Pattern.compile(pString);

            System.out.print("Enter input string to search["+input+"]: ");
            k = kybrd.readLine();
            if(!k.equals(""))
                input = k;
            Matcher matcher =
            pattern.matcher(input);

            boolean found = false;
            while (matcher.find()) {
                System.out.println("matcher.find() found the text \""+matcher.group()+"\" starting at " +
                   "index "+matcher.start()+" and ending at index "+matcher.end());
                found = true;
            }

            System.out.println("pattern.matcher("+input+").matches(): "+pattern.matcher(input).matches());



            if(!found){
                System.out.println("No match found.");
            }
        }
    }
}
