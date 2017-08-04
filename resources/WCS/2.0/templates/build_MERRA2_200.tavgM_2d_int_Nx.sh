#!/bin/bash



server="https://goldsmr4.gesdisc.eosdis.nasa.gov"

template="MERRA2_200.statD_2d_slv_Nx.xml"
echo "TEMPLATE: $template";

# make sure we drop this stuff in a non-destructive spot...
mkdir -p -v tmp

# Start LFC.xml
cat LFC_OPEN.xml > tmp/LFC.xml;

# The data start at 1980, but I'm just doing getting 5 years (~5GB) to save space
for year in {2012..2017}; do
    echo "Processing year $year"
    for month in 01 02 03 04 05 06 07 08 09 10 11 12 ; do
        echo "Processing Mnth: $month"
        #mkdir -p $year/$month
        files=`curl -s "$server/opendap/MERRA2/M2SDNXSLV.5.12.4/$year/$month/catalog.xml" | grep ID | grep nc4 | sed -e "s/ID=\"//g" -e "s/\">//g"`
        for file in $files; do
            echo "FILE: $file"
            target=`basename $file`
            #echo "TARGET: $target"  
            dataset_url="$server/$file" # We add .nc4 to invoke returnAs since direct file downloads are disabled.
            #echo "DATASET_URL: $dataset_url"
            
            # parse TARGET for day of month ex: MERRA2_400.statD_2d_slv_Nx.20120223.nc4
            
            day=`echo $target | awk '{split($0,s,".");print s[3];}' | sed -e "s/$year$month//g"`
            echo "DAY: $day"
            
            # I inspected the time values and they are all "690" or 11:30 AM
            # So I construct a time period limited to the individual date, begining with the 
            # the start time in the dataset metadata            
            start_date="$year-$month-$day";
            start_time="00:30:00" 
            echo "START_DATE: $start_date  START_TIME: $start_time"
            
            # the end time is a bit of a "punt"
            end_date=$start_date
            end_time="23:30:00"
            echo "END_DATE: $end_date  END_TIME: $end_time"

            # add coverage to LFC.xml
            cat LFC_$template | sed -e "s/@COVERAGE_NAME@/$target/g" -e "s/@DATE@/$start_date/g" >> tmp/LFC.xml
            
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
cat LFC_CLOSE.xml >> tmp/LFC.xml;
