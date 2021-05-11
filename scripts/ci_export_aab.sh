#!/usr/bin/env bash
# fail if any commands fails
set -e
# debug log
set -x

aab_path=`echo $1/*.aab | cut -d ' ' -f1`

if [ $2 = true ]; then

	/usr/bin/jarsigner -sigfile "CERT" -sigalg SHA1withRSA -digestalg SHA1 -keystore $BITRISEIO_ANDROID_KEYSTORE_URL -storepass $BITRISEIO_ANDROID_KEYSTORE_PASSWORD -keypass $BITRISEIO_ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD $aab_path "$BITRISEIO_ANDROID_KEYSTORE_ALIAS"


	/usr/bin/jarsigner -verify -verbose -certs $aab_path

	tools_version=`ls /opt/android-sdk-linux/build-tools  | tail -n 1`

	aab_name=`ls -t $aab_path | tail -n 1`

	aligned_aab_path=/bitrise/deploy/aligned/$aab_name

	/opt/android-sdk-linux/build-tools/$tools_version/zipalign -f 4 $aab_path $aligned_aab_path

else

	cp $aab_path /bitrise/deploy/

fi

bitrise_aab_path=`echo /bitrise/deploy/*.aab | cut -d ' ' -f1`

envman add --key BITRISE_AAB_PATH --value $bitrise_aab_path

envman add --key BITRISE_AAB_PATH_LIST --value $bitrise_aab_path

envman add --key APPCENTER_DISTRIBUTE_FILE --value $bitrise_aab_path