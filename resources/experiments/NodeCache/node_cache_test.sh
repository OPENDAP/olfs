#!/bin/bash

target_hyrax="http://3.91.237.29:8080/"
sitemapFileName="site_map.txt"
catalogsFile="catalogs.txt"
datasetsFile="datasets.txt"


maxRandom=32767;


function retrieveSitemap() {

    rm -f ${sitemapFileName}

    echo "Retrieving site map."

    time siteMapFileList=`curl -s "${target_hyrax}/robots.jsp" | awk '{print $2}' - | tee robots.txt`

    echo "Site map file list: "
    echo "${siteMapFileList}"

    for siteMapFileUrl in ${siteMapFileList}
    do
        echo "Site Map File: ${siteMapFileUrl}"
        sfile=`echo "${siteMapFileUrl}" | sed -e "s=http://==g"`
        sfile=`basename ${sfile}`
        echo "sfile: ${sfile}"
        time -p curl -s ${siteMapFileUrl} | tee -a ${sitemapFileName} > ${sfile}
    done
    
    grep contents.html ${sitemapFileName} > ${catalogsFile};
    numCat=`wc -l ${catalogsFile}`; 
    grep -v contents.html ${sitemapFileName} > ${datasetsFile}; 
    numData=`wc -l ${datasetsFile}`; 
    echo "Found ${numData} datasets in ${numCat} catalogs";

}


function mkCatalogs() {
    echo "----- mkCatalogs()"
    catalogsPerLevel=$1;
    echo "catalogsPerLevel: ${catalogsPerLevel}";
    
    baseDir=`pwd`
    echo "baseDir: ${baseDir}"
    for catNum in `seq 1 ${catalogsPerLevel}`
    do
        thisCat="cat${catNum}";
        echo "thisCat: ${thisCat}";
        mkdir -pv ${thisCat};
    done
    
}

function buildTestTreeDirs {
    catalogsPerLevel=$1;
    echo "catalogsPerLevel: ${catalogsPerLevel}";

    catBase=`pwd`/toplevel;
    mkdir -pv ${catBase};
    cd ${catBase};

    mkCatalogs ${catalogsPerLevel}
    for i in * ; 
    do 
        cd $i;  
        mkCatalogs ${catalogsPerLevel} ; 
        for i in * ; 
        do 
            cd $i;  
            mkCatalogs ${catalogsPerLevel} ; 
            for i in * ; 
            do 
                cd $i;  
                mkCatalogs ${catalogsPerLevel} ; 
                for i in * ; 
                do 
                    cd $i;  
                    mkCatalogs ${catalogsPerLevel} ; 
                    cd ..;
                done
                cd ..;
            done
            cd ..;
        done
        cd ..; 
    done  
      
}

function copyData() {
    sourceDataFile=$1;
    echo "sourceDataFile: ${sourceDataFile}";
    copiesPerDir=$2
    echo "copiesPerDir: ${copiesPerDir}";

    fname=$(basename -- "${sourceDataFile}");
    dataFileBase="${fname%.*}";
    echo "dataFileBase: ${dataFileBase}";
    suffix="${fname##*.}";
    echo "suffix: ${suffix}";  
    
    
    theDirs=`find toplevel -type d`
    for aDir in ${theDirs}
    do
        for fileNum in `seq 1 ${copiesPerDir}`
        do
            fileName="${aDir}/${dataFileBase}_${fileNum}.${suffix}";
            echo "fileName: ${fileName}";
            cp ${sourceDataFile} ${fileName};
        done
    done
    
     
}

function makeRandomDatasetRequest() {

    numData=`cat ${datasetsFile} | wc -l `; 
    echo "numData=${numData}";

    dataRanScale=`echo "scale=10;x=${numData}/${maxRandom};print x" | bc`;
    echo "dataRanScale=${dataRanScale}";
    
    rValue=$RANDOM;
    echo "rValue=${rValue}";
    randomDataIndex=`echo "x=${rValue}*${dataRanScale};print x" | bc | awk '{printf("%d",$0);}' -`;
    echo "randomDataIndex=${randomDataIndex}";
    randomDataURL=`sed "${randomDataIndex}q;d" ${datasetsFile}`;
    echo "randomDataURL=${randomDataURL}";
    
    # Get it...
    status=`curl -s -o /dev/null -w "%{http_code}" --url "${randomDataURL}"`
    if [ ${status} -ne 200 ] 
    then
        echo "Curl request FAILED. Status: ${status} URL: ${randomDataURL}"
    fi
      
}



function makeRandomCatRequest() {

    numCat=` cat ${catalogsFile} | wc -l`; 
    #echo "numCat=${numCat}";

    catRanScale=`echo "scale=10;x=${numCat}/${maxRandom};print x" | bc`;
    #echo "catRanScale=${catRanScale}";
    
    rValue=$RANDOM;
    #echo "rValue=${rValue}";
    randomCatIndex=`echo "x=${rValue}*${catRanScale};print x" | bc | awk '{printf("%d",$0);}' -`;
    #echo "randomCatIndex=${randomCatIndex}";
    randomCatURL=`sed "${randomCatIndex}q;d" ${catalogsFile}`;
    # echo "randomCatURL=${randomCatURL}";
    
    # Get it...
    status=`curl -s -o /dev/null -w "%{http_code}" --url "${randomCatURL}"`
    if [ ${status} -ne 200 ] 
    then
        echo "Curl request FAILED. Status: ${status} URL: ${randomCatURL}"
    fi
      
}


function makeRandomRequests {
    for i in {1..30000}
    do
        makeRandomCatRequest
    done
}


function siteMapGrinder {
    
    for i in {1..30000}
    do
        retrieveSitemap
    done
}


#############################################################################
#############################################################################
#############################################################################

# buildTestTreeDirs 10;

# copyData /Users/ndp/OPeNDAP/hyrax/build/share/hyrax/data/nc/fnoc1.nc 5

# retrieveSitemap

# makeRandomRequests

# siteMapGrinder





