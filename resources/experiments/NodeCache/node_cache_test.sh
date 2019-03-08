#!/bin/bash

target_hyrax="http://test.opendap.org:8080/"

# Get site map:

siteMapFileList=`curl -s "${target_hyrax}/robots.txt" | awk '{print $2}' - `

# echo "${siteMapList}"

for siteMapFileUrl in ${siteMapFileList}
do
    echo "Site Map File: ${siteMapFileUrl}"

    siteMapContent="`curl -s ${siteMapFileUrl}`"

    echo "SITE MAP CONTENT:"
    echo "${siteMapContent}"
done