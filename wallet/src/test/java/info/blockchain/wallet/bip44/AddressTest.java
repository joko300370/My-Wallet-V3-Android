package info.blockchain.wallet.bip44;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AddressTest {

    String seed = "15e23aa73d25994f1921a1256f93f72c";
    String xpriv = "L1HazQbEpwQKWnE5gXNKsFtHy2ufmpScQ8zZAd14BpwRX5DJpsdq";
    DeterministicKey key;
    HDAddress address;

    @Before
    public void setup() {
        key = HDKeyDerivation.createMasterPrivateKey(seed.getBytes());
        address = new HDAddress(MainNetParams.get(), key, 0,44);
    }

    @Test
    public void getPubKey() {
        Assert.assertEquals(33,address.getPubKey().length);
    }

    @Test
    public void getPubKeyHash() {
        Assert.assertEquals(20,address.getPubKeyHash().length);
    }

    @Test
    public void getAddressString() {
        Assert.assertEquals("1NbtWC8uX9spHtFnhoLGc2haXyfGpMuBuf",address.getFormattedAddress());
    }

    @Test
    public void getPrivateKeyString() {
        Assert.assertEquals(xpriv, address.getPrivateKeyString());
    }

    @Test
    public void getPath() {
        Assert.assertEquals("M/0", address.getPath());
    }

    @Test
    public void getChildNum() {
        Assert.assertEquals(0, address.getChildNum());
    }
}