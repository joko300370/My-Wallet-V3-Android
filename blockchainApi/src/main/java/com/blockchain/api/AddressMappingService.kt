package com.blockchain.api

import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.addressmapping.data.AddressMapRequest
import io.reactivex.Single
import retrofit2.HttpException
import java.util.Locale

class DomainAddressNotFound : Exception()

class AddressMappingService(
    private val addressApi: AddressMappingApiInterface,
    apiCode: String
) {
    fun resolveAssetAddress(
        domainName: String,
        assetTicker: String
    ): Single<String> =
        addressApi.resolveAssetAddress(
            AddressMapRequest(
                domainName = domainName.toLowerCase(Locale.ROOT),
                assetTicker = assetTicker.toLowerCase(Locale.ROOT)
            )
        ).map {
            check(it.assetTicker.compareTo(assetTicker, true) == 0) { "Asset ticker mismatch" }
            it.address
        }.onErrorResumeNext {
            when {
                it is HttpException && it.code() == 404 -> Single.error(DomainAddressNotFound())
                else -> Single.error(ApiException(it.message))
            }
        }
}