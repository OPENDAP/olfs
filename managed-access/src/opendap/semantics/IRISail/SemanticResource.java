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
package opendap.semantics.IRISail;

/**
  * This simple class holds a (semantic) resource definition.
 */
public class SemanticResource {
    private String _localId;
    private String _namespaceUri;

    /**
     * Builds a new SemanticResource.
     * @param namespaceUri The namespace to which the resource belongs.
     * @param localId The locally unique (within the namespace) ID of the resource.
     */
    public SemanticResource(String namespaceUri, String localId){
        _localId = localId;
        _namespaceUri = namespaceUri;
    }

    /**
     *
     * @return THe local ID of the resource.
     */
    public String getLocalId(){
        return _localId;
    }

    /**
     *
     * @return  The complete ID of the resource. (Essentially this is namespace+localID)
     */
    public String getUri(){
        return _namespaceUri+_localId;
    }

    /**
     *
     * @return The namespace to which this resource belongs.
     */
    public String getNamespace(){
        return _namespaceUri;
    }

    /**
     *
     * @return Returns the same as getUri()
     */
    public String toString(){
        return getUri();
    }
}
