package info.blockchain.wallet.api;

import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FeeEndpoints {

    @Deprecated
    @GET("mempool/fees/btc")
    Observable<FeeOptions> getBtcFeeOptions();

    @Deprecated
    @GET("mempool/fees/eth")
    Observable<FeeOptions> getEthFeeOptions();

    @Deprecated
    @GET("mempool/fees/bch")
    Observable<FeeOptions> getBchFeeOptions();

    @GET("mempool/fees/{currency}")
    Observable<FeeOptions> getFeeOptions(@Path("currency") String currency);

    @GET("mempool/fees/eth")
    Observable<FeeOptions> getErc20FeeOptions(@Query("contractAddress") String contractAddress);
}
