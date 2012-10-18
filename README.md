# Checklist for release #

- Add README.txt to web app, with quick config and run info
- Add GPL to web app
- Fix embedding of GetCaps reply
- Add links to xslt for each request
- need to decide how to deal with groovy bug handling HTTP parameters
named 'request'!
- complete and review api docs
- Add ordering clause to GVR requests on ordered collections; default to ordering by URN for unordered collections
- Check datans declarations on all replies that include urn content
- abstract a single method for serialization of a CITE object
- add artifacts to upload war, source bundle, and api docs to nexus repository



# Overview
An implementation of the CITE Collections API for data stored in 
one or more Google fusion table(s). 

Source code is laid out following gradle build conventions.

`src/main/webapp` is a groovlet (a servlet written in Groovy) that fields
CITE Collections requests.  It's a wrapper for the real work that is
done by the `edu.holycross.shot.citecoll.CollectionService` class.
For details of that class, see the groovydocs (which you can build with `gradle groovydoc`).

# License
All source code in this project is (c) 2011-2012 Neel Smith and is made
available under the terms of the GNU General Public License, version 3.
See the included file `gpl-3.0.txt` for details.

# Prerequisites

- an internet connection.  Obviously, this servlet is only usable if you 
can connect to Google Fusion.

# Configuration #


- Edit  `src/main/webapp/configs/capabilities.xml` with information about the Google Fusion data set you want to serve.
- Optionally, you may also edit `src/main/webapp/home` to tailor the servlet's
home page to your project.

# Building and running
Use standard gradle tasks to build a war, to build api documentation, or to
run the war.  Run `gradle tasks` to see options.

Note that while `gradle jettyRun` does not correctly set up the
dependencies on other libraries to run properly with source files in place, `gradle jettyRunWar` creates a full war file and does work correctly, so you can experiment with that task.


