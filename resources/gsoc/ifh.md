# OPeNDAP Inc.

## Mentors

- James Gallagher
- Nathan Potter
- Kodi Neumiller

## Information for students
See
[ESIP Student Guide](https://github.com/ESIPFed/gsoc/blob/master/STUDENT-contribution-guide.md)

## Project

###  Geo-Selection For Hyrax Data Request Form

#### Abstract
The Hyrax Data Request Form (aka IFH) provides a UI from which people can choose
and subset variables in a dataset. This project is to prototype adding a map
interface that displays the bounds geographic domain coordinate variables in a
dataset and that allows users to generate
subset values for these domain coordinates by using a mouse to select an
area on the map, or by entering latitude and longitude values in to text boxes
associated with the map UI.

#### Technical Details
Much of the data served by Hyrax contains geolocation information, however no
explicit use of these data are employed in the IFH. This has been a challenge
because there is little consistency in the naming of geographic domain coordinate
variables in the many datasets served by Hyrax.

The project is to develop at least one, but hopefully several, protoype versions
of the IFH based on different datasets served by Hyrax. Each of these protoypes
will include the map UI and will connect the map UI to the underlying dataset by
employing a heuristic approach to determining which of the variables in the
dataset are the geographic domain coordinates.

Part of this heuristic should include searching the dataset and variable
metadata for the
[Climate Forecast (CF)](http://cfconventions.org) convention terms that would
easily identify the geographic
domain coordinate variables within the dataset. If the dataset is either not
advertising (in the metadata) that it is using the CF convention, or if it claims
to utilize CF yet does not, the software should evaluate obvious alternative 
possible names for geographic domain coordinate variables such as:

- *latitude* == latitude | lat | y | Y
- *longitude* == longitude | lon | x | X


##### JSON Representations
The current IFH pages are built from XML encoded metadata. However the server 
can produce JSON encoded data responses and these might be utilized if data 
retrieval is desired. 

##### Utilize Existing Mapping Libraries
There are several powerful Javascript libraries that provide mapping display 
services. One of these should be utilized to perform this work. Some examples of 
these are:

- [OpenLayers](https://openlayers.org)
- [D3](https://d3js.org)
- [Leaflet](https://leafletjs.com)

This list is just a tiny sample of what might be used and is neither an 
endorsement or recommendation about should utilized in your work..

##### IFH Examples
These examples show various datasets from different origins that we have 
collected on our test server, test.opendap.org. Each example provides a link to 
the IFH for the dataset and a second link to the associated metadata response 
that was used to construct the IFH.

- _MODIS_: 
[IFH](http://test.opendap.org/opendap/data/nc/20070917-MODIS_A-JPL-L2P-A2007260000000.L2_LAC_GHRSST-v01.nc.html),
[Metadata](http://test.opendap.org/opendap/data/nc/20070917-MODIS_A-JPL-L2P-A2007260000000.L2_LAC_GHRSST-v01.nc.ddx),
- _COADS_: 
[IFH](http://test.opendap.org/opendap/data/nc/coads_climatology.nc.html),
[Metadata](http://test.opendap.org/opendap/data/nc/coads_climatology.nc.ddx)
- _NSCAT_: 
[IFH](http://test.opendap.org/opendap/data/hdf4/S2000415.HDF.gz.html), 
[Metadata](http://test.opendap.org/opendap/data/hdf4/S2000415.HDF.gz.ddx)
- _AIRS_: 
[IFH](http://test.opendap.org/opendap/AIRS/AIRH3STM.003/2003.02.01/AIRS.2003.02.01.L3.RetStd_H028.v4.0.21.0.G06116143217.hdf.html),
[Metadata](http://test.opendap.org/opendap/AIRS/AIRH3STM.003/2003.02.01/AIRS.2003.02.01.L3.RetStd_H028.v4.0.21.0.G06116143217.hdf.ddx)
- _Pathfinder_:
[IFH](http://test.opendap.org/opendap/noaa_pathfinder/2005001-2005008.s0484pfv50-sst.hdf.html),
[Metadata](http://test.opendap.org/opendap/noaa_pathfinder/2005001-2005008.s0484pfv50-sst.hdf.ddx)
- _VIIRS_: 
[IFH](http://test.opendap.org/opendap/trink/GMTCO_npp_d20120120_t0528446_e0530088_b01189_c20120120114656525950_noaa_ops.h5.html),
[Metadata](http://test.opendap.org/opendap/trink/GMTCO_npp_d20120120_t0528446_e0530088_b01189_c20120120114656525950_noaa_ops.h5.ddx)

#### Helpful Experience

- Javascript
- Java
- XSLT (nice, but not a must)

#### First Steps

- Examine the IFH examples in the _Technical Details_ section along with the 
metadata responses that were used build them.

- You might try using Google with the terms _OPeNDAP_ and _Hyrax_ in order to 
see the IFH produced by servers with real (not test) data. 
_Caveat: Most of what you will find doing this will be older versions of the 
Hyrax server which produce an antiquated version of the IFH. These server's 
metadata will be correct and data responses will be correct._

- Think about how you might propose to develop these prototypes. The expectation 
is not for you to integrate the result into the dynamic page generation of the 
server, but rather to build standalone pages based on what is currently being 
produced that can be used by OPeNDAP programmers as a basis for the dynamically 
generated page content.
- **Contact us!**
