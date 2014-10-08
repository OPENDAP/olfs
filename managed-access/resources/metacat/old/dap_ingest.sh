#!/bin/sh
#
# Run the EML Retriever programs

java -Xmn100M -Xms500M -Xmx500M -jar ../libexec/DapIngest.jar $*
