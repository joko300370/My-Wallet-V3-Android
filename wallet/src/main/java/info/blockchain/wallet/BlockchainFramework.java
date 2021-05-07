package info.blockchain.wallet;

import retrofit2.Retrofit;

/**
 * Class for initializing an instance of the HDWallet JAR
 *
 * TODO: Remove me and replace with Injection framework
 */
@Deprecated
public final class BlockchainFramework {

    private static FrameworkInterface blockchainInterface;

    public static void init(FrameworkInterface frameworkInterface) {
        blockchainInterface = frameworkInterface;
    }

    @Deprecated
    public static Retrofit getRetrofitApiInstance() {
        return blockchainInterface.getRetrofitApiInstance();
    }

    public static String getApiCode() {
        return blockchainInterface.getApiCode();
    }

    public static String getDevice() {
        return blockchainInterface.getDevice();
    }

    public static String getAppVersion() {
        return blockchainInterface.getAppVersion();
    }
}
