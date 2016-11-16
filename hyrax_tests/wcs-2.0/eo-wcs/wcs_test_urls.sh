#!/bin/sh


WCS_SERVICE="http://localhost:8080/WCS-2.0?service=WCS&version=2.0.1"


# $WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=isobaric(0.0,500.0)&subset=time(0)



curl "http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc.dods?grid(u-component_of_wind_isobaric,\"0.0<=isobaric<=500.0\"),grid(v-component_of_wind_isobaric,\"0.0<=isobaric<=500.0\"),u-component_of_wind_isobaric\[0\]\[*\]\[*\]\[*\],v-component_of_wind_isobaric\[0\]\[*\]\[*\]\[*\]"



 curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=isobaric(0.0,500.0)&subset=time(0)"> foo.nc





curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=lat(-20.0,20.0)&subset=isobaric(0.0,500.0)&subset=time(0)&mediaType=multipart/related" > foo.nc




# EO Footprint (Req 45):
# "If, in a successful GetCoverage request on an EO Coverage, trimming along spatial coordi- nates is specified then the footprint of the EOWCS::EOMetadata in the coverage returned shall be given by the intersection of the spatial request interval and the footprint of the cover- age requested. Otherwise, the footprint in the result coverage shall be given by the footprint of the coverage requested."

# Where the definition is supplied via a combination of the EO-WCS and GML schemas:

# GML: A LinearRing is defined by four or more coordinate tuples, with linear interpolation between them; the first and last coordinates shall be coincident. The number of direct positions in the list shall be at least four.

# EOWCS: Acquisition footprint coordinates, described by a closed polygon (last point=first point), using CRS:WGS84, Latitude,Longitude pairs (per-WGS84 definition of point ordering, not necessarily per all WFS implementations). Expected structure is gml:Polygon/gml:exterior/gml:LinearRing/gml:posList.
# eop/EOLI : polygon/coordinates (F B b s)

# This definition is woefully inadequate for any type of coverage whose coordinate dimensionality is greater than 2. Defining a bounding object in 3D must minimally be a set of 4 planes (or a sphere or some collection of intersecting surfaces) and a general solution enclosing case fo N-dimensions may be challenging to define and then represent.






curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=lat(-20.0,20.0)&subset=isobaric(0.0,500.0)&subset=time(0)&mediaType=multipart/related" > foo.nc


curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=lon(-125.0,-116.4)&subset=lat(41.9,46.3)&subset=isobaric(0.0,500.0)&subset=time(0)&mediaType=multipart/related" > foo.nc


curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=longitude(55.0,63.6)&subset=latitude(41.9,46.3)&subset=isobaric(0.0,500.0)&subset=time(0)&mediaType=multipart/related" > foo.nc


curl "http://testbed-12.opendap.org:8080/WCS-2.0?service=WCS&version=2.0.1&request=GetCoverage&coverageId=eo_ncep_model_example&subset=longitude(55.0,63.6)&subset=latitude(41.9,46.3)&subset=isobaric(0.0,500.0)&subset=time(0)&mediaType=multipart/related" > foo.nc


########################
#
#
# EO_NCEP   DAP REquests
#
#
# DDS: http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc.dds
# DMR: http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc.dmr
#
# http:://localhost:8080/opendap/testbed-12/ncep/


curl "$WCS_SERVICE&request=DescribeCoverage&coverageId=eo_ncep_model_example"


curl "http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc.dods?grid(u-component_of_wind_isobaric,\"41.9<=lat<=46.3\",\"0.0<=isobaric<=500.0\",\"55.0<=lon<=63.6\"),grid(v-component_of_wind_isobaric,\"41.9<=lat<=46.3\",\"0.0<=isobaric<=500.0\",\"55.0<=lon<=63.6\"),u-component_of_wind_isobaric\[0\]\[*\]\[*\]\[*\],v-component_of_wind_isobaric\[0\]\[*\]\[*\]\[*\]" > foo.nc



curl "http://localhost:8080/opendap/testbed-12/ncep/Global_0p25deg_best_hs002.nc.dods?grid(u-component_of_wind_isobaric,\"41.9<=lat<=46.3\",\"0.0<=isobaric<=500.0\",\"55.0<=lon<=63.6\"),u-component_of_wind_isobaric\[0\]\[*\]\[*\]\[*\]" > foo.nc




curl "$WCS_SERVICE&request=GetCoverage&coverageId=eo_ncep_model_example&subset=longitude(55.0,63.6)&subset=latitude(41.9,46.3)&subset=isobaric(0.0,500.0)&subset=time(0)&scaleFactor=.1" > foo.nc;



###########################################################################################################################################################################
###########################################################################################################################################################################
SF_1in100

WCS_SERVICE="http://localhost:8080/WCS-2.0?service=WCS&version=2.0.1"
COVERAGE_ID="eo_newSF_1in100_reorder,nc"

http://localhost:8080/WCS-2.0?service=WCS&version=2.0.1&request=DescribeCoverage&coverageId=$COVERAGE_ID

http://testbed-12:8080/opendap/testbed-12/NETCDF_Flood/newSF_1in100_reorder.nc.dds

#        Float64 depth[time = 108][latitude = 4800][longitude = 4800];

latitude     min:   37.3333332966667  max:   38.6663888522222
longitude    min: -122.666388920222   max: -121.333333364667


latSubset="subset=latitude(37.6,38.1)"
lonSubset="subset=longitude(-122.2,-121.7)"

curl "$WCS_SERVICE&request=GetCoverage&coverageId=$COVERAGE_ID&$latSubset&$lonSubset&subset=time(0)&scaleFactor=.1" > foo.nc;





curl "http://localhost:8080/opendap/testbed-12/NETCDF_Flood/2d.nc4.dds?scale_grid(depth,48,48)"
