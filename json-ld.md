# Hyrax and JSON-LD

## Overview

Hyrax produces embedded JSON-LD metadata in every "navigable catalog" page 
(aka pages that match: `*/contents.html`) and in every Data Request Form page. 
The `contents.html` pages each contain list of datasets linked to by that page.
The Data Request Form pages contain all of the Dataset metadata transformed into
JSON-LD.

Additionally, the server is now able to produce SiteMap files which are crucial
to publishing/submitting a particular site to search engines.

### What is JSON-LD?

JSON-LD (JavaScript Object Notation for Linked Data), is a method of encoding 
Linked Data using JSON.

Hyrax embeds JSON-LD content in every catalog and every Data Request Form page 
produced by the server.

### robots.txt
Web site owners use the `/robots.txt` file to give instructions about their site 
to web robots/crawlers. The `robots.txt` file matters in this discussion because 
it can be the primary mechanism for robots/crawlers to discover site map content. 

A simple `robots.txt` file might look like something like this:
```
User-agent: *
Disallow: /private
```
However, it is possible to explicitly specify one (or more) site map files 
in the robots.txt file like this:
```
sitemap: http://test.opendap.org:8080/opendap/siteMap/smap_0.txt
sitemap: http://test.opendap.org:8080/opendap/siteMap/smap_1.txt
```
Hyrax will generate both the sitemap files and, if you wish, the 
`robots.txt` file, dynamically.

### Site Maps

Hyrax can now automatically generate site map files. Additionally, Hyrax can be 
deployed with our ROOT web application that provides a dynamically generated 
`robots.txt` response for the Hyrax service. Is that is not feasible, the server
will still produce the site map content in a way that can be submitted to 
crawling services but that does not attempt to prduce a `robots.txt` file for
the site.

_From the [Wikipedia SiteMaps](https://en.wikipedia.org/wiki/Sitemaps) entry:_
> The Sitemaps protocol allows the Sitemap to be a simple list of URLs in a text 
file. The file specifications of XML Sitemaps apply to text Sitemaps as well; 
the file must be UTF-8 encoded, and cannot be more than 10 MB large or contain 
more than 50,000 URLs, but can be compressed as a gzip file.

So, the number of site map files produced is a function of both URL length and 
the number of things being served. Hyrax's dynamic site map service handles this
when generating its list of site map files.

### Hyrax SiteMap service

With the current release of Hyrax you can access the site map service like this:

`http://your.server/opendap/siteMap/`

For example, you can see our test server's robots.txt here:
http://test.opendap.org/opendap/siteMap/

Which currently lists a single site map file:
http://test.opendap.org/opendap/siteMap/smap_0.txt

If a server has more entries than are allowed in a single site map file then
it will break the site map into multiple files and list them in the site map
service resonse `opendap/siteMap/`

### The robots.txt web service
We have also produced a simple `ROOT` web application for Tomcat that provides a
dynamically generated `robots.txt` service. Using this will allow crawlers to 
easily locate the site map content automatically. If your site already utilizes 
a `ROOT` application for Tomcat then submitting the siteMap service to the various
crawlers will be the best path forward.

## GeoCODES and p418

GeoCODES is the new name for the NSF EarthCube's Project 418. We worked with 
them while developing our JSON-LD content and they have a search engine to 
ingest JSON-LD for the purposes of data discovery.

### Submit your SiteMap to GeoCODES
TBD

## Google and [Google Dataset Search](https://toolbox.google.com/datasetsearch)
[Google Dataset Search](https://toolbox.google.com/datasetsearch) is Google's 
data-centric search system (very nice). Our JSON-LD works there as well with the 
caveat that if the metadata for a particular dataset causes the Data Request 
Form page to exceed 2.5MB Google will ignore it. This can be a real issue for 
some data providers. (You _know_ who you are...)

### Submit your sitemap to Google 
Google provides instructions for
[Building and submiting a sitemap](https://support.google.com/webmasters/answer/183668?hl=en)
but since Hyrax builds the site map files(s) for you all you really need to do
it submit your site map which can be done very easily using curl:

`curl http://www.google.com/ping?sitemap=<complete_url_of_sitemap>`

So, for the OPeNDAP test server one could imagine that submitting the 
`robots.txt` should produce the desired result. 
```
curl http://www.google.com/ping?sitemap=http://test.opendap.org/robots.txt
```
If you are are not able to utilize the `ROOT` web application you can still
submit your site map by using the sitMap service directly:
```
curl http://www.google.com/ping?sitemap=http://test.opendap.org/opendap/siteMap/
```
It's not clear at this point if Google will actually ingest a `robots.txt` file via
their ping interface, but it is clear that it will ingest the site map files.
So, to be certain, goto the siteMap service (`http://your_server/opendap/siteMap/`)
and submit each site map file listed to the Google:
```
curl http://www.google.com/ping?sitemap=http://test.opendap.org/opendap/siteMap/smap_0.txt
curl http://www.google.com/ping?sitemap=http://test.opendap.org/opendap/siteMap/smap_1.txt
curl http://www.google.com/ping?sitemap=http://test.opendap.org/opendap/siteMap/smap_2.txt
```
etc.

Now prepare to reap the rewards of the search!!


