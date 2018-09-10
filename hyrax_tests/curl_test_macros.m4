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

dnl Given a filename, remove any date-time string of the form "yyyy-mm-dd hh:mm:ss" 
dnl in that file and put "removed date-time" in its place. This hack keeps the baselines
dnl more or less true to form without the obvious issue of baselines being broken 
dnl one second after they are written.
dnl  
dnl Note that the macro depends on the baseline being a file.
dnl
dnl jhrg 6/3/16
 
m4_define([REMOVE_DATE_TIME], [dnl
    sed 's@[[0-9]]\{4\}-[[0-9]]\{2\}-[[0-9]]\{2\} [[0-9]]\{2\}:[[0-9]]\{2\}:[[0-9]]\{2\}\( GMT\)*\( Hyrax-[[-0-9a-zA-Z.]]*\)*@removed date-time@g' < $1 > $1.sed
    dnl ' Added the preceding quote to quiet the Eclipse syntax checker. jhrg 3.2.18
    mv $1.sed $1
])
m4_define([REMOVE_DATE_HEADER], [dnl
    sed 's/^Date:.*$/Date: REMOVED/g' < $1 > $1.sed
    cp $1.sed $1
])

dnl The above macro modified to edit the '<h3>OPeNDAP Hyrax (Not.A.Release)' issue
dnl so that whatever appears in the parens is moot.

m4_define([PATCH_HYRAX_RELEASE], [dnl
    sed 's@OPeNDAP Hyrax (.*))\(.*\)@OPeNDAP Hyrax (Not.A.Release)\1@g' < $1 > $1.sed
    mv $1.sed $1
])

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
	PATCH_HYRAX_RELEASE([$baseline.tmp])
        ],
        [
        AT_CHECK([curl -K $input], [0], [stdout])
	PATCH_HYRAX_RELEASE([stdout])
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
        REMOVE_DATE_HEADER([http_header])
        AT_CHECK([mv stdout $baseline.tmp])
        AT_CHECK([echo "^\c" > $baseline.http_header.tmp; head -1 http_header | sed "s/\./\\\./g" >> $baseline.http_header.tmp])
        ],
        [
        AT_CHECK([curl -D http_header -K $input], [0], [stdout])
        REMOVE_DATE_HEADER([http_header])
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
#######################################################################################
#######################################################################################
#######################################################################################
#######################################################################################
#
#   OLD (UNUSED) BESSTANDALONE TESTS
#     (Keeping them in as templates for yet to be written curl tests)
#


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
m4_define([AT_BESCMD_NETCDF_RESPONSE_TEST],
[_AT_BESCMD_NETCDF_TEST([$abs_srcdir/$1], [$abs_srcdir/$1.baseline], [$2])])
#######################################################################################
