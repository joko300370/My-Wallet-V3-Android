package info.blockchain.wallet.payload.data;

import com.blockchain.serialization.JsonSerializableAccount;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.moshi.Json;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.params.MainNetParams;
import java.io.IOException;

import info.blockchain.wallet.BlockchainFramework;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
        getterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE)
public class ImportedAddress implements JsonSerializableAccount {

    public static final int NORMAL_ADDRESS = 0;
    public static final int ARCHIVED_ADDRESS = 2;

    @Json(name = "addr")
    @JsonProperty("addr")
    private String address;

    @Json(name = "priv")
    @JsonProperty("priv")
    private String privateKey;

    @Json(name = "label")
    @JsonProperty("label")
    private String label;

    @Json(name = "created_time")
    @JsonProperty("created_time")
    private long createdTime;

    @Json(name = "tag")
    @JsonProperty("tag")
    private int tag;

    @Json(name = "created_device_name")
    @JsonProperty("created_device_name")
    private String createdDeviceName;

    @Json(name = "created_device_version")
    @JsonProperty("created_device_version")
    private String createdDeviceVersion;

    public String getAddress() {
        return address;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public boolean isPrivateKeyEncrypted() {
        try {
            Base58.decode(privateKey);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public String getLabel() {
        if(label != null)
            return label;
        else
            return address;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public int getTag() {
        return tag;
    }

    public String getCreatedDeviceName() {
        return createdDeviceName;
    }

    public String getCreatedDeviceVersion() {
        return createdDeviceVersion;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPrivateKeyFromBytes(byte[] privKeyBytes) {
        this.privateKey = Base58.encode(privKeyBytes);
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public void setCreatedDeviceName(String createdDeviceName) {
        this.createdDeviceName = createdDeviceName;
    }

    public void setCreatedDeviceVersion(String createdDeviceVersion) {
        this.createdDeviceVersion = createdDeviceVersion;
    }

    public static ImportedAddress fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, ImportedAddress.class);
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public static ImportedAddress fromECKey(ECKey ecKey) {

        ImportedAddress importedAddress = new ImportedAddress();
        importedAddress.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());
        importedAddress.setLabel("");

        importedAddress.setAddress(
            LegacyAddress.fromKey(
                MainNetParams.get(),
                ecKey
            ).toBase58()
        );
        importedAddress.setCreatedDeviceName(BlockchainFramework.getDevice());
        importedAddress.setCreatedTime(System.currentTimeMillis());
        importedAddress.setCreatedDeviceVersion(BlockchainFramework.getAppVersion());

        return importedAddress;
    }

    public XPubs xpubs() {
        return new XPubs(new XPub(address, XPub.Format.LEGACY));
    }
}
