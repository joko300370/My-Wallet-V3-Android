package info.blockchain.api.addressmapping

import info.blockchain.api.addressmapping.data.AddressMapRequest
import info.blockchain.api.addressmapping.data.AddressMapResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AddressMappingApiInterface {
    // Request the resolution of an arbitrary name. May return 0, 1 or more results
    @POST("/resolve")
    @Headers("accept: application/json")
    fun resolveAssetAddress(
        @Body resolveQuery: AddressMapRequest
    ): Single<AddressMapResponse>
}
