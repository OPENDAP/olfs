#!/bin/bash
export H1="##############################################################"
export H2="--------------------------------------------------------------"
export H3="-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --"


#########################################################################
# loggy()
#
# A handy logging op.
#
function loggy(){
    if test -n "${log_file}"
    then
        echo  "$@"  | awk '{ print "# "$0;}' | tee -a "${log_file}" >&2
    else
        echo  "$@" | awk '{ print "# "$0;}'  >&2
    fi
}

#########################################################################
# timed_retrieval()
#
# Run a bescmd that asks for a dap4 data response.
# Returns the time, as ms, that the transmit_data operation took to 
# complete.
#
function timed_retrieval(){
    local target_url="$1"
    local target_file="$2"
 
    cookies="~/.edl_cookies"
    echo "" > "$bes_log"
    real_time=$( (time -p curl -n -c "${cookies}" -b "${cookies}" -L -s -o "$target_file" "$target_url" ; ) 2>&1 | grep real )
    loggy "time(): $real_time"
    
    # Look in the bes_log and use our friend grep to find all the transmit data lines, 
    # Format them as JSON using beslog2json.py, truning on the timing output, nad 
    cmd_timing=$(cat "$bes_log" | grep transmit_data | beslog2json.py -t t | \
            grep -E "transmit_data" | grep "get dap for d1" | jq '."elapsed-us"')
    cmd_timing=$(cat "$bes_log" | grep transmit_data | beslog2json.py -t t | \
                 grep "get dap for d1" | jq '."elapsed-us"')
    loggy "command_timing: $cmd_timing"
    echo "$cmd_timing"
}

function padme(){
    local value="$1"
    
    local retval=$value
    if test $value -lt 10;   then retval="0${retval}"; fi
    if test $value -lt 100;  then retval="0${retval}"; fi
    if test $value -lt 1000; then retval="0${retval}"; fi
    
    echo $retval
}


export prod_deployment="https://opendap.earthdata.nasa.gov/hyrax/"
export uat_deployment="https://opendap.uat.earthdata.nasa.gov/hyrax/"
export sit_deployment="https://opendap.sit.earthdata.nasa.gov/hyrax/"

export local_deployment="http://localhost:8080/opendap/"
export testoo_deployment="http://test.opendap.org:8080/opendap/"

export hyrax_endpoint="$testoo_deployment"

function fnoc1_test_config() {
    local dataset="data/nc/fnoc1.nc.dap"
    chksums_true_url="$hyrax_endpoint$dataset?dap4.checksum=true"
    chksums_false_url="$hyrax_endpoint$dataset?dap4.checksum=false"
    no_chksums_url="$hyrax_endpoint$dataset"
}

function coads_test_config() {
    local dataset="data/nc/coads_climatology.nc.dap"
    chksums_true_url="$hyrax_endpoint$dataset?dap4.checksum=true"
    chksums_false_url="$hyrax_endpoint$dataset?dap4.checksum=false"
    no_chksums_url="$hyrax_endpoint$dataset"
}

function daymet_test_config() {
    local dataset="daymet/daymet_v4_daily_na_tmax_2010.nc4"
    chksums_true_url="$hyrax_endpoint$dataset?dap4.checksum=true"
    chksums_false_url="$hyrax_endpoint$dataset?dap4.checksum=false"
    no_chksums_url="$hyrax_endpoint$dataset"
}

function ngap_local_daymet_test_config() {
    local dataset_url="http://localhost:8080/opendap/ngap/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_hi_tmax_2023.nc.dap?dap4.ce=/x;/y;/time"
    chksums_true_url="$dataset_url&dap4.checksum=true"
    chksums_false_url="$dataset_url&&dap4.checksum=false"
    no_chksums_url="$dataset_url"
}


function ngap_prod_daymet_test_config() {
    local dataset_url="https://opendap.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_hi_tmax_2023.nc.dap?dap4.ce=/x;/y;/time"
    chksums_true_url="$dataset_url&dap4.checksum=true"
    chksums_false_url="$dataset_url&dap4.checksum=false"
    no_chksums_url="$dataset_url"
}


