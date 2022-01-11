#!bin/bash
#
# When the release numbers are edited for a release, update these
# to the current Travis number so that the 'build number' in
# x.y.z-<build number> is zero. jhrg 3/22/21

OLFS_VERSION=1.18.10
TRAVIS_OLFS_BUILD_OFFSET=1161
echo "OLFS_VERSION: ${OLFS_VERSION}"
echo "TRAVIS_OLFS_BUILD_OFFSET: ${TRAVIS_OLFS_BUILD_OFFSET}"

HYRAX_VERSION=1.16.5
TRAVIS_HYRAX_BUILD_OFFSET=1161
echo "HYRAX_VERSION: ${HYRAX_VERSION}"
echo "TRAVIS_HYRAX_BUILD_OFFSET: ${TRAVIS_HYRAX_BUILD_OFFSET}"
