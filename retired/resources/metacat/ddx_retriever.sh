#!/bin/sh
#
# Run the DDX Retriever program

#profiler="-javaagent:/Users/jimg/src/jip-src-1.2/profile/profile.jar \
#-Dprofile.properties=/Users/jimg/src/olfs/resources/metacat/profile.properties"

java $profiler -Xms256m -Xmx1024m -jar ../libexec/DDXRetriever.jar $*
