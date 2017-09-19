#!/bin/bash

years="1995 2005 2015"
months="07"

auth=" --load-cookies ~/.urs_cookies --save-cookies ~/.urs_cookies --keep-session-cookies ";
wget_opts=" -r -c -nH -nd -np -A nc4";

# wget --load-cookies ~/.urs_cookies --save-cookies ~/.urs_cookies --keep-session-cookies -r -c -nH -nd -np -A nc4,xml "https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2_MONTHLY/M2TMNXSLV.5.12.4/1981/"

function get_daily_hourly_collection () {
    collection_url=$1;
    collectionDir=`pwd`
    for year in $years
    do
        echo "Year: $year";
        mkdir -p $year;
        yearDir=$collectionDir"/"$year
        echo "YearDir: $yearDir"
        cd $yearDir
        for month in $months
        do
            echo "Month: $month";
            mkdir -p $month;
            monthDir=$yearDir"/"$month
            echo "MonthDir: $monthDir"
            cd $monthDir
            target_url=$collection_url"/"$year"/"$month"/";
            echo "Target URL: $target_url";
            echo "Download dir "`pwd`
            wget $auth $wget_opts $target_url
            
            cd $yearDir
        done
        cd $collectionDir
    done
}

function get_monthly_collection () {
    collection_url=$1;
    collectionDir=`pwd`
    for year in $years
    do
        echo "Year: $year";
        mkdir -p $year;
        yearDir=$collectionDir"/"$year
        echo "YearDir: $yearDir"
        cd $yearDir

        echo "Year: $year";
        target_url=$collection_url"/"$year"/";
        echo "Target URL: $target_url";
        wget $auth $wget_opts $target_url
        cd $collectionDir
    done
}



#month_url_example="https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2/M2I1NXASM.5.12.4/1980/06/"

# wget --load-cookies ~/.urs_cookies --save-cookies ~/.urs_cookies --keep-session-cookies -r -c -nH -nd -np -A nc4,xml "https://goldsmr4.gesdisc.eosdis.nasa.gov/data/MERRA2_MONTHLY/M2TMNXSLV.5.12.4/1981/"



for collection_url in `cat  one_month_collections`
do
    startDir=`pwd`
    myDir=tmp/`basename $collection_url`
    mkdir -p $myDir
    echo "--------------------------------------------------------------"
    echo "Retrieving collection: $collection_url";
    echo "collecitonDir: $myDir"
    cd $myDir
    get_monthly_collection $collection_url;
    cd $startDir
    
done


for collection_url in `cat  one_day_collections gpm_collections one_hour_collections`
do
    startDir=`pwd`
    myDir=tmp/`basename $collection_url`
    mkdir -p $myDir
    echo "--------------------------------------------------------------"
    echo "Retrieving collection: $collection_url";
    echo "collecitonDir: $myDir"
    cd $myDir
    get_daily_hourly_collection $collection_url;
    cd $startDir
    
done


