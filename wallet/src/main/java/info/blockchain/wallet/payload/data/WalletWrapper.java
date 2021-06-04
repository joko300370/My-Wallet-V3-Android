package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import org.json.JSONException;

import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
public class WalletWrapper {

    public static final int V4 = 4;
    public static final int V3 = 3;
    public static final int SUPPORTED_VERSION = V4;

    public static final int DEFAULT_PBKDF2_ITERATIONS_V2 = 5000;

    @JsonProperty("version")
    private int version;

    @JsonProperty("pbkdf2_iterations")
    private int pbkdf2_iterations;

    @JsonProperty("payload")
    private String payload;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getPbkdf2Iterations() {
        return pbkdf2_iterations;
    }

    public void setPbkdf2Iterations(int pbkdf2_iterations) {
        this.pbkdf2_iterations = pbkdf2_iterations;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public static WalletWrapper fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, WalletWrapper.class);
    }

    public String toJson(ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    private void validateVersion() throws UnsupportedVersionException {
        if (getVersion() > SUPPORTED_VERSION) {
            throw new UnsupportedVersionException(getVersion() + "");
        }
    }

    /**
     * Set iterations to default if need
     */
    private void validatePbkdf2Iterations() {
        if (pbkdf2_iterations <= 0) {
            pbkdf2_iterations = DEFAULT_PBKDF2_ITERATIONS_V2;
        }
    }

    public Wallet decryptPayload(String password)
        throws UnsupportedVersionException,
            IOException,
            DecryptionException,
            HDWalletException {

        validateVersion();
        validatePbkdf2Iterations();

        String decryptedPayload;
        try {
            decryptedPayload = AESUtil.decrypt(getPayload(), password, getPbkdf2Iterations());
        } catch (Exception e) {
            throw new DecryptionException(e);
        }

        if (decryptedPayload == null) {
            throw new DecryptionException("Decryption failed.");
        }

        try {
            ObjectMapper mapper = getMapperForVersion(getVersion());

            Wallet wallet = Wallet.fromJson(decryptedPayload, mapper);
            wallet.setWrapperVersion(getVersion());
            return wallet;
        } catch (JSONException e) {
            throw new DecryptionException("Decryption failed.");
        }
    }

    public static ObjectMapper getMapperForVersion(int version) {
        ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(
            mapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
        );

        KotlinModule module = new KotlinModule();
        if (version == V4) {
            module.addAbstractTypeMapping(Account.class, AccountV4.class);
        } else {
            module.addAbstractTypeMapping(Account.class, AccountV3.class);
        }

        mapper.registerModule(module);

        return mapper;
    }

    public static WalletWrapper wrap(String encryptedPayload, int version, int iterations) {
        WalletWrapper walletWrapperBody = new WalletWrapper();
        walletWrapperBody.setVersion(version);
        walletWrapperBody.setPbkdf2Iterations(iterations);
        walletWrapperBody.setPayload(encryptedPayload);
        return walletWrapperBody;
    }
}
