#!/bin/bash


# For example invoke like this:
# ./buildMerra2.sh MERRA2_400.tavgM_2d_int_Nx

collection=$1
echo $collection

files=`curl -s https://s3.amazonaws.com/testbed-13/merra2//index.xml | grep $collection | awk '{split($2,f,"\"");print f[2];}'`;
echo $files;

cat LFC_OPEN.xml > tmp;

for file in $files ; do
    echo Processing $file
    #curl https://s3.amazonaws.com/testbed-13/merra2/$i > $i;


    # MERRA2_400.tavgM_2d_int_Nx.201310.nc4

    year=`echo $file | awk '{print substr($0,28,4)};'`;
    month=`echo $file | awk '{print substr($0,32,2)};'`;
    date="$year-$month-01";
    echo "DATE: $date"
    cat LFC_MERRA2_400.tavgM_2d_int_Nx.xml | sed -e "s/@COVERAGE_NAME@/$file/g" -e "s/@DATE@/$date/g" >> tmp

done
cat LFC_CLOSE.xml >> tmp;
