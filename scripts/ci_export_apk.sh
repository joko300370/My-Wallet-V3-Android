#!/usr/bin/env bash
# fail if any commands fails
set -e
# debug log
set -x

apk_path=`echo $1/*.apk | cut -d ' ' -f1`

cp $apk_path /bitrise/deploy/

bitrise_apk_path=`echo /bitrise/deploy/*.apk | cut -d ' ' -f1`

envman add --key BITRISE_APK_PATH --value $bitrise_apk_path

envman add --key BITRISE_APK_PATH_LIST --value $bitrise_apk_path
