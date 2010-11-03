/////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2010 OPeNDAP, Inc.
// Author: James Gallagher  <jgallagher@opendap.org>
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

package opendap.metacat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.DateClassification.DatePart;

public class DateString implements Comparable<DateString> {

    private static Logger log = LoggerFactory.getLogger(DateString.class);

	private String theDate;
	private GregorianCalendar calendar;
	
	public DateString(String d, List<DatePart> dps) {
		theDate = d;
		calendar = getCalendar(d, dps);
	}
	
	public String getDateString() {
		return theDate; //calendar.toString();
	}
	
	public Date getDate() {
		return calendar.getTime();
	}
	
	private GregorianCalendar getCalendar(String d, List<DatePart> dps) {
		log.debug("Building Calendar for '" + d + "'");
		
		GregorianCalendar c = new GregorianCalendar();
		c.clear();
		int pos = 0;

		// Special case for two and four digit years
		if (dps.contains(DatePart.year)) {
			c.set(Calendar.YEAR, new Integer(d.substring(pos, pos + 4)));
			pos += 4;
		}
		else if (dps.contains(DatePart.year2)) {
			int year = new Integer(d.substring(pos, pos + 2));
			if (year < 70)
				year += 2000;
			else
				year += 1900;
			c.set(Calendar.YEAR, year);
			pos += 2;
		}

		if (dps.contains(DatePart.daynum)) {
			c.set(Calendar.DAY_OF_YEAR, new Integer(d.substring(pos, pos + 3)));
			pos += 3;
		}
		// Now special case for one and two digit months. Unlike the varying
		// year representations, this distinction has been lost in the parse,
		// so detect it hear using odd/even number of digits.
		if (dps.contains(DatePart.month)) {
			if (d.length() % 2 == 0) { // even, must be two-digit year
				c.set(Calendar.MONTH, new Integer(d.substring(pos, pos + 2)));
				pos += 2;
			}
			else {
				c.set(Calendar.MONTH, new Integer(d.substring(pos, pos + 1)));
				pos += 1;
			}
		}

		if (dps.contains(DatePart.day)) {
			c.set(Calendar.DAY_OF_MONTH, new Integer(d.substring(pos, pos + 2)));
			pos += 2;
		}
		if (dps.contains(DatePart.hours)) {
			c.set(Calendar.HOUR, new Integer(d.substring(pos, pos + 2)));
			pos += 2;
		}
		if (dps.contains(DatePart.minutes)) {
			c.set(Calendar.MINUTE, new Integer(d.substring(pos, pos + 2)));
			pos += 2;
		}
		if (dps.contains(DatePart.seconds))
			c.set(Calendar.SECOND, new Integer(d.substring(pos)));

		log.debug("The resulting calendar object: " + c.toString());
		
		return c;
	}

	@Override
	public int compareTo(DateString o) {
		return calendar.compareTo(o.calendar);
	}
}
