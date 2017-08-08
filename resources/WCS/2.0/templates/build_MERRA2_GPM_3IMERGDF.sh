#!/bin/bash

# This collection contains daily precipitation data at 0.1 deg spatial resolution
server="https://gpm1.gesdisc.eosdis.nasa.gov"
collection="opendap/GPM_L3/GPM_3IMERGDF.04"

template="MERRA2_GPM_3IMERGDF.xml"
echo "TEMPLATE: $template";

# make sure we drop this stuff in a non-destructive spot...
mkdir -p -v tmp

# Start LFC.xml
cat LFC_OPEN.xml > tmp/LFC_$template;

# The data start in 2014
for year in {2014..2017}; do
    echo "Processing year $year"
    for month in 01 02 03 04 05 06 07 08 09 10 11 12 ; do
        echo "Processing Month: $month"
        #mkdir -p $year/$month
        files=`curl -s "$server/$collection/$year/$month/catalog.xml" | grep ID | grep nc4 | sed -e "s/ID=\"//g" -e "s/\">//g"`
        for file in $files; do
            echo "FILE: $file"
            target=`basename $file`
            echo "TARGET: $target"  
            dataset_url="$server/$file" # We add .nc4 to invoke returnAs since direct file downloads are disabled.
            echo "DATASET_URL: $dataset_url"
            
            # parse TARGET for day of month ex: MERRA2_400.statD_2d_slv_Nx.20120223.nc4
            
            day=`echo $target | awk '{split($0,s,".");split(s[5],f,"-");print f[1];}' | sed -e "s/$year$month//g"`
            echo "DAY: $day"
            
            # Metadat inspection revealed that each file represents a single day.
            # From the DAS response, HDF5_GLOBAL container:
            # String BeginDate "2014-05-01";
            # String BeginTime "00:00:00.000Z";
            # String EndDate "2014-05-01";
            # String EndTime "23:59:59.999Z";
            # So I construct a time period limited to the individual date, utilizing
            # the start end end times from the metadata.            
            start_date="$year-$month-$day";
            start_time="00:00:00" 
            echo "START_DATE: $start_date  START_TIME: $start_time"
            
            # the end time is a bit of a "punt"
            end_date=$start_date
            end_time="23:59:59.999"
            echo "END_DATE: $end_date  END_TIME: $end_time"

            # add coverage to LFC.xml
            cat LFC_$template | sed -e "s/@COVERAGE_NAME@/$target/g" -e "s/@DATE@/$start_date/g" >> tmp/LFC_$template
            
            # Make CoverageDescription file.
            cat $template  |  sed \
                -e "s/@MERRA2DATASET@/$target/g" \
                -e "s/@START_DATE@/$start_date/g"  \
                -e "s/@START_TIME@/$start_time/g"  \
                -e "s/@END_DATE@/$end_date/g" \
                -e "s/@END_TIME@/$end_time/g" \
                > tmp/$target.xml           
        done     
    done
done
# finish up LFC.xml
cat LFC_CLOSE.xml >> tmp/LFC_$template;
