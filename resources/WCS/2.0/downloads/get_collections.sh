#!/bin/bash

years="1980 1985 1990 1995 2000 2005 2010 2015"
months="07"

auth="--load-cookies tmp/urs_cookies --save-cookies tmp/urs_cookies --keep-session-cookies";
wget_opts=" --spider –r -c -nH -nd -np -A ";


function get_collection () {
    collection_url=$1;
    
    mkdir -p tmp;
    for year in $years
    do
        for month in $months
        do
            target_url=$collection_url"/"$year"/"$month;
            echo $target_url;
            wget $auth $wget_opts $target_url
        done
    done
        
}

#month_url_example="https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2/M2I1NXASM.5.12.4/1980/06/"

for collection_url in `cat *collections`
do
    get_collection $collection_url;
    
done
