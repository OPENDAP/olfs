#!/bin/bash


# For example invoke like this:
# ./buildMerra2.sh MERRA2_400.tavgM_2d_int_Nx

server="https://goldsmr4.gesdisc.eosdis.nasa.gov"

template="MERRA2_200.statD_2d_slv_Nx.xml"
echo "TEMPLATE: $template";

cat LFC_OPEN.xml > tmp/LFC.xml;

# The data start at 1980, but I'm getting 5 years (~5GB) to save space
for year in {2012..2017}; do
    echo "Processing year $year"
    for month in 01 02 03 04 05 06 07 08 09 10 11 12 ; do
        echo "Processing Mnth: $month"
        #mkdir -p $year/$month
        files=`curl -s "$server/opendap/MERRA2/M2SDNXSLV.5.12.4/$year/$month/catalog.xml" | grep ID | grep nc4 | sed -e "s/ID=\"//g" -e "s/\">//g"`
        for file in $files; do
            #echo "FILE: $file"
            target=`basename $file`
            #echo "TARGET: $target"  
            dataset_url="$server/$file" # We add .nc4 to invoke returnAs since direct file downloads are disabled.
            #echo "DATASET_URL: $dataset_url"
            
            # get time value
            curl -k -n -c ~/ursCookies -b ~/ursCookies -s -L --url "$dataset_url.ascii?time" | grep time
            
            
                            
            #start_date="$year-$month-01";
            #echo "START_DATE: $start_date"
            
            #end_date=`echo "$year $month" | awk '{y=$1+0.0;m=$2+0.0; if(m==12){y++;m=1;}else{m++;} printf("%04d-%02d-01",y,m)}'`
            #echo "END_DATE: $end_date"
            
            #cat LFC_$collection | sed -e "s/@COVERAGE_NAME@/$file/g" -e "s/@DATE@/$start_date/g" >> tmp/LFC.xml
            
            #cat $collection.xml  |  sed -e "s/@MERRA2DATASET@/$file/g" -e "s/@START_DATE@/$start_date/g"  -e "s/@END_DATE@/$end_date/g" > tmp/$file.xml
          
            
        done     
    done
done
cat LFC_CLOSE.xml >> tmp/LFC.xml;
