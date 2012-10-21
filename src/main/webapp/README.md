# Notes towards a README for CITE Collections service implementation #

1. Copy gradle.properties-dist to gradle.properties.
2. In gradle.properties, enter a Google API key.  (Add notes here on how to get one if you don't already have one).
3. Add a capabilities file to the web/configs directory, with configurations for the Collections you want to serve.  (Add pointer here to docs on capabilities file.)
3. To test your configuration, you can run `gradle jettyRunWar` to run the servlet in a local jetty instance
4. Run `gradle war` to build a war file you can add to any servlet container,

## NB ##
The CITE Collection Service requests are submitted to the URL  `SERVICEINSTALLAION/api`.  This servlet supports two optional parameters to the `api` URL that are not part of the specification, and allow you to override default settings:

- `withXslt`:  use this to supply the name of an xslt file in the `xslt` directory.  To specify that you want a pure XML reply with no transformation, set `withXslt` = `none`.
- `config`:  use this to supply the name of a capabilities file in the `config` directory.  You can maintain multiple configuration files to present multiple virtual collection services.  The configuration file is read afresh with every request, so if you modify an existing file, or add a new configuration file, the new configuration is immediately available without restarting the servlet.  
