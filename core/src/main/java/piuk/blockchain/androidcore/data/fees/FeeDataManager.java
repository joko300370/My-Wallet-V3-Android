package piuk.blockchain.androidcore.data.fees;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.data.rxjava.RxPinning;

public class FeeDataManager {

    private final RxPinning rxPinning;
    private final FeeApi feeApi;

    public FeeDataManager(FeeApi feeApi, RxBus rxBus) {
        this.feeApi = feeApi;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        return rxPinning.call(() -> feeApi.getBtcFeeOptions())
            .onErrorReturnItem(FeeOptions.Companion.defaultFee(CryptoCurrency.BTC))
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for Ethereum.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        return rxPinning.call(() -> feeApi.getEthFeeOptions())
                .onErrorReturnItem(FeeOptions.Companion.defaultFee(CryptoCurrency.ETHER))
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains both a "regular" and a "priority" fee
     * option for ERC20 tokens.
     * @param contractAddress the contract address for ERC20
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getErc20FeeOptions(String contractAddress) {
        return rxPinning.call(() -> feeApi.getErc20FeeOptions(contractAddress))
            .onErrorReturnItem(FeeOptions.Companion.defaultFee(CryptoCurrency.ETHER))
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a {@link FeeOptions} object which contains a "regular" fee
     * option, both listed in Satoshis/byte.
     *
     * @return An {@link Observable} wrapping a {@link FeeOptions} object
     */
    public Observable<FeeOptions> getBchFeeOptions() {
        return feeApi.getBchFeeOptions()
                .onErrorReturnItem(FeeOptions.Companion.defaultFee(CryptoCurrency.BCH));
    }

    /**
     * Returns a {@link FeeOptions} object for XLM fees.
     */
    public Observable<FeeOptions> getXlmFeeOptions() {
        return feeApi.getXlmFeeOptions()
                .onErrorReturnItem(FeeOptions.Companion.defaultFee(CryptoCurrency.XLM));
    }
}
