
AUTOMAKE_OPTIONS = foreign 

noinst_DATA = bes.conf

CLEANFILES = bes.conf

EXTRA_DIST = data bescmd $(HYRAX_TEST_SUITE).at $(HYRAX_TEST_SUITE) \
$(srcdir)/package.m4 atlocal.in bes.conf.in handler_tests_macros.m4

DISTCLEANFILES = atconfig

# Build the bes.conf used for testing so that the value substituted for
# @abs_top_srcdir@ does not contain '../'. This happens when using 
# configure's value for the parameter when running the distcheck target.
bes.conf: bes.conf.in $(top_srcdir)/configure.ac
	@clean_abs_top_srcdir=`echo ${abs_top_srcdir} | sed 's/\(.*\)\/\(.[^\/]*\)\/\.\./\1/g'`; \
	sed -e "s%[@]abs_top_srcdir[@]%$$clean_abs_top_srcdir%" \
		-e "s%[@]abs_top_builddir[@]%${abs_top_builddir}%" $< > bes.conf

############## Autotest follows #####################

AUTOM4TE = autom4te

HYRAX_TEST_SUITE = $(srcdir)/hyraxTest
HYRAX_TEST_SUITE_FLAGS =

$(HYRAX_TEST_SUITE): $(HYRAX_TEST_SUITE).at $(srcdir)/curl_test_macros.m4 $(srcdir)/package.m4

check-local: atconfig atlocal $(HYRAX_TEST_SUITE)
	@echo "Run tests with an empty cache"
	$(SHELL) '$(HYRAX_TEST_SUITE)' $(HYRAX_TEST_SUITE_FLAGS)
	-rm -rf dap_cache/*
	
clean-local:
	test ! -f '$(HYRAX_TEST_SUITE)' || $(SHELL) '$(HYRAX_TEST_SUITE)' --clean
	-rm -f $(HYRAX_TEST_SUITE) $(srcdir)/package.m4

AUTOTEST = $(AUTOM4TE) --language=autotest

% : %.at 
	$(AUTOTEST) -I '$(srcdir)' -o $@ $@.at

# The `:;' works around a Bash 3.2 bug when the output is not writeable.
$(srcdir)/package.m4: $(top_srcdir)/configure.ac
	:;{ \
	echo '# Signature of the current package.' && \
	echo 'm4_define([AT_PACKAGE_NAME],      [@PACKAGE_NAME@])' && \
	echo 'm4_define([AT_PACKAGE_TARNAME],   [@PACKAGE_TARNAME@])' && \
	echo 'm4_define([AT_PACKAGE_VERSION],   [@PACKAGE_VERSION@])' && \
	echo 'm4_define([AT_PACKAGE_STRING],    [@PACKAGE_STRING@])' && \
	echo 'm4_define([AT_PACKAGE_BUGREPORT], [@PACKAGE_BUGREPORT@])'; \
	} >'$(srcdir)/package.m4'
