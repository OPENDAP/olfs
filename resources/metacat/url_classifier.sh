#!/bin/sh
#
# Run the EML Retriever programs

java -Xmn100M -Xms512M -Xmx1024M -jar ../libexec/URLClassifier.jar $*
