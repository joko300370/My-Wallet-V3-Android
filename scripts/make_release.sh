#!/bin/bash
#
#  make-release.sh
#  Blockchain
#
#  Compatibility
#  -------------
#  ‣ This script only runs on macOS using Bash 3.0+
#  ‣ Requires Xcode Command Line Tools.
#
#  What It Does
#  ------------
#  Automatically bumps the version code and version name based on user input and creates and tags
#  the release/X.X.X branch from develop

currentDir=$(dirname "$0")
changedDir=0
if [[ $currentDir != *"/scripts"* ]]; then
  changedDir=1
  pushd ..
fi

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
    printf '\e[1;31m%-6s\e[m\n' "Making a new build requires that you have a clean git working directory. Please commit your changes or stash them to continue."
    exit 1
fi

if ! [ -x "$(command -v agvtool)" ]; then
    printf '\e[1;31m%-6s\e[m\n' "You are missing the Xcode Command Line Tools. To install them, please run: xcode-select --install."
    exit 1
fi

git fetch --tags
latestTag=$(git describe --tags $(git rev-list --tags --max-count=1))

echo "Latest release tag is: $latestTag"

read -p "What would you like to increase? (M/m/P)" version_increase

if [ $version_increase != "M" ] && [ $version_increase != "P" ] && [ $version_increase != "m" ]; then
    echo "$version_increase is an invalid command. Please select one of (M - Major / m - minor / P - Patch)"
    exit 1
fi

echo ""
dependenciesFilePath="./buildSrc/src/main/java/Dependencies.kt"
currentVersionCode=$(awk '/const val versionCode = / {print $5}' $dependenciesFilePath)
echo "Current versionCode is: $currentVersionCode"

currentVersionName=$(awk '/const val versionName = / {print $5}' $dependenciesFilePath)
echo "Current versionName is: $currentVersionName"

updatedVersionCode=$((currentVersionCode + 1))

strippedVersion="${currentVersionName%\"}"
strippedVersion="${strippedVersion#\"}"

splitNamesM=$( echo "$strippedVersion" | cut -d "." -f 1)
splitNamesm=$( echo "$strippedVersion" | cut -d "." -f 2)
splitNamesP=$( echo "$strippedVersion" | cut -d "." -f 3)

echo ""
newVersionName=""
if [ $version_increase == "M" ]; then
  echo "> Increasing major version"
  increasedMajor=$((splitNamesM + 1))
  newVersionName="\"$increasedMajor.0.0\""
fi

if [ $version_increase == "m" ]; then
  echo "> Increasing minor version"
  increasedMinor=$((splitNamesm + 1))
  newVersionName="\"$splitNamesM.$increasedMinor.0\""
fi

if [ $version_increase == "P" ]; then
  echo "> Increasing patch version"
  increasedPatch=$((splitNamesP + 1))
  newVersionName="\"$splitNamesM.$splitNamesm.$increasedPatch\""
fi

echo ""
echo "############"
echo "Updating version code from: $currentVersionCode to: $updatedVersionCode"
echo "Updating version name from: $currentVersionName to: $newVersionName"
echo "############"
echo ""

read -p "Are you sure you want to continue? y/n" updateConfirmation

if [ $updateConfirmation == "y" ] || [ $updateConfirmation == "Y" ]; then
  strippedUpdatedVersionName="${newVersionName%\"}"
  strippedUpdatedVersionName="${strippedUpdatedVersionName#\"}"

  prod_tag="v$strippedUpdatedVersionName($updatedVersionCode)"

  if [ $(git tag -l "$prod_tag") ]; then
    printf '\n\e[1;31m%-6s\e[m\n' "The updated version already exists! Please check versions manually"
    exit 1
  fi

  git checkout develop > /dev/null 2>&1
  git pull > /dev/null 2>&1

  releaseBranch="release/$strippedUpdatedVersionName"

  git checkout -b "$releaseBranch"

  sed -i '' "s#$currentVersionCode#$updatedVersionCode#g" buildSrc/src/main/java/Dependencies.kt
  sed -i '' "s#$currentVersionName#$newVersionName#g" buildSrc/src/main/java/Dependencies.kt

  git add $dependenciesFilePath
  git commit -m "chore(release): AND-0000 version bump: $strippedUpdatedVersionName($updatedVersionCode)"

  git tag -a "$prod_tag" -m "$prod_tag"

  echo ""
  echo "Your available remotes are:"
  echo ""
  git remote -v

  read -p "Where would you like to push to?" selectedRemote

  git ls-remote "$selectedRemote" > /dev/null 2>&1
  if [ "$?" -ne 0 ]; then
    printf '\n\e[1;31m%-6s\e[m\n' "Unable to read from '$selectedRemote' - Please check the name is correct and re-run the script"
    exit 1;
  fi

  git push -u "$selectedRemote" "$releaseBranch"
  git push --tags

  echo "All done!"
fi

if [ $changedDir == 1 ]; then
    popd
fi

if [ $updateConfirmation != "y" ] && [ $updateConfirmation != "Y" ]; then
  printf '\n\e[1;31m%-6s\e[m\n' "Release update cancelled - Please re-run the script if you wish to make changes"
  exit 1
fi