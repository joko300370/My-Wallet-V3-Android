package info.blockchain.wallet.ethereum;

final class EthUrls {


    private EthUrls() {
        throw new UnsupportedOperationException("You can't implement this class");
    }

    /* Base endpoint for all ETH operations */
    private static final String ETH = "eth";
    /* Base endpoint for v2 ETH operations */
    private static final String ETHV2 = "v2/eth";

    /* Additional paths for certain queries */
    static final String IS_CONTRACT = "/isContract";
    static final String BALANCE = "/balance";
    private static final String DATA = "/data";

    /* Complete paths */
    static final String ACCOUNT = ETH + "/account";
    static final String PUSH_TX = ETH + "/pushtx";
    static final String LATEST_BLOCK = ETHV2 + "/latestblock";
    static final String TX = ETH + "/tx";
    static final String FEES = ETH + "/fees";
    static final String V2_DATA = ETHV2 + DATA;
    static final String V2_DATA_ACCOUNT = ETHV2 + DATA + "/account";
    static final String V2_DATA_TRANSACTION = ETHV2 + DATA + "/transaction";
}
