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

td_tag_value="-test-deploy"
test_deploy_tag=""
if [[ "$TRAVIS_BRANCH" == *"$td_tag_value" ]]
then
    test_deploy_tag="$td_tag_value"
fi
if test -z "$test_deploy_tag"
then
    grep "test-deploy" "$BES_SNAPSHOT_FILE"
    if $? -eq 0
    then
        test_deploy_tag="$td_tag_value"
    fi
fi
echo "# test_deploy_tag: '${test_deploy_tag}'" >&2
echo "#" >&2


export OLFS_BUILD_NUMBER=$(expr $TRAVIS_BUILD_NUMBER - $TRAVIS_OLFS_BUILD_OFFSET)
export OLFS_BUILD_VERSION="${OLFS_VERSION}-${OLFS_BUILD_NUMBER}${test_deploy_tag}"
echo "#        OLFS_BUILD_VERSION: ${OLFS_BUILD_VERSION}" >&2

export HYRAX_BUILD_NUMBER=$(expr $TRAVIS_BUILD_NUMBER - $TRAVIS_HYRAX_BUILD_OFFSET)
export HYRAX_BUILD_VERSION="${HYRAX_VERSION}-${HYRAX_BUILD_NUMBER}${test_deploy_tag}"
echo "#       HYRAX_BUILD_VERSION: ${HYRAX_BUILD_VERSION}" >&2
echo "#" >&2

