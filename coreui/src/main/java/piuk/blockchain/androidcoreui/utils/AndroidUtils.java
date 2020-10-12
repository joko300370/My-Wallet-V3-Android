package piuk.blockchain.androidcoreui.utils;

import android.os.Build;

public class AndroidUtils {

    public static boolean is25orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public static boolean is26orHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

}
