/*
 * /////////////////////////////////////////////////////////////////////////////
 * This file is part of the "Hyrax Data Server" project.
 *
 *
 * Copyright (c) 2025 OPeNDAP, Inc.
 * Author: Nathan David Potter  <ndp@opendap.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 *
 * You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.coreServlet;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;


/**
 * This class is used to ensure that request id values transmitted to the BES are always unique.
 * This is accomplished by taking a supplied request id string (which may have been submitted in
 * the request headers, possibly repeatedly) and pairing it with a uuid string.
 * @author Nathan Potter
 */
public class RequestId {
    private String id;
    private UUID uuid;

    /**
     * Make a new RequestId object using the thread name and thread id strings to construct the request id string.
     * Of course a uuid will be generated as well.
     */
    public RequestId() {
        id = Thread.currentThread().getName() + "_" + Thread.currentThread().getId();
        uuid = UUID.randomUUID();
    }

    /**
     * Make a new RequestId object using the string id as the request id value.
     * @param id The string to use as the request id.
     */
    public RequestId(String id) {
        this.id = id;
        uuid = UUID.randomUUID();
    }

    /**
     *
     * @return The request id
     */
    public String id() {
        return id;
    }

    /**
     *
     * @return The uuid string that assures that this RequestId is unique.
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Returns a concatenation of the request id and uuid strings for use in logging. This
     * combination is reasonably assured of being unique.
     * If a client submits multiple requests while providing the same request id information
     * this method is still going to produce unique request id values for the various logs.
     * The BES receives both the id and the uuid when commands are sent, and contains identical
     * operations to construct unique request ids for its logs using the passed in values.
     * This allows us to merge the various log streams for later analysis.
     * @return The request id string for logging = id + "-" + uuid
     */
    public String logId(){
        return id + "-" + uuid.toString();
    }

    public String toString(){
        String s = "";
        s += "\"request_id\": { \"id\": \"" + id + "\", \"uuid\": \"" + uuid.toString() + "\"}";
        return s;
    }
}
