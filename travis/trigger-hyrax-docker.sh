#!/bin/bash
#
# Given that the BES has just pushed a new set of packages, built with the libdap
# RPMs, grab those and use them to make a new set of Docker containers. The
# hyrax-docker git repo runs its own build to do this (and can be triggered
# separately).

set -e

echo "-- -- -- -- -- -- -- -- -- after_deploy BEGIN -- -- -- -- -- -- -- -- --"

BES_SNAPSHOT=`cat bes-snapshot`;

echo "New OLFS Web Archive snapshot has been pushed. Triggering the Docker build"

git clone https://github.com/opendap/hyrax-docker
git config --global user.name "The-Robot-Travis"
git config --global user.email "npotter@opendap.org"



cd hyrax-docker/hyrax-snapshot;
git checkout master;

OLFS_SNAPSHOT="OLFS-<version.build> "`date "+%FT%T%z"`

echo "${BES_SNAPSHOT}" > snapshot.time;
echo "${OLFS_SNAPSHOT}" >> snapshot.time;

cat snapshot.time;

git commit -am "The OLFS has produced new snapshot files. Triggering Hyrax-Docker image builds for snapshots.";
git status;
git push https://$GIT_UID:$GIT_PSWD@github.com/opendap/hyrax-docker --all;

echo "-- -- -- -- -- -- -- -- -- after_deploy END -- -- -- -- -- -- -- -- --"


