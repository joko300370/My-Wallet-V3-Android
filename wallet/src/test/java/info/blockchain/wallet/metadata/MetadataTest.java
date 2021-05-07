package info.blockchain.wallet.metadata;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.FrameworkInterface;
import info.blockchain.wallet.MockInterceptor;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.payload.data.Derivation;
import info.blockchain.wallet.util.MetadataUtil;
import info.blockchain.wallet.util.RestClient;

import org.bitcoinj.crypto.DeterministicKey;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public final class MetadataTest {

    private MockInterceptor mockInterceptor;

    private final MetadataDerivation metadataDerivation = new MetadataDerivation();

    @Before
    public void setup() {

        mockInterceptor = new MockInterceptor();

        //Set environment
        BlockchainFramework.init(new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {

                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(mockInterceptor)//Mock responses
                        .addInterceptor(loggingInterceptor)//Extensive logging
                        .build();

                return RestClient.getRetrofitApiInstance(okHttpClient);
            }

            @Override
            public Environment getEnvironment() {
                return Environment.PRODUCTION;
            }

            @NotNull
            @Override
            public String getApiCode() {
                return "API_CODE";
            }

            @Override
            public String getDevice() {
                return null;
            }

            @Override
            public String getAppVersion() {
                return null;
            }
        });
    }

    private HDWallet getWallet() {
        return HDWalletFactory
            .restoreWallet(
                Language.US,
                "15e23aa73d25994f1921a1256f93f72c",
                "",
                1,
                Derivation.LEGACY_PURPOSE
            );
    }

    @Test
    public void testAddressDerivation() throws Exception {

        String address = "12sC9tqHzAhdoukhCbTnyx2MjYXNXBGHnF";

        mockInterceptor.setResponseString("{\"message\":\"Not Found\"}");
        mockInterceptor.setResponseCode(404);

        DeterministicKey metaDataHDNode = MetadataUtil.INSTANCE.deriveMetadataNode(
            getWallet().getMasterKey().toDeterministicKey()
        );

        boolean isEncrypted = false;
        Metadata metadata = Metadata.Companion.newInstance(metaDataHDNode, 2, isEncrypted, null, metadataDerivation);

        Assert.assertEquals(metadata.getAddress(), address);
    }
}