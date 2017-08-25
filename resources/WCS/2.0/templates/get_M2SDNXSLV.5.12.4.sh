#!/bin/bash


server="https://goldsmr4.gesdisc.eosdis.nasa.gov"

# The data start at 1980, but I'm getting 20 years (~20GB) to save space
for year in {1997..2017}; do
    echo "Processing year $year"
    for month in 01 02 03 04 05 06 07 08 09 10 11 12 ; do
        echo "Processing Mnth: $month"
        #mkdir -p $year/$month
        files=`curl -s "$server/opendap/MERRA2/M2SDNXSLV.5.12.4/$year/$month/catalog.xml" | grep ID | grep nc4 | sed -e "s/ID=\"//g" -e "s/\">//g"`
        for file in $files; do
            echo "FILE: $file"
            target=`basename $file`
            echo "TARGET: $target"  
            curl -k -n -c ~/ursCookies -b ~/ursCookies -s -L --url "$server/$file.nc4" > $target
        done     
    done
done


# https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2SDNXSLV.5.12.4/1980/06/MERRA2_100.statD_2d_slv_Nx.19800601.nc4.dds

#              https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2SDNXSLV.5.12.4/1980/06/catalog.xml
