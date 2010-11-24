#!/bin/sh
#
# Run the EML Retriever programs

profiler="-javaagent:/Users/jimg/src/jip-src-1.2/profile/profile.jar \
-Dprofile.properties=/Users/jimg/src/olfs/resources/metacat/profile.properties"

java -Xmn100M -Xms512M -Xmx2048M -jar ../libexec/URLClassifier.jar $*
