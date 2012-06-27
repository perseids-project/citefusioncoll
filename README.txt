OVERVIEW:
An implementation of the CITE Collections API for data stored in 
one or more Google fusion table(s). 

Source code is layed out following gradle build conventions.

src/main/webapp is a groovlet (a servlet written in Groovy) that fields
CITE Collections requests.  It's a wrapper for the real work that is
done by the edu.holycross.shot.citecoll.CollectionService class.
For details of that class, see the groovydocs.

LICENSE:
All source code in this project is (c) 2011 Neel Smith and is made
available under the terms of the GNU General Public License, version 3.
See the included file gpl-3.0.txt for details.

PREREQUISITES:
* an internet connection.  Obviously, this servlet is only usable if you a
can connect to Google Fusion.

CONFIGURATION:
* Edit  src/main/webapp/configs/capabilities.xml with information about the Google Fusion
data set you want to serve.

* Optionally, you may also edit src/main/webapp/home to tailor the servlet's
home page to your project.


BUILDING AND RUNNING:
Use standard gradle tasks to build a war or api documentation, or to
run the war.  Run 'gradle tasks' to see options.

Note that while 'gradle jettyRun' does not correctly set up the
dependencies on other libraries to run properly, 'gradle jettyRunWar'
does work correctly, so you can test with that task.


TBD/BUGS:
* need to decide how to deal with groovy bug handling HTTP parameters
named 'request'!
