#!/bin/bash

years="1980 1985 1990 1995 2000 2005 2010 2015"
months="07"

auth=" --load-cookies tmp/urs_cookies --save-cookies tmp/urs_cookies --keep-session-cookies ";
wget_opts=" -r -c -nH -nd -np -A nc4";

# wget --load-cookies ~/.urs_cookies --save-cookies ~/.urs_cookies --keep-session-cookies -r -c -nH -nd -np -A nc4,xml "https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2_MONTHLY/M2TMNXSLV.5.12.4/1981/"

function get_daily_collection () {
    collection_url=$1;
    
    mkdir -p tmp;
    for year in $years
    do
        echo "Year: $year";
        for month in $months
        do
            echo "Month: $month";
            target_url=$collection_url"/"$year"/"$month"/";
            echo "Target URL: $target_url";
            wget "$auth" "$wget_opts" "$target_url"
        done
    done
}

function get_monthly_collection () {
    collection_url=$1;
    mkdir -p tmp;
    for year in $years
    do
        echo "Year: $year";
        target_url=$collection_url"/"$year"/";
        echo "Target URL: $target_url";
        wget $auth $wget_opts $target_url
    done
}



#month_url_example="https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2/M2I1NXASM.5.12.4/1980/06/"

# wget --load-cookies ~/.urs_cookies --save-cookies ~/.urs_cookies --keep-session-cookies -r -c -nH -nd -np -A nc4,xml "https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2_MONTHLY/M2TMNXSLV.5.12.4/1981/"



for collection_url in `cat one_month_collections`
do
    echo "--------------------------------------------------------------"
    echo "Retrieving collection: $collection_url";
    cd tmp
    get_monthly_collection $collection_url;
    cd ..
    
done
