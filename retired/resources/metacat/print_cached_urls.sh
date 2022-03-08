#!/bin/sh
#
# Run the PrintVisitedCache program

#profiler="-javaagent:/Users/jimg/src/jip-src-1.2/profile/profile.jar \
#-Dprofile.properties=/Users/jimg/src/olfs/resources/metacat/profile.properties"

java $profiler -Xms256m -Xmx1024m -jar ../libexec/PrintVisitedCache.jar $*

