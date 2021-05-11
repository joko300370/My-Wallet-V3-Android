package info.blockchain.wallet.api;

@SuppressWarnings("WeakerAccess")
@Deprecated // Pass params instead
public class PersistentUrls {

    // Env enum keys
    public static final String KEY_ENV_PROD = "env_prod";
    public static final String KEY_ENV_STAGING = "env_staging";
    public static final String KEY_ENV_DEV = "env_dev";

    // Production API Constants
    public static final String API_URL = "https://api.blockchain.info/";

    private PersistentUrls() {
        // Empty constructor
    }
}
