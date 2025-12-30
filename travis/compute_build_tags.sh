#!/bin/bash

echo ""
echo "##########################################################################" >&2
echo "#" >&2
echo "# compute_build_tags.sh" >&2
echo "#" >&2
echo "#  pwd: "$(pwd) >&2
echo "#" >&2

echo "# TRAVIS_BUILD_NUMBER: ${TRAVIS_BUILD_NUMBER}" >&2
echo "#" >&2
source ./travis/versions_and_build_offsets.sh
echo "#" >&2

export BES_SNAPSHOT_FILE=${BES_SNAPSHOT_FILE:-"./bes-snapshot"}
if test -f "${BES_SNAPSHOT_FILE}"
then
    echo "#         BES_SNAPSHOT_FILE: ${BES_SNAPSHOT_FILE}" >&2
    # cat "${BES_SNAPSHOT_FILE}" | awk '{print "# "$0;}' >&2
else
    echo "ERROR: Failed to located the bes snapshot file: ${BES_SNAPSHOT_FILE}" >&2
    exit 2;
fi

td_tag_value="test-deploy"
export TEST_DEPLOY_TAG=""
if [[ "$TRAVIS_BRANCH" == *"$td_tag_value" ]]
then
    TEST_DEPLOY_TAG="$td_tag_value"
fi
if test -z "$TEST_DEPLOY_TAG"
then
    grep "test-deploy" "$BES_SNAPSHOT_FILE"
    if $? -eq 0
    then
        TEST_DEPLOY_TAG="$td_tag_value"
    fi
fi
echo "# TEST_DEPLOY_TAG: '${TEST_DEPLOY_TAG}'" >&2
echo "#" >&2


export OLFS_BUILD_NUMBER=$(expr $TRAVIS_BUILD_NUMBER - $TRAVIS_OLFS_BUILD_OFFSET)
export OLFS_BUILD_VERSION="${OLFS_VERSION}-${OLFS_BUILD_NUMBER}"
echo "#        OLFS_BUILD_VERSION: ${OLFS_BUILD_VERSION}" >&2

export HYRAX_BUILD_NUMBER=$(expr $TRAVIS_BUILD_NUMBER - $TRAVIS_HYRAX_BUILD_OFFSET)
export HYRAX_BUILD_VERSION="${HYRAX_VERSION}-${HYRAX_BUILD_NUMBER}"
echo "#       HYRAX_BUILD_VERSION: ${HYRAX_BUILD_VERSION}" >&2
echo "#" >&2

