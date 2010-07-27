#!/bin/sh
#
# Run the DDX Crawler program

java -Xms256m -Xmx1024m -jar ../libexec/DDXCrawler.jar $*
