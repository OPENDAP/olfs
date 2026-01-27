#!/bin/bash
HR="-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --"
###########################################################################
# loggy()
#
function loggy(){
  echo  "$@" | awk '{ print "# "$0;}'  >&2
}

#
# Given that the BES has just pushed a new set of packages, built with the libdap
# RPMs, grab those and use them to make a new set of Docker containers. The
# hyrax-docker git repo runs its own build to do this (and can be triggered
# separately).

set -e
loggy "$HR"
loggy "trigger-hyrax-docker.sh"
loggy ""

git config --global user.name "The-Robot-Travis"
git config --global user.email "npotter@opendap.org"

# This will get the BES and libdap version numbers
BES_SNAPSHOT=$(cat ./bes-snapshot)

test_deploy=""
if [[ "$TRAVIS_BRANCH" == *"-test-deploy" ]]
then
  test_deploy=" test-deploy"
fi

loggy "TARGET_OS: $TARGET_OS"
loggy "OLFS_BUILD_VERSION: $OLFS_BUILD_VERSION"

OLFS_SNAPSHOT_TAG="olfs-${OLFS_BUILD_VERSION} "$(date "+%FT%T%z")"$test_deploy"
loggy " OLFS_SNAPSHOT_TAG: $OLFS_SNAPSHOT_TAG"

HYRAX_SNAPSHOT_TAG="hyrax-${HYRAX_BUILD_VERSION} "$(date "+%FT%T%z")"$test_deploy"
loggy "HYRAX_SNAPSHOT_TAG: $HYRAX_SNAPSHOT_TAG"
loggy ""

loggy "Tagging olfs with: ${OLFS_BUILD_VERSION}"
git tag -m "olfs-${OLFS_BUILD_VERSION}" -a "${OLFS_BUILD_VERSION}"

loggy "Pushing tags to origin..."
git push "https://${GIT_UID}:${GIT_PSWD}@github.com/OPENDAP/olfs.git" "${OLFS_BUILD_VERSION}"

loggy "New OLFS has been pushed and tagged. Triggering the Docker build..."

loggy "Cloning hyrax-docker"
git clone https://github.com/opendap/hyrax-docker

cd hyrax-docker
loggy "Checking out branch: $TARGET_OS"
git checkout "$TARGET_OS"

echo "TARGET_OS: $TARGET_OS" > snapshot.time
echo "${BES_SNAPSHOT}"       >> snapshot.time
echo "${OLFS_SNAPSHOT_TAG}"  >> snapshot.time
echo "${HYRAX_SNAPSHOT_TAG}" >> snapshot.time

loggy "Updated snapshot.time file:"
loggy "$(cat snapshot.time)"


# Bounding the commit message with the " character allows use to include
# new line stuff for easy commit message readability later.
loggy "Commiting snapshot.time file:"
git commit -am \
"OLFS: Triggering hyrax-docker image production.
Build Version Matrix:
${BES_SNAPSHOT}
${OLFS_SNAPSHOT_TAG}
${HYRAX_SNAPSHOT_TAG}
";
git status;

export hyrax_tag="hyrax-${HYRAX_BUILD_VERSION}"
loggy "Tagging hyrax-docker with: ${hyrax_tag}"
git tag -m "${hyrax_tag}" -a "${hyrax_tag}"


loggy "Pushing to changes hyrax-docker:master:"
git push "https://${GIT_UID}:${GIT_PSWD}@github.com/OPENDAP/hyrax-docker.git" --all
loggy "Pushing to tags hyrax-docker:master:"
git push "https://${GIT_UID}:${GIT_PSWD}@github.com/OPENDAP/hyrax-docker.git" "${hyrax_tag}"

loggy "done"
loggy "$HR"

