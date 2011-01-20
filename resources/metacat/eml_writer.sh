#!/bin/sh
#
# Run the DDX Retriever program

#profiler="-javaagent:/Users/jimg/src/jip-src-1.2/profile/profile.jar \
#-Dprofile.properties=/Users/jimg/src/olfs/resources/metacat/profile.properties"

java $profiler -Xms512m -Xmx2048m -jar ../libexec/EMLWriter.jar $*
