/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2011 OPeNDAP, Inc.
//
// Authors:
//     Haibo Liu  <haibo@iri.columbia.edu>
//     Nathan David Potter  <ndp@opendap.org>
//     M. Benno Blumenthal <benno@iri.columbia.edu>
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
