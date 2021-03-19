#!/usr/bin/env bash
# fail if any commands fails
set -e
# debug log
set -x

apk_path=`echo $1/*.apk | cut -d ' ' -f1`

if [ $2 = true ]; then

	/usr/bin/jarsigner -sigfile "CERT" -sigalg SHA1withRSA -digestalg SHA1 -keystore $BITRISEIO_ANDROID_KEYSTORE_URL -storepass $BITRISEIO_ANDROID_KEYSTORE_PASSWORD -keypass $BITRISEIO_ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD $apk_path "$BITRISEIO_ANDROID_KEYSTORE_ALIAS"

	/usr/bin/jarsigner -verify -verbose -certs $apk_path

	tools_version=`ls /opt/android-sdk-linux/build-tools  | tail -n 1`

	apk_name=`ls -t $apk_path | tail -n 1`

	aligned_apk_path=/bitrise/deploy/aligned/$apk_name

	/opt/android-sdk-linux/build-tools/$tools_version/zipalign -f 4 $apk_path $aligned_apk_path

else

	cp $apk_path /bitrise/deploy/

fi

bitrise_apk_path=`echo /bitrise/deploy/*.apk | cut -d ' ' -f1`

envman add --key BITRISE_APK_PATH --value $bitrise_apk_path

envman add --key BITRISE_APK_PATH_LIST --value $bitrise_apk_path

envman add --key APPCENTER_DISTRIBUTE_FILE --value $bitrise_apk_path
