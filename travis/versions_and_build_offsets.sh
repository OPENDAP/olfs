#!/bin/bash
#
# When the release numbers are edited in configure.ac, update this
# to the current Travis number so that the 'build number' in
# x.y.z-<build number> is zero. jhrg 3/22/21
#
HR="=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-="
###########################################################################
# loggy()
function loggy() {
  echo "$@" | awk '{ print "# travis_bes_build_offset.sh() - "$0;}' >&2
}

loggy "BEGIN $HR"

# This is the offical OLFS version, sans build number.
export OLFS_VERSION="1.19.0"
loggy "            OLFS_VERSION: ${OLFS_VERSION}" >&2

# This is the TravisCI build number when the
# last formal release was built.
export TRAVIS_OLFS_BUILD_OFFSET=3256

loggy "TRAVIS_OLFS_BUILD_OFFSET: ${TRAVIS_OLFS_BUILD_OFFSET}" >&2

if [ "$TRAVIS_PULL_REQUEST" != "false" ]
then
  loggy "This is a Pull Request build for PR #$TRAVIS_PULL_REQUEST"
  loggy "Setting TRAVIS_OLFS_BUILD_OFFSET to 0"
  TRAVIS_OLFS_BUILD_OFFSET=0
fi

loggy "Using TRAVIS_OLFS_BUILD_OFFSET: $TRAVIS_OLFS_BUILD_OFFSET"
loggy "END $HR"
