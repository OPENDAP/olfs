#!/bin/sh
#
# Run the Thredds Crawler programs

java -Xms256m -Xmx1024m -jar ../libexec/ThreddsCrawler.jar $*
