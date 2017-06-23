#!/bin/bash


# For example invoke like this:
# ./buildMerra2.sh MERRA2_400.tavgM_2d_int_Nx

collection=$1
echo "COLLECTION: $collection";

files=`curl -s https://s3.amazonaws.com/testbed-13/merra2//index.xml | grep $collection | awk '{split($2,f,"\"");print f[2];}'`;
echo FILES: $files;

cat LFC_OPEN.xml > tmp/LFC.xml;

for file in $files ; do
    echo "------------------------------------------"
    echo Processing: $file
    #curl https://s3.amazonaws.com/testbed-13/merra2/$i > $i;
    # MERRA2_400.tavgM_2d_int_Nx.201310.nc4

    year=`echo $file | awk '{print substr($0,28,4)};'`;
    month=`echo $file | awk '{print substr($0,32,2)};'`;

    start_date="$year-$month-01";
    echo "START_DATE: $start_date"

    end_date=`echo "$year $month" | awk '{y=$1+0.0;m=$2+0.0; if(m==12){y++;m=1;}else{m++;} printf("%04d-%02d-01",y,m)}'`
    echo "END_DATE: $end_date"

    cat LFC_MERRA2_400.tavgM_2d_int_Nx.xml | sed -e "s/@COVERAGE_NAME@/$file/g" -e "s/@DATE@/$start_date/g" >> tmp/LFC.xml

    cat $collection.xml  |  sed -e "s/@MERRA2DATASET@/$file/g" -e "s/@START_DATE@/$start_date/g"  -e "s/@END_DATE@/$end_date/g" > tmp/$file.xml

done
cat LFC_CLOSE.xml >> tmp/LFC.xml;
