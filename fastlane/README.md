fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android build
```
fastlane android build
```
Builds using the given environment and build type
### android test
```
fastlane android test
```
Runs all tests
### android alpha
```
fastlane android alpha
```
Submit a release Alpha build to the Play Store. This won't publish, just upload.
### android credentials
```
fastlane android credentials
```
Get the configuration files from the Android credentials repository.
### android ci_credentials
```
fastlane android ci_credentials
```
Get the configuration files from the Android credentials repository on CI.
### android upload_to_appcenter
```
fastlane android upload_to_appcenter
```
Upload to AppCenter.
### android ci_test
```
fastlane android ci_test
```
Tests to run on CI
### android ci_build
```
fastlane android ci_build
```
Build to run on CI
### android ci_lint
```
fastlane android ci_lint
```
Checks to run on CI

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