function ngap_sit_daymet_test_config() {
    local dataset_url="https://opendap.sit.earthdata.nasa.gov/collections/C2532426483-ORNL_CLOUD/granules/Daymet_Daily_V4R1.daymet_v4_daily_hi_tmax_2023.nc.dap?dap4.ce=/x;/y;/time"
    chksums_true_url="$dataset_url&dap4.checksum=true"
    chksums_false_url="$dataset_url&dap4.checksum=false"
    no_chksums_url="$dataset_url"
}


#---------------------------------------------------------------------
# The Settings

loggy "$H1"

# BES Things
prefix=${prefix:-"/Users/ndp/OPeNDAP/hyrax/build"}
loggy "                prefix: $prefix"

bes_log="$prefix/var/bes.log"
loggy "               bes_log: $bes_log"

bes_conf="$prefix/etc/bes/bes.conf"
loggy "              bes_conf: $bes_conf"

results_dir="./results"
loggy "           results_dir: $results_dir"
mkdir -p "$results_dir"
loggy ""

export chksums_true_url
export chksums_false_url
export no_chksums_url


# fnoc1_test_config
#daymet_test_config
# coads_test_config
#ngap_local_daymet_test_config

# ngap_prod_daymet_test_config
ngap_sit_daymet_test_config 



loggy "   chksums_true_url: $chksums_true_url"
loggy "  chksums_false_url: $chksums_false_url"
loggy "     no_chksums_url: $no_chksums_url"


# - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
# Checksum True Files
#
chksums_true_file="checksums_true.csv"
loggy "     chksums_true_file: $chksums_true_file"

chksums_true_dap_file="checksums_true"
loggy " chksums_true_dap_file: $chksums_true_dap_file "

loggy ""

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
# Checksum False Files
#
chksums_false_file="checksums_false.csv"
loggy "    chksums_false_file: $chksums_false_file"

chksums_false_dap_file="checksums_false"
loggy "chksums_false_dap_file: $chksums_false_dap_file "

loggy ""

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
# No Checksum Files
#
no_chksums_file="no_checksums.csv"
loggy "       no_chksums_file: $no_chksums_file"

no_chksums_dap_file="no_checksums"
loggy "   no_chksums_dap_file: $no_chksums_dap_file "

loggy ""


#---------------------------------------------------------------------
#
# Reset output files and write header.
#
echo "rep, $chksums_true_file" > "$chksums_true_file"
echo "rep, $chksums_false_file" > "$chksums_false_file"
echo "rep, $no_chksums_file" > "$no_chksums_file"

rm -rv "${results_dir}"
mkdir -p "${results_dir}"

#---------------------------------------------------------------------
# Do The Reps
#
loggy "$H2"
for i in {1..1}
do
    loggy "Rep: $i"
    
    lapstr=$(padme $i)
    
    loggy "Request Checksums True"
    target_file="${results_dir}/${chksums_true_dap_file}_${lapstr}.dap"
    # target_file="/dev/null"
    loggy "target_file: $target_file"
    
    echo -n "$i, " >> "$chksums_true_file"
    time_in_ms=$(timed_retrieval "$chksums_true_url" "${target_file}" )
    echo "$time_in_ms" >> "$chksums_true_file"
    loggy $(ls -l "$target_file")
    loggy ""
        
    loggy "Request Checksums False"
    target_file="${results_dir}/${chksums_false_dap_file}_${lapstr}.dap"
    # target_file="/dev/null"
    loggy "target_file: $target_file"
    
    echo -n "$i, " >> "$chksums_false_file"
    time_in_ms=$(timed_retrieval "$chksums_false_url" "${target_file}" )
    echo "$time_in_ms" >> "$chksums_false_file"
    loggy $(ls -l "$target_file")
    loggy ""
    
    loggy "Request No Checksums"
    target_file="${results_dir}/${no_chksums_dap_file}_${lapstr}.dap"
    # target_file="/dev/null"
    loggy "target_file: $target_file"
    
    echo -n "$i, " >> "$no_chksums_file"
    time_in_ms=$(timed_retrieval "$no_chksums_url" "${target_file}" )
    echo "$time_in_ms" >> "$no_chksums_file"
    loggy $(ls -l "$target_file")
    loggy "$H3"
done



