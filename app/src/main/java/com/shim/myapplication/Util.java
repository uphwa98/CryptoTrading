package com.shim.myapplication;

import android.util.Log;

public class Util {
    private static final String TAG = "DEBUG_" + Util.class.getSimpleName();

    // 2017-12-07 (BTC: 0.001 | ETH: 0.01 | DASH: 0.01 | LTC: 0.1 | ETC: 0.1 | XRP: 10 | BCH: 0.001 | XMR: 0.01 | ZEC: 0.001 | QTUM: 0.1 | BTG: 0.01)
    public static float getTradingUnit(String currency) {
        Log.d(TAG, "getTradingUnit. " + currency);
        switch (currency) {
            case "BTC":
            case "BCH":
            case "ZEC":
                return 0.001F;
            case "ETH":
            case "DASH":
            case "XMR":
            case "BTG":
                return 0.01F;
            case "LTC":
            case "ETC":
            case "QTUM":
                return 0.1F;
            case "XRP":
                return 10.0F;
        }
        Log.e(TAG, "unexpected currency: " + currency);
        return 0F;
    }
}
