package piuk.blockchain.android.ui.kyc.profile.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProfileModel(
    val firstName: String,
    val lastName: String,
    val countryCode: String,
    val stateCode: String?,
    val stateName: String?
) : Parcelable