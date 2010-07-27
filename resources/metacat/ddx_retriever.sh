#!/bin/sh
#
# Run the DDX Retriever program

java -Xms256m -Xmx1024m -jar ../libexec/DDXRetriever.jar $*
