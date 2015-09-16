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
package opendap.logging;

/**
 * A Simple helper class for the Timer. Because Timer has static methods Procedure
 * could not be an inner class without becoming a singlton.
 *
 * Created by ndp on 5/18/15.
 *
 *
 */
public class Procedure {
    String name;
    long start;
    long end;

    /**
     * Records System.nanoTime() as the start value and sets end to same value as start;
     */
    public void start(){
        start = System.nanoTime();
        end = start;
    }

    /**
     *  Records System.nanoTime() as the end value
     */
    public void end(){
        end = System.nanoTime();
    }

    /**
     *
     * @return   Elapsed time for this procedure in milliseconds.
     * If the returned time is zero chances are extremely good that the end() method was not called.
     */
    public double elapsedTime(){
        return (end - start)/1000000.00;
    }


}
