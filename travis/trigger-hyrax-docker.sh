#!/bin/bash
#
# Given that the BES has just pushed a new set of packages, built with the libdap
# RPMs, grab those and use them to make a new set of Docker containers. The
# hyrax-docker git repo runs its own build to do this (and can be triggered
# separately).

set -e

echo "-- -- -- -- -- -- -- -- -- after_deploy BEGIN -- -- -- -- -- -- -- -- --"

# This will get both the BES and libdap version numbers
BES_SNAPSHOT=`cat bes-snapshot`;

echo "New OLFS Web Archive snapshot has been pushed. Triggering the Docker build"

git clone https://github.com/opendap/hyrax-docker
git config --global user.name "The-Robot-Travis"
git config --global user.email "npotter@opendap.org"

cd hyrax-docker
git checkout master

OLFS_SNAPSHOT_TAG="olfs-${OLFS_BUILD_VERSION} "`date "+%FT%T%z"`
HYRAX_SNAPSHOT_TAG="hyrax-${HYRAX_BUILD_VERSION} "`date "+%FT%T%z"`

echo "${BES_SNAPSHOT}" > snapshot.time
echo "${OLFS_SNAPSHOT_TAG}" >> snapshot.time
echo "${HYRAX_SNAPSHOT_TAG}" >> snapshot.time

cat snapshot.time

# Bounding the commit message with the " character allows use to include
# new line stuff for easy commit message readability later.
git commit -am \
"olfs: Triggering hyrax-docker image production.
Build Version Matrix:
  ${BES_SNAPSHOT}
  ${OLFS_SNAPSHOT_TAG}
  ${HYRAX_SNAPSHOT_TAG}
";

git status;
git push https://$GIT_UID:$GIT_PSWD@github.com/opendap/hyrax-docker --all;

echo "-- -- -- -- -- -- -- -- -- after_deploy END -- -- -- -- -- -- -- -- --"

