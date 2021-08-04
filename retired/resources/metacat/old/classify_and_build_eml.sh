#!/bin/sh
#
# Run the ClassifyServerBuildEML program

java -Xmn100M -Xms512M -Xmx1024M -jar ../libexec/ClassifyServerBuildEML.jar $*
