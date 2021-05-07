package info.blockchain.wallet.util;

import org.bitcoinj.core.AddressFormatException;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FormatsUtilTest {

    @Test
    public void isEncrypted() {
        assertFalse(FormatsUtil.isKeyEncrypted("3tcnfpTzY6G6oL4NujXkXJfpkEJr69fDSRESuA76izac"));

        assertTrue(FormatsUtil.isKeyEncrypted("51jTHC6+phVaDTqZOyldKRRqrZQiXm/IhTMAjM/G9eCVQJt6POLTsKQT29RlFH9vH2tbJaowM5firNiSiNNIPw=="));
        assertTrue(FormatsUtil.isKeyEncrypted("QQBIDa4SO84Uow1AlWo/1STqO2n5OXN6seU2eULjK/4ydHYW/LRTmBQT3eyIgdYCnNtJ1QBSatZ/9d4oNbkH0pmPeZEd+4Sekz9zoqfJs35k0kt7R3De+L6cqYymLpQJLELZwlP78SmWnlC31pCAB/lklBXwlv9xcSRq9qO9sLk="));
    }

    @Test
    public void isUnencrypted() {
        assertFalse(FormatsUtil.isKeyUnencrypted("51jTHC6+phVaDTqZOyldKRRqrZQiXm/IhTMAjM/G9eCVQJt6POLTsKQT29RlFH9vH2tbJaowM5firNiSiNNIPw=="));
        assertFalse(FormatsUtil.isKeyUnencrypted("QQBIDa4SO84Uow1AlWo/1STqO2n5OXN6seU2eULjK/4ydHYW/LRTmBQT3eyIgdYCnNtJ1QBSatZ/9d4oNbkH0pmPeZEd+4Sekz9zoqfJs35k0kt7R3De+L6cqYymLpQJLELZwlP78SmWnlC31pCAB/lklBXwlv9xcSRq9qO9sLk="));
        assertTrue(FormatsUtil.isKeyUnencrypted("3tcnfpTzY6G6oL4NujXkXJfpkEJr69fDSRESuA76izac"));
    }

    @Test
    public void isValidBitcoinCashAddress() {
        assertFalse(FormatsUtil.isValidBCHAddress(""));
        assertFalse(FormatsUtil.isValidBCHAddress("test string"));
        assertFalse(FormatsUtil.isValidBCHAddress("https://www.google.co.uk"));
        // Standard BTC address
        assertFalse(FormatsUtil.isValidBCHAddress("19dPodLBKT4Fpym4PJ3UfkoMBDiTGkHw2V"));
        // BECH32 Segwit BTC address
        assertFalse(FormatsUtil.isValidBCHAddress("3MG8XBSphrQg8HLkz51Y6vJVgtXV1R8qS6"));
        // Valid BECH32 BCH address
        assertTrue(FormatsUtil.isValidBCHAddress("bitcoincash:qp02xpzz9qq0u7mtefw028mtlkszshxxdv0xsgv8pc"));
        // Valid BECH32 BCH address but with single digit changed
        assertFalse(FormatsUtil.isValidBCHAddress("bitcoincash:qp02xpzz9qq0u7mtefw028mtlkszshxxdv0xsgv8pd"));
        // Valid BECH32 BCH address but with single digit missing
        assertFalse(FormatsUtil.isValidBCHAddress("bitcoincash:qp02xpzz9qq0u7mtefw028mtlkszshxxdv0xsgv8p"));
        // Valid BECH32 BCH address - no prefix
        assertTrue(FormatsUtil.isValidBCHAddress("qp02xpzz9qq0u7mtefw028mtlkszshxxdv0xsgv8pc"));
    }

    @Test
    public void getBtcBitpayPaymentRequestUrl() {
        assertEquals("", FormatsUtil.getPaymentRequestUrl(""));
        assertEquals("", FormatsUtil.getPaymentRequestUrl("Satoshi Nakamoto"));
        assertEquals("", FormatsUtil.getPaymentRequestUrl("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu"));
        assertEquals("https://bitpay.com/i/AX146S9yK1ftUPmZGoNr9B", FormatsUtil.getPaymentRequestUrl("bitcoin:?r=https://bitpay.com/i/AX146S9yK1ftUPmZGoNr9B"));
    }

    @Test
    public void getBitcoinAmount() {
        assertEquals("0.0000", FormatsUtil.getBitcoinAmount(""));
        assertEquals("0.0000", FormatsUtil.getBitcoinAmount("Tobi Kadachi"));
        assertEquals("0.0000", FormatsUtil.getBitcoinAmount("bitcoin:?r=https://bitpay.com/i/AX146S9yK1ftUPmZGoNr9B"));
        assertEquals("0.0000", FormatsUtil.getBitcoinAmount("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu"));
        assertEquals("120000000", FormatsUtil.getBitcoinAmount("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2"));
        assertEquals("120000000", FormatsUtil.getBitcoinAmount("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2&message=Payment&label=Satoshi&extra=other-param"));
    }

    @Test
    public void isBitcoinUri() {
        assertFalse(FormatsUtil.isBitcoinUri(""));
        assertFalse(FormatsUtil.isBitcoinUri("Tobi Kadachi"));
        assertTrue(FormatsUtil.isBitcoinUri("bitcoin:?r=https://bitpay.com/i/AX146S9yK1ftUPmZGoNr9B"));
        assertTrue(FormatsUtil.isBitcoinUri("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu"));
        assertTrue(FormatsUtil.isBitcoinUri("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2"));
        assertTrue(FormatsUtil.isBitcoinUri("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu?amount=1.2&message=Payment&label=Satoshi&extra=other-param"));
    }

    @Test
    public void getLinkWithEmptyAmount() {
        final BigInteger BTC_TEST_AMOUNT = BigInteger.valueOf(12345);
        final BigInteger BTC_TEST_AMOUNT_EMPTY = BigInteger.ZERO;
        final String BTC_VALID_ADDRESS = "13ceUWo48GyCcSHwKpCKZtTkkJfr7jyxbT";
        final String BTC_VALID_AMOUNT = "0.00012345";
        final String BTC_VALID_URI_NO_AMOUNT = "bitcoin:" + BTC_VALID_ADDRESS;
        final String BTC_VALID_URI_WITH_AMOUNT = "bitcoin:" + BTC_VALID_ADDRESS + "?amount=" + BTC_VALID_AMOUNT;

        // Act
        assertEquals(
            FormatsUtil.toBtcUri(
                BTC_VALID_ADDRESS, BTC_TEST_AMOUNT_EMPTY),
                BTC_VALID_URI_NO_AMOUNT
        );

        assertEquals(
            FormatsUtil.toBtcUri(BTC_VALID_ADDRESS, BTC_TEST_AMOUNT),
            BTC_VALID_URI_WITH_AMOUNT
        );
    }

    @Test
    public void toShortCashAddressValid() {

        assertEquals("qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p",
                FormatsUtil.toShortCashAddress("bitcoincash:qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p"));

        assertEquals("qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p",
                FormatsUtil.toShortCashAddress("1BppmEwfuWCB3mbGqah2YuQZEZQGK3MfWc"));

        assertEquals("qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p",
                FormatsUtil.toShortCashAddress("qpmtetdtqpy5yhflnmmv8s35gkqfdnfdtywdqvue4p"));
    }

    @Test(expected = AddressFormatException.class)
    public void toShortCashAddressInvalid_1() {
        FormatsUtil.toShortCashAddress("bitcoincashqpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a");
    }

    @Test(expected = AddressFormatException.class)
    public void toShortCashAddressInvalid_2() {
        FormatsUtil.toShortCashAddress("bitcoin:12A1MyfXbW6RhdRAZEqofac5jCQQjwEPBu");
    }

    @Test(expected = AddressFormatException.class)
    public void toShortCashAddressInvalid_3() {
        FormatsUtil.toShortCashAddress("bitcoincash:");
    }

    @Test(expected = AddressFormatException.class)
    public void toShortCashAddressInvalid_4() {
        FormatsUtil.toShortCashAddress("");
    }
}