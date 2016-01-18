#
# These macros represent the best way I've found to incorporate building baselines
# into autotest testsuites. Until Hyrax/BES has a comprehensive way to make these
# kinds of tests - using a single set of macros from one source, copy this into
# the places it's needed and hack. If substantial changes are needed, try to copy
# them back into this file. jhrg 12/14/15 
#
# See below for the macros to use - do not use the macros that start with an 
# underscore.

AT_ARG_OPTION_ARG([baselines],
    [--baselines=yes|no   Build the baseline file for parser test 'arg'],
    [echo "baselines set to $at_arg_baselines";
     baselines=$at_arg_baselines],[baselines=])

# Usage: _AT_TEST_*(<bescmd source>, <baseline file>, <xpass/xfail> [default is xpass])

#######################################################################################
#
#   CURL TESTS


#--------------------------------------------------------------------------------------
#
# Basic test using diff  Response output should be text!!
#
m4_define([_AT_CURL_TEST], [dnl

    AT_SETUP([CURL $1])
    AT_KEYWORDS([curl])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([curl -K $input], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([curl -K $input], [0], [stdout])
        AT_CHECK([diff -b -B $baseline stdout], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])


#--------------------------------------------------------------------------------------
#
# DAP2 Data response test
#
m4_define([_AT_CURL_DAP2_DATA_TEST],  [dnl

    AT_SETUP([CURL $1])
    AT_KEYWORDS([curl])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([curl -K $input | getdap -Ms -], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([curl -K $input | getdap -Ms -], [0], [stdout])
        AT_CHECK([diff -b -B $baseline stdout], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])


#--------------------------------------------------------------------------------------
#
# DAP4 Data response test
#
m4_define([_AT_CURL_DAP4_DATA_TEST],  [dnl

    AT_SETUP([CURL $1])
    AT_KEYWORDS([curl])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([curl -K $input | getdap4 -D -M -s -], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([curl -K $input | getdap4  -D -M -s -], [0], [stdout])
        AT_CHECK([diff -b -B $baseline stdout], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])


#--------------------------------------------------------------------------------------
#
# ASCII Regex test
#
m4_define([_AT_CURL_PATTERN_TEST], [dnl

    AT_SETUP([CURL $1])
    AT_KEYWORDS([curl])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([curl -K $input], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([curl -K $input], [0], [stdout])
        AT_CHECK([grep -f $baseline stdout], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])



#--------------------------------------------------------------------------------------
#
# ASCII Compare PLUS Check HTTP Header using REGEX
# The http_header baseline MUST be edited to make a correct regular expression
#
m4_define([_AT_CURL_HEADER_AND_RESPONSE_TEST], [dnl

    AT_SETUP([CURL $1])
    AT_KEYWORDS([curl])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([curl -D http_header -K $input], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        AT_CHECK([echo "^\c" > $baseline.http_header.tmp; head -1 http_header | sed "s/\./\\\./g" >> $baseline.http_header.tmp])
        ],
        [
        AT_CHECK([curl -D http_header -K $input], [0], [stdout])
        AT_CHECK([diff -b -B $baseline stdout], [0], [ignore])
        AT_CHECK([grep -f $baseline.http_header http_header], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])




#######################################################################################
#
# Curl Testing Macro Definitions
#
m4_define([AT_CURL_RESPONSE_TEST],
[_AT_CURL_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])

m4_define([AT_CURL_DAP2_DATA_RESPONSE_TEST],
[_AT_CURL_DAP2_DATA_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])

m4_define([AT_CURL_DAP4_DATA_RESPONSE_TEST],
[_AT_CURL_DAP4_DATA_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])


m4_define([AT_CURL_RESPONSE_PATTERN_MATCH_TEST],
[_AT_CURL_PATTERN_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])


m4_define([AT_CURL_RESPONSE_AND_HTTP_HEADER_TEST],
[_AT_CURL_HEADER_AND_RESPONSE_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])






















#######################################################################################
#
#   OLD (UNUSED) BESSTANDALONE TESTS
#     (Keeping them in as templates for yet to be written curl tests)
#

m4_define([_AT_BESCMD_PATTERN_TEST], [dnl

    AT_SETUP([BESCMD $1])
    AT_KEYWORDS([bescmd])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input], [0], [stdout])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input], [0], [stdout])
        AT_CHECK([grep -f $baseline stdout], [0], [ignore])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])

m4_define([_AT_BESCMD_ERROR_TEST], [dnl

    AT_SETUP([BESCMD $1])
    AT_KEYWORDS([bescmd])

    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input], [ignore], [stdout], [ignore])
        AT_CHECK([mv stdout $baseline.tmp])
        ],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input], [ignore], [stdout], [ignore])
        AT_CHECK([diff -b -B $baseline stdout])
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])



dnl AT_CHECK (commands, [status = `0'], [stdout = `'], [stderr = `'], [run-if-fail], [run-if-pass])

dnl This is similar to the "binary data" macro above, but instead assumes the
dnl output of besstandalone is a netcdf3 file. The binary stream is read using
dnl ncdump and the output of that is compared to a baseline. Of couse, this
dnl requires ncdump be accessible.

m4_define([_AT_BESCMD_NETCDF_TEST],  [dnl

    AT_SETUP([BESCMD $1])
    AT_KEYWORDS([netcdf])
    
    input=$1
    baseline=$2

    AS_IF([test -n "$baselines" -a x$baselines = xyes],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input > test.nc])
        
        dnl first get the version number, then the header, then the data
        AT_CHECK([ncdump -k test.nc > $baseline.ver.tmp])
        AT_CHECK([ncdump -h test.nc > $baseline.header.tmp])
        AT_CHECK([ncdump test.nc > $baseline.data.tmp])
        ],
        [
        AT_CHECK([besstandalone -c $abs_builddir/bes.conf -i $input > test.nc])
        
        AT_CHECK([ncdump -k test.nc > tmp])
        AT_CHECK([diff -b -B $baseline.ver tmp])
        
        AT_CHECK([ncdump -h test.nc > tmp])
        AT_CHECK([diff -b -B $baseline.header tmp])
        
        AT_CHECK([ncdump test.nc > tmp])
        AT_CHECK([diff -b -B $baseline.data tmp])
        
        AT_XFAIL_IF([test "$3" = "xfail"])
        ])

    AT_CLEANUP
])

#######################################################################################
#
# Besstandalone Testing Macro Definitions
#
m4_define([AT_BESCMD_RESPONSE_TEST],
[_AT_BESCMD_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline])])

m4_define([AT_BESCMD_RESPONSE_PATTERN_TEST],
[_AT_BESCMD_PATTERN_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline])
])

m4_define([AT_BESCMD_ERROR_RESPONSE_TEST],
[_AT_BESCMD_ERROR_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline])
])

m4_define([AT_BESCMD_BINARYDATA_RESPONSE_TEST],
[_AT_BESCMD_BINARYDATA_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])

m4_define([AT_BESCMD_BINARY_DAP4_RESPONSE_TEST],
[_AT_BESCMD_DAP4_BINARYDATA_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])

m4_define([AT_BESCMD_NETCDF_RESPONSE_TEST],
[_AT_BESCMD_NETCDF_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])
#######################################################################################