OVERVIEW:
An implementation of the CITE Collections API for data stored in 
one or more Google fusion table(s).

src/fusioncoll/api is a groovlet (a servlet written in Groovy) fields
CITE Collections requests.  It's a wrapper for the real work that is
done by the edu.holycross.shot.citecoll.CollectionService class.
For details of that class, see the groovydocs.

LICENSE:
All source code in this project is (c) 2011 Neel Smith and is made
available under the terms of the GNU General Public License, version 3.
See the included file gpl-3.0.txt for details.

PREREQUISITES:
* an internet connection.  Obviously, this is only usable if you a
can connect to Google Fusion.

CONFIGURATION:
* Edit  configs/capabilities.xml with information about the Google Fusion
data set you want to serve.

* Optionally, you may also edit src/fusioncoll/home to tailor the servlet's
home page to your project.


BUILDING AND RUNNING:
Run
	ant
to see a list of available targets.

The main targets you need are

    ant run-coll
to run the service in the included jetty container.

   ant war
to build a .war file you can drop into any servlet container.

   ant clean
to empty the build directory.

   ant doc
to generate groovy docs (javadoc-like API documentation
for groovy source of the  edu.holycross.shot.citecoll package).

TBD/BUGS:
* need to decide how to deal with groovy bug handling HTTP parameters
named 'request'!
