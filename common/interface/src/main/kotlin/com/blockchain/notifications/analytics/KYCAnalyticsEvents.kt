package com.blockchain.notifications.analytics

import java.io.Serializable

sealed class KYCAnalyticsEvents(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object CountrySelected : KYCAnalyticsEvents("kyc_country_selected")
    class PersonalDetailsSet(data: String) : KYCAnalyticsEvents("kyc_personal_detail_set", mapOf("field_name" to data))
    object AddressScreenSeen : KYCAnalyticsEvents("kyc_address_detail_seen")
    object VerifyIdentityStart : KYCAnalyticsEvents("kyc_verify_id_start_button_click")
    object VeriffInfoSubmitted : KYCAnalyticsEvents("kyc_veriff_info_submitted")
    object VeriffInfoStarted : KYCAnalyticsEvents("kyc_veriff_started")
    object Tier1Clicked : KYCAnalyticsEvents("kyc_unlock_silver_click")
    object Tier2Clicked : KYCAnalyticsEvents("kyc_unlock_gold_click")
    object PhoneNumberUpdateButtonClicked : KYCAnalyticsEvents("kyc_phone_update_button_click")
    object AddressChanged : KYCAnalyticsEvents("kyc_address_detail_set")

    class EmailVeriffRequested(override val origin: LaunchOrigin) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.EMAIL_VERIFF_REQUESTED.eventName
        override val params: Map<String, Serializable>
            get() = emptyMap()
    }

    class UpgradeKycVeriffClicked(override val origin: LaunchOrigin) : AnalyticsEvent {
        override val event: String
            get() = AnalyticsNames.EMAIL_VERIFF_REQUESTED.eventName
        override val params: Map<String, Serializable>
            get() = emptyMap()
    }
}