#!/usr/bin/env bash
# fail if any commands fails
set -e
# debug log
set -x

aab_path=`echo $1/*.aab | cut -d ' ' -f1`

cp $aab_path /bitrise/deploy/

bitrise_aab_path=`echo /bitrise/deploy/*.aab | cut -d ' ' -f1`

envman add --key BITRISE_AAB_PATH --value $bitrise_aab_path

envman add --key BITRISE_AAB_PATH_LIST --value $bitrise_aab_path
