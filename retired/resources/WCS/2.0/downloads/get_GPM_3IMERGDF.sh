#!/bin/bash

# https://gpm1.gesdisc.eosdis.nasa.gov/opendap/GPM_L3/GPM_3IMERGDF.04/contents.html

server="https://gpm1.gesdisc.eosdis.nasa.gov"
collection="opendap/GPM_L3/GPM_3IMERGDF.04"

# The data begin in 2014
for year in {2014..2017}; do
    echo "Processing Year $year"
    for month in 01 02 03 04 05 06 07 08 09 10 11 12 ; do
        echo "Processing Month: $month"
        #mkdir -p $year/$month
        echo "##############################################################################";
        thredds_url="$server/$collection/$year/$month/catalog.xml";
        echo "Retrieving THREDDS catalog: $thredds_url";
        curl -s "$thredds_url" > thredds_catalog.xml;
        cat thredds_catalog.xml;
        files=`cat thredds_catalog.xml | grep ID | grep nc4 | sed -e "s/ID=\"//g" -e "s/\">//g"`
        for file in $files; do
            echo "FILE: $file ";
            target=`basename $file`".nc";
            echo "TARGET: $target";            
            URL="$server$file.nc";
            echo "URL: $URL";

            get_next="false";
            #skip it if it looks good.
            local_size=`ls -l $target | awk '{print $5;}'`;
            echo "local_size: $local_size"
            if [ "$local_size" != "0" ] 
            then
                get_next="true";
                echo "$SKIPPING $file"
            fi
            
            let pass=0;
            while [ "$get_next" == "false" ]
            do
                echo "TARGET: $target ";
                let pass++;
                curl -k -n -c ~/ursCookies -b ~/ursCookies -s -L --url "$URL" > $target;
                result_size=`ls -l $target | awk '{print $5;}'`;
                echo "PASS: $pass  RESULT_SIZE: $result_size"
                if [ "$result_size" != "0" ] || [ pass -gt 10 ]; then
                    get_next="true";
                    echo "Completed file $target";
                fi
            done
        done     
    done
done

