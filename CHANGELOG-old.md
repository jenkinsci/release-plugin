# Changelog

## New versions are on GitHub releases

## Version 2.10.2 (Nov 18, 2018)

-   [JENKINS-53994](https://issues.jenkins-ci.org/browse/JENKINS-53994) -
    Fix issue with visualization of parameters coming from plugins

## Version 2.10.1 (Mar 13, 2018)

-   No user-visible changes
-   Developer: Upgrade the plugin to the latest plugin POM

## Version 2.10 (Jan 22, 2018)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2018-01-22/)

## Version 2.9 (Dec 08, 2017)

-   [JENKINS-26895](https://issues.jenkins-ci.org/browse/JENKINS-26895) -
    Prevent exception when Dashboard View plugin is not installed
-   [PR \#27](https://github.com/jenkinsci/release-plugin/pull/27) -
    Update Maven Plugin dependency to 3.0, cleanup other dependencies

## Version 2.8 (June 27, 2017)

-   [PR \#25](https://github.com/jenkinsci/release-plugin/pull/25) -
    Enable release actions for [Job Generator
    Plugin](https://wiki.jenkins.io/display/JENKINS/Job+Generator+Plugin)

## Version 2.7 (Apr 08, 2017)

-   [JENKINS-40765](https://issues.jenkins-ci.org/browse/JENKINS-40765) -
    Add a new **release()** step for [Pipeline
    Plugin](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin)
-   [PR \#14](https://github.com/jenkinsci/release-plugin/pull/14) -
    Enable the plugin for [Ivy
    Plugin](https://wiki.jenkins.io/display/JENKINS/Ivy+Plugin)

## Version 2.6.1 (10/13/2016)

-   [JENKINS-20797](http://issues.jenkins-ci.org/browse/JENKINS-20797) -
    In addition to the fix in 2.6, schedule new release builds with the
    actual User Cause widely supported by plugins

## Version 2.6 (09/29/2016)

[Unknown User
(oleg\_nenashev)](https://wiki.jenkins.io/display/~oleg_nenashev) is a
temporary maintainer.

-   Core dependency has been updated from 1.481 to 1.609.3 LTS
    ([justification](https://github.com/jenkinsci/release-plugin/pull/17#r81082784))
-   [JENKINS-34996](http://issues.jenkins-ci.org/browse/JENKINS-34996)
    Fix the compatibility with Jenkins cores containing the
    [SECURITY-170](https://wiki.jenkins-ci.org/display/JENKINS/Plugins+affected+by+fix+for+SECURITY-170)
    fix (pull request \#17, thanks to [Antonio
    Muñiz](https://wiki.jenkins.io/display/~amuniz))
-   [JENKINS-11176](http://issues.jenkins-ci.org/browse/JENKINS-11176)
    "If the build is a release build" promotion criteria was broken due
    to the improper descriptor handling in the plugin (pull request
    \#15, thanks to [Allan Burdajewicz](https://github.com/Dohbedoh))
-   [JENKINS-28132](http://issues.jenkins-ci.org/browse/JENKINS-28132)
    "Release" permission is now implied by Jenkins Administer permission
    (pull request \#16, thanks to aprueller)
-   [JENKINS-20797](http://issues.jenkins-ci.org/browse/JENKINS-20797)
    Recent Releases Portlet should support extraction of users from the
    new "triggered by user" cause being used in Jenkins 1.427+ (pull
    request \#18)
-   [PR \#18](https://github.com/jenkinsci/release-plugin/pull/18)
    Recent Releases Portlet should not create new users for non-existent
    usernames when rendering the output page (pull request \#18)
-   Cleanup of minor issues discovered by FindBugs (pull request \#19)

## Version 2.5.4 (10/26/2015)

-   [JENKINS-31159](http://issues.jenkins-ci.org/browse/JENKINS-31159)
    Fix postMatrixBuildSteps (Pull request \#13, thanks to Fiouz)

## Version 2.5.3 (4/25/2015)

-   [JENKINS-27722](http://issues.jenkins-ci.org/browse/JENKINS-27722)
    upgrade to the release plugin has left the plugin broken (Pull
    request \#12, thanks to glenritchie)
-   Change so that now you can select Publisher and Builders in the
    (release) build steps (Pull request \#11, thanks to glenritchie)
-   Small translation fix (Pull request \#10, thanks to Batmat)
-   Add two custom view job filters "All Release Jobs" and "Release
    Jobs" (Pull request \#9, thanks to fritaly)
-   Define a new RELEASE permission (Pull request \#8, thanks to
    fritaly)
    -   **BEWARE:** **You need to adapt your permissions so that users
        still see the release button**
-   Set the description for the parent of a matrix build (Pull request
    \#7, thanks to fritaly)
-   Add ability to run steps before/after all matrix configurations

## Version 2.4.1 (9/27/2013)

-   Don't display release action in matrix configuration

## Version 2.4 (8/04/2013)

-   [JENKINS-5079](http://issues.jenkins-ci.org/browse/JENKINS-5079)
    Added matrix projects support

## Version 2.3 (9/20/2012)

-   [JENKINS-13422](http://issues.jenkins-ci.org/browse/JENKINS-13422)
    Added release button column
-   Use package.png instead of package.gif to have transparent icons
-   Fixed release link being shown when project was disabled

## Version 2.2 (9/13/2011)

-   Disabled auto-refresh when triggering a new release (thanks rseguy)
-   [JENKINS-9705](http://issues.jenkins-ci.org/browse/JENKINS-9705)
    Option to override regular build parameters during release

## Version 2.1 (3/13/2011)

-   [JENKINS-8816](http://issues.jenkins-ci.org/browse/JENKINS-8816)
    Wrapped each build steps list in a f:block which seems to correct
    the drag and drop behavior
-   [JENKINS-8829](http://issues.jenkins-ci.org/browse/JENKINS-8829)
    Create permalinks for the latest release and latest successful
    release builds
-   Added i8n for promotion support
-   Added German translations

## Version 2.0 (2/15/2011)

-   Migrated to Jenkins
-   If release build result is not at least unstable, then don't keep
    build forever.
-   Expand release version template using build variables as well as
    release parameters
-   Add support for the promoted build plugin to add a condition that
    the build must be a release build
-   Show all previous release parameters when scheduling a release build
-   Add post successful build steps and post failed build steps
-   Prefill release parameters with previous release builds parameters
    (supports text field, checkbox & select list (drop-down list) input
    types)

## Version 1.10 (7/21/2010)

-   Added new checkbox on job config page to allow the disabling of the
    automated marking of the build as keep forever
-   Fixed issue where if you had overlapping parameter names defined as
    release and build parameters, the default build parameter values
    were being used to resolve the release version template instead of
    the release parameter values.

## Version 1.9 (11/15/2009)

-   Fixed issue where release plugin would prevent Jenkins from starting
    if dashboard view plugin was not installed
    ([4845](https://issues.jenkins-ci.org/browse/JENKINS-4845))
-   Fixed issue where recent releases portlet would throw NullPointer if
    a build was active

## Version 1.8 (10/13/2009)

-   Added support for [Dashboard
    View](https://wiki.jenkins.io/display/JENKINS/Dashboard+View) plugin
    by adding Recent Releases portlet

## Version 1.7 (08/30/2009)

-    After sleeping on it, changed the implementation to use the release
     version template so that parameters types don't have to be aware of
     the release plugin in order to be used as a release version string.

## Version 1.6 (08/29/2009)

-   Added new Release String Parameter that, when configured as a
    release parameter, will be used as the release value and the plugin
    will then set description and tooltip.
    ([4022](https://issues.jenkins-ci.org/browse/JENKINS-4022))

## Version 1.5 (08/06/2009)

-   Changed form submission to use post instead of get. File upload
    parameters work now.

## Version 1.4 (05/16/2009)

-   Fixed regression issue introducing release parameters
    ([3690](https://issues.jenkins-ci.org/browse/JENKINS-3690))

## Version 1.3 (05/11/2009)

-   Fixed regression due to maven plugin change
    ([3628](https://issues.jenkins-ci.org/browse/JENKINS-3628))

## Version 1.2 (05/1/2009)

-   Added support for user supplied release parameters leveraging
    Jenkins' parameter capability
    ([3370](https://issues.jenkins-ci.org/browse/JENKINS-3370))

## Version 1.1 (03/26/2009)

-   Add permissions on triggering a release
-   Fixed issue where parameters were not being resolved
-   Captured release parameters as build parameters which can now be
    viewed via build parameters link

## Version 1.0 (02/10/2009)

-   Initial release 
