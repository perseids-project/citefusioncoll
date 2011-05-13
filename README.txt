An implementation of the CITE Collections API for data stored in
a Google fusion table.

PREREQUISITES:
* an installed AppEngine java SDK
* an internet connection.  Obviously, this is only usable if you a
can connect to Google Fusion.

CONFIGURATION:
* Edit local.properties with the correct location of your AppEngine SDK
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


TBD/BUGS:
* need to implement full csv parser
* need to add groovydocs generation to build file
* need to decide how to deal with groovy bug handling HTTP parameters
named 'request'!
* need to implement the following requests:
- GetLast
- GetNext
- GetPrev
- GetCollectionSize
- perhaps GetValidIds?
