#!/bin/bash
#
# When the release numbers are edited for a release, update these
# to the current Travis number so that the 'build number' in
# x.y.z-<build number> is zero. jhrg 3/22/21

OLFS_VERSION=1.18.15
TRAVIS_OLFS_BUILD_OFFSET=2188
echo "#              OLFS_VERSION: ${OLFS_VERSION}" >&2
echo "#  TRAVIS_OLFS_BUILD_OFFSET: ${TRAVIS_OLFS_BUILD_OFFSET}" >&2

HYRAX_VERSION=1.17.1
TRAVIS_HYRAX_BUILD_OFFSET=2188
echo "#             HYRAX_VERSION: ${HYRAX_VERSION}" >&2
echo "# TRAVIS_HYRAX_BUILD_OFFSET: ${TRAVIS_HYRAX_BUILD_OFFSET}" >&2
