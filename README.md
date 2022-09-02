# Niord
The niord repository contains the common code-base for the NW + NM T&P editing
and publishing system.

From version 3.0.0 onwards, Niord has been ported and run using the Red Har
[Quarkus](https://quarkus.io/) framework.

## Development Setup

To get started with developing Niord you need to check out the developer guide 
at http://docs.niord.org/dev-guide/guide.html.

From version 3.0.0 onwards, the niord-parent project structured and individual
Maven modules that can be independently deployed through a Maven repositoty
such as [Nexus](https://www.sonatype.com/products/nexus-repository). This
means that a country specific implementation is enough to get a deployment
started.

## Country-specific Implementations

Country-specific implementations of the Niord system are easily created using a
web-application overlay project. Here additional code can be added and web 
resources (images, stylesheets, javascript files, etc) can be replaced with 
custom versions.

For an example, please refer to [niord-dk](https://github.com/NiordOrg/niord-dk)
- a DK implementation of Niord.

For the UK example, please refer to [niord-uk](https://github.com/gla-rad/niord-uk) 
- a UK implementation of Niord.

## Public API
A swagger definition of the public portion of the Rest API is published at 
https://niord.e-navigation.net/rest/swagger.json.

The swagger definition is generated from the jersey annotated methods in 
[ApiRestService.java](https://github.com/NiordOrg/niord/blob/master/niord-web/src/main/java/org/niord/web/api/ApiRestService.java) and 
[S124RestService.java](https://github.com/NiordOrg/niord/blob/master/niord-s124/src/main/java/org/niord/s124/S124RestService.java).

## Configuration

Sensitive or environment-specific settings should be placed in a "${niord.home}/niord.json" file. Example:

    [
      {
        "key"         : "baseUri",
        "description" : "The base application server URI",
        "value"       : "https://niord.mydomain.com",
        "web"         : false,
        "editable"    : true
      },
      {
        "key"         : "wmsLogin",
        "description" : "The WMS login",
        "value"       : "YOUR-SECRET-WMS-LOGIN",
        "web"         : false,
        "editable"    : true
      },
      {
        "key"         : "wmsPassword",
        "description" : "The WMS password",
        "value"       : "YOUR-SECRET-WMS-PASSWORD",
        "type"        : "Password",
        "web"         : false,
        "editable"    : true
      }
    ]

## Tips and Tricks

*IntelliJ set-up:*

Notice the following describes the setup for a previous version of Niord.

* First, check out and open the parent niord project in IntelliJ.
* In Run -> Edit configuration..., configure a Maven configuration to clean and build using: *[clean install]* in the top directory.
* Optionally you can also create a configuration to deploy your artefacts through: *[deploy]*.
* If working on a country-specific Niord implementation, e.g. [niord-uk](https://github.com/gla-rad/niord-uk/tree/niord-grad-quarkus), 
  import this maven project via the "Maven Projects" tab. Then you can run the country-specific *niord-web* module.
* To get rid of superfluous IntelliJ code editor warnings, disable the "Declaration access can be weaker" 
  and "Dangling Javadoc comment" inspections.

