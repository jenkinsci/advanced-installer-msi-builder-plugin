# ChangeLog

Changelog of Advanced Installer plugin for Jenkins.

## Advanced Installer Msi Builder Plugin  2.1

Features:

* Added **Enable PowerShell support** option. 
* The plugin now supports two execution senarios:
  * Deploy Advanced Installer tool
  * Deploy Advanced Installer tool and build project

__If you are referencing an Advanced Installer version prior to 17.7, in order to use *Enable PowerShell support* the build agent needs to run with elevated privileges. Versions 17.7 and later do not have this restriction.__

## Advanced Installer Msi Builder Plugin  2.0.3

Bugs:

* Fixed issue which prevented Advanced Installer to be deployed on nodes where JENKINS_HOME path contained spaces.
* Fixed issue which prevented Advanced Installer to be deployed when no License ID was specified.

## Advanced Installer Msi Builder Plugin  2.0.2

Bugs:

* Fixed crashing bug which prevented the deploy of Advanced Installer on slave nodes.

## Advanced Installer Msi Builder Plugin  2.0.1

Improvements:

* Added specific license registration support (requires [Advanced Installer 14.6](https://www.advancedinstaller.com/version-history.html) )

## Advanced Installer Msi Builder Plugin  2.0

Features:

* Advanced Installer is automatically deployed on the slave nodes.

## Advanced Installer Msi Builder Plugin  1.3.1

Bugs:

* AIP fails validation when it contains specials characters like &#x;

## Advanced Installer Msi Builder Plugin  1.3

Added support for additional edit commands.

## Advanced Installer Msi Builder Plugin  1.2

Added support for distributed build environments.

## Advanced Installer Msi Builder Plugin  1.1

Changed plugin name to: Advanced Installer Msi Builder Plugin

## Advanced Installer Msi Builder Plugin  1.0

First official release.
