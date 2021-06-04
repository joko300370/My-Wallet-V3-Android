package info.blockchain.wallet.util;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;

public class MetadataUtilTest {

    @Test
    public void testGetMessage() throws Exception {

        String message = "{\"hello\":\"world\"}";
        String expected1 = "eyJoZWxsbyI6IndvcmxkIn0=";
        String expected2 = "LxR+2CipfgdIdi4EZgNOKTT+96WbppXnPZZjdZJ2vwCTojlxqRTl6svwqNJRVM2jCcPBxy+7mRTUfGDzy2gViA==";

        byte[] result = MetadataUtil.INSTANCE.message(message.getBytes("utf-8"), null);

        Assert.assertEquals(expected1, new String(Base64.encode(result)));

        byte[] magic = MetadataUtil.INSTANCE.magic(message.getBytes(), null);
        byte[] nextResult = MetadataUtil.INSTANCE.message(message.getBytes(), magic);

        Assert.assertEquals(expected2, new String(Base64.encode(nextResult)));
    }

    @Test
    public void testMagic() throws Exception {

        String message = "{\"hello\":\"world\"}";
        String expected1 = "LxR+2CipfgdIdi4EZgNOKTT+96WbppXnPZZjdZJ2vwA=";
        String expected2 = "skkJOHg9L6/1OVztbUohjcvVR3cNdRDZ/OJOUdQI41M=";

        byte[] magic = MetadataUtil.INSTANCE.magic(message.getBytes(), null);
        Assert.assertEquals(expected1, new String(Base64.encode(magic)));

        byte[] nextMagic = MetadataUtil.INSTANCE.magic(message.getBytes(), magic);
        Assert.assertEquals(expected2, new String(Base64.encode(nextMagic)));
    }
}