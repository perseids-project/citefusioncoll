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
