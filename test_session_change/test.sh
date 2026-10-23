#!/bin/bash
hr0="##############################################################################################"
hr1="#---------------------------------------------------------------------------------------------"

edl_config=~/.edl_tokens
netrc=~/.netrc

HYRAX_SIT="https://opendap.sit.earthdata.nasa.gov/hyrax/"
HYRAX_UAT="https://opendap.uat.earthdata.nasa.gov/hyrax/"
HYRAX_PROD="https://opendap.earthdata.nasa.gov/hyrax/"
HYRAX_LOCAL="http://localhost:8080/hyrax/"
request="data/nc/fnoc1.nc.dds"

test_url="$HYRAX_SIT$request"

wait_for_session_expiration_seconds=15

req_id_key="a-api-request-uuid"

##############################################################################
#
function setup_edl_token_auth(){

    echo "$hr0" 
    
    echo "# Checking for $edl_config script..."
    if test -f "$edl_config"
    then
        echo "# Loading EDL configuration from $edl_config"
        source $edl_config
            
        edl_prod
        #edl_uat
        echo "# EDL tokens configured for $edl_env" >&2

    else
        echo "# The EDL configuration script $edl_config was not found."
        echo "# Checking for the edl_hdr environment variable..."
        if test -z "$edl_hdr"
        then
            echo "# No edl_hdr environment variable was available in the shell" >&2
            echo "" >&2
            echo "Please create the shell variable edl_hdr=\"Authorization: Bearer EDL_TOKEN_FOR_TARGET_ENV\"" >&2
            echo "" >&2
            exit 2
        else 
            echo "# Found edl_hdr environment variable."
        fi
    fi
    echo "# EDL token auth is ready."
    
}

##############################################################################
#
function setup_edl_login_auth(){

    echo "$hr0" 
    
    echo "# Checking $netrc for credentials..."
    if test -f $netrc 
    then
        echo "# Found ~/.netrc file."
        
        if test -n "$USE_EDL_SIT_CREDS"
        then
            echo "# Checking for SIT credentials..."
            grep "machine sit.urs.earthdata.nasa.gov" $netrc  > /dev/null
            status=$?
            if test $status -ne 0
            then
                echo "# WARNING - Failed to locate SIT EDL credentials in $netrc" >&2
            else
                echo "# Located SIT EDL credentials in $netrc"
            fi
        fi
        
        echo "# Checking for UAT credentials..."
        grep "machine uat.urs.earthdata.nasa.gov" $netrc > /dev/null
        status=$?
        if test $status -ne 0
        then
            echo "ERROR Failed to locate UAT EDL credentials in $netrc" >&2
            exit $status
        else
            echo "# Located UAT EDL credentials in $netrc"
        fi
        
        echo "# Checking for PROD credentials..."
        grep "machine urs.earthdata.nasa.gov" $netrc > /dev/null
        status=$?
        if test $status -ne 0
        then
            echo "ERROR Failed to locate PROD EDL credentials in $netrc" >&2
            exit $status
        else
            echo "# Located PROD EDL credentials in $netrc"
        fi
    else
        echo "ERROR Failed to locate file $netrc" >&2
        exit 2
    fi

    echo "# EDL login auth is ready."
  
}




##############################################################################
#
function curl_token(){
    local cookies="$1"
    local ua="$2"
    local req_id="$3"
    
    local request_id_hdr=""
    if test -n "$req_id"
    then
        request_id_hdr="-H $req_id_key: $req_id"
        echo "# request_id_hdr: $request_id_hdr" >&2
    fi
    
    #set -x
    curl -v -s \
        -H "User-Agent: $ua" \
        -c "${cookies}" \
        -b "${cookies}" \
        -H "${edl_hdr}" \
        -o response.tmp \
        "$test_url"
        
    #set +x
    #echo ""

}

##############################################################################
#
function run_token_test() {
    local cookies="$1"
    local ua="$2"
    local req_id="$3"
    
    echo $hr0
    echo "# run_token_test() - "$(date)
    echo "#"
    echo "#       test_url: $test_url"
    echo "#     cookie jar: $cookies"
    echo "#     user-agent: $ua"
    echo "#         req_id: $req_id"
    echo "#"
    echo ""
    
        
    curl_token "$cookies" "$ua" "$req_id" 2>&1 | grep  -e "GET " -e "[<>] HTTP/" -e "^< Location: " -e "JSESSIONID" -e "Host: "
    
    # Show the repsonse.
    cat response.tmp 
    rm -f response.tmp
    
    echo ""
    echo $hr1
    echo "# cookie jar ($cookies):"
    echo ""
    cat "$cookies" | grep "JSESSIONID"
    echo ""
    echo $hr0
    echo ""


}

