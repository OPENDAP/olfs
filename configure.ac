dnl -*- automake -*-
dnl Process this file with autoconf to produce a configure script.

AC_PREREQ(2.63)

AC_INIT(OLFS, 1.17.0, support@opendap.org)
AC_CONFIG_SRCDIR([configure.ac])

dnl This tells autoconf/make to use the directory 'conf' to hold
dnl m4 macro files and various scripts. 
AC_CONFIG_AUX_DIR([conf])
AC_CONFIG_MACRO_DIR([conf])

dnl Require automake 1.10. This can be a list of space separated options
dnl that are passed to every Makefile.am
AM_INIT_AUTOMAKE([1.10])

dnl Example of an --enable-<feature> switch. 
dnl dnl AC_ARG_ENABLE([developer],
dnl   [AS_HELP_STRING([--enable-developer],[developer mode])],
dnl   [AM_CONDITIONAL([BES_DEVELOPER], [true])
dnl    AM_CONDITIONAL([BUILD_HYRAX_DEBUG], [true])
dnl    dnl Only set CXXFLAGS if the caller does not have them set in their environment
dnl    AS_IF([test -z "${CXXFLAGS+set}"], [CXXFLAGS="-g3 -O0  -Wall -W -Wcast-align"])],
dnl   [AM_CONDITIONAL([BES_DEVELOPER], [false])
dnl    AM_CONDITIONAL([BUILD_HYRAX_DEBUG], [false])
dnl    AC_DEFINE([NDEBUG], [1], [Define this to suppress assert() statements.])])

dnl Example of a --with-<feature> switch.
dnl AC_ARG_WITH([dap-modules],
dnl   [AS_HELP_STRING([--without-dap-modules], [don't build the bundled dap modules (default is to build the modules)])],
dnl   [with_dap_modules=${withval} ],
dnl   [with_dap_modules=yes])

dnl Example automake/Makefile.am conditional
dnl AM_CONDITIONAL([DAP_MODULES], [test x$with_dap_modules = xyes])

dnl These are tests that only apply if the modules are being built too.
dnl AM_COND_IF([DAP_MODULES], [

AC_CONFIG_FILES([Makefile hyrax_tests/Makefile hyrax_tests/atlocal])

dnl Parameter two is used as the value of AUTOTEST_PATH when the 
dnl shell script built from the *.at file is run. That script will
dnl look for executables using $PATH and $AUTOTEST_PATH.
AC_CONFIG_TESTDIR(hyrax_tests, [$abs_top_builddir])

AC_OUTPUT

