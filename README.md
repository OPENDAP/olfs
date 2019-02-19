
# Hyrax/OLFS

Hyrax Version 1.15.3  (25 February 2019)    
OLFS  Version 1.18.3  (25 February 2019)    
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2008584.svg)](https://doi.org/10.5281/zenodo.2008584)

Which depends on:    
BES Version 3.20.2 [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2008575.svg)](https://doi.org/10.5281/zenodo.2008575)
libdap Version 3.20.2 [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.2002799.svg)](https://doi.org/10.5281/zenodo.2002799)

The file install.html or docs/index.html may have additional information.

### First:

Build and install bes and at minimum the netcdf_handler projects.

Launch the bes (you can use besctl to do that). 

Make sure there s a beslistener process running.

### Check it out:

    git clone https://github.com/OPENDAP/olfs.git


### Build it:

    ant server

(To make a distribution for release:  ant server -DHYRAX_VERSION=<num> -DOLFS_VERSION=<num> )

### Install it:

    rm -rf $CATALINA_HOME/webapps/opendap*
    cp build/dist/opendap.war $CATALINA_HOME/webapps

### Launch it:

    $CATALINA_HOME/bin/startup.sh

### Configure it:

By default the OLFS will utilize it's bundled default configuration in the directory
    $CATALINA_HOME/webapps/opendap/WEB-INF/conf

In order to configure your system so that your configuration changes are persistent 
you will need to do one of the following:

* For the user that will be running the OLFS (the Tomcat user), set
the environment variable OLFS_CONFIG_DIR to an existing directory to
which the Tomcat user has both read and write privileges.

OR

* Create the directory /etc/olfs and set it's permissions/ownership so
that the Tomcat user has both read and write permission.

If both of these steps are done then priority is given to the environment variable.

Restart Tomcat. When it starts the OLFS will check these locations and then install a copy of its default configuration into the new spot.

Edit the configuration files as needed.

If, for example, your beslistener is not running on localhost:10022
then you'll need to edit the olfs.xml file in the configuration
directory and adjust the <host> and <port> values to reflect your
situation.

### Relaunch it:

    $CATALINA_HOME/bin/shutdown.sh; $CATALINA_HOME/bin/startup.sh

For the configuration changes to take effect.

See http://docs.opendap.org/index.php/Hyrax for information about this software, Installation
instructions and NEWS.

### About the Aggregation servlet

In src/opendap/aggregation we have a servlet that performs aggregation for use,
initially, with NASA's EDSC (Earth Data Search Client). In that directory you
will find a README along with some help in testing the servlet using curl.