##############################################################################
#
function curl_netrc(){
    local cookies="$1"
    echo "# cookies: $cookies" >&2
    
    local ua="$2"
    echo "# ua: $ua" >&2
    
    local req_id="$3"
    echo "# req_id: $req_id" >&2

    local request_id_hdr=""
    if test -n "$req_id"
    then
        request_id_hdr="-H $req_id_key: $req_id"
        echo "# request_id_hdr: $request_id_hdr" >&2
    fi
    

    set -x
    curl -v -s -n -L \
        -H "User-Agent: $ua" \
        -c "${cookies}" \
        -b "${cookies}" \
        -o response.tmp \
        "$test_url"
    set +x 

}

##############################################################################
#
function run_login_test() {
    local cookies="$1"
    local ua="$2"
    local req_id="$3"
    
    echo $hr0
    echo "# run_login_test() - "$(date)
    echo "#"
    echo "#         req_id: $req_id"
    echo "#     cookie jar: $cookies"
    echo "#     user-agent: $ua"
    echo "#       test_url: $test_url"
    echo "#"
    echo ""
    
    # Run the cURL command but filter the output so the "narrative" is easier to follow.
    curl_netrc "$cookies" "$ua" "$req_id" 2>&1 | grep  -e "$req_id_key" -e "GET " -e "[<>] HTTP/" -e "^< Location: " -e "JSESSIONID" -e "Host: "
    
    # Show the repsonse.
    cat response.tmp 
    rm -f response.tmp
        
    echo ""
    echo $hr1
    echo "# cookie jar ($cookies):"
    echo ""
    cat "$cookies" | grep "JSESSIONID"
    echo ""
    echo $hr0
    echo ""


}

##############################################################################
#
function token_tests(){

    echo $hr0
    echo "# Running token_tests"
    token_cookie_jar="cjar-token-tests"
    echo -n "" >  $token_cookie_jar 
    ls -l "$token_cookie_jar"
    echo ""
    
    run_token_test "$token_cookie_jar" "SecretAgentMan"
        
    run_token_test "$token_cookie_jar" "SecretAgentMan"
    
    echo $hr1
    echo "#"
    echo "# token_tests() - Sleeping for $wait_for_session_expiration_seconds seconds"
    
    sleep $wait_for_session_expiration_seconds # Assumes session expiration time is less than wait_for_session_expiration_seconds 

    echo "# token_tests() - Continuing..."
    echo "#"
    echo $hr1
    echo ""

    run_token_test "$token_cookie_jar" "SecretAgentMan"
    
    run_token_test "$token_cookie_jar" "SecretAgentMan"
    
    run_token_test "$token_cookie_jar" "GetSmart"

    echo ""

}


##############################################################################
#
function login_tests(){

    echo $hr0
    echo "# Running login_tests"
    login_cookie_jar="cjar-login-tests"
    echo -n "" >  $login_cookie_jar 
    ls -l "$login_cookie_jar"
    echo ""
    
    run_login_test "$login_cookie_jar" "SecretAgentMan" "LoginRequest-01"
    run_login_test "$login_cookie_jar" "SecretAgentMan" "LoginRequest-02"
    
    echo $hr1
    echo "#"
    echo "# login_tests() - Sleeping for $wait_for_session_expiration_seconds seconds"
    
    sleep $wait_for_session_expiration_seconds # Assumes session expiration time is less than wait_for_session_expiration_seconds 
    
    echo "# login_tests() - Continuing..."
    echo "#"
    echo $hr1
    echo ""

    run_login_test "$login_cookie_jar" "SecretAgentMan" "LoginRequest-03"

    run_login_test "$login_cookie_jar" "SecretAgentMan" "LoginRequest-04"
    
    run_login_test "$login_cookie_jar" "GetSmart" "LoginRequest-05"
    
    echo ""

}

##############################################################################
# MAIN

echo ""

echo "test_url: $test_url"

        echo "$test_url" | grep -e"\.uat\."  -e"\.sit\." -e "localhost" > /dev/null
        status=$?
        echo "status: $status"
        
        if test 0 -eq $(echo "$test_url" | grep -e"localhost|127.0.0.1" > /dev/null;)
        then
            echo "Using NGAP env PROD"
        elif  test  0 -eq $(echo "$test_url" | grep  -e "\.uat\." > /dev/null;)
        then
            echo "Using NGAP env UAT"
        elif  test  0 -eq $(echo "$test_url" | grep  -e "\.sit\." > /dev/null;)
        then
            echo "Using NGAP env SIT"
        elif  test  0 -eq $(echo "$test_url" | grep  -e"opendap\.earthdata\.nas.gov" > /dev/null;)
        then
            echo "Using NGAP env PROD"
        else
            echo "Using DEFAULT NGAP env PROD"
        fi
        

exit

setup_edl_login_auth
login_tests 2>&1 | tee login_tests.log 

setup_edl_token_auth
token_tests 2>&1 | tee token_tests.log


