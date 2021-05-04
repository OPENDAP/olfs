#!/bin/bash

snyk test --severity-threshold=high
snykjson=$(snyk test --severity-threshold=high --json)
isUpgradable=$(jq '.vulnerabilities[].isUpgradable | tostring | select(. == "true")' <<< $snykjson)

if [[ $isUpgradable != '' ]]; then
    echo "To fix a problem you should change version of library in the build.gradle file
    and download library of new version into lib folder."
    exit 1
else
    exit 0
fi
