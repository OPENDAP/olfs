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

import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  A local SailRepository. The state (up/down) of the repository is
 *  recorded by <code>isRepositoryDown</code>.
 *
 */
public class IRISailRepository extends SailRepository {

    private static Logger log ;


    private AtomicBoolean isRepositoryDown;


    public IRISailRepository(Sail sail) {
        super(sail);
        log = LoggerFactory.getLogger(getClass());
        isRepositoryDown = new AtomicBoolean();
    }


    public void initialize() throws org.openrdf.repository.RepositoryException {
        super.initialize();
        isRepositoryDown.set(false);
    }

    public Boolean isRepositoryDown() {
        return isRepositoryDown.get();
    }

    /**
     * Does a repository shutdown, but only if the repository is running!.
     *
     * @throws RepositoryException
     */
    @Override
    public void shutDown() throws RepositoryException {

        log.debug("shutDown(): Shutting down Repository...");
        if (!isRepositoryDown.get()) {
            super.shutDown();
            isRepositoryDown.set(false);
            log.info("shutDown(): Semantic Repository Has Been Shutdown.");
        } else {
            log.info("shutDown(): Semantic Repository was already down.");
        }
        log.debug("shutDown(): Repository shutdown complete.");
    }



}