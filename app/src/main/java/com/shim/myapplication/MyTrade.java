package com.shim.myapplication;

import android.util.Log;

import com.shim.myapplication.bit.Api_Client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;

public class MyTrade {
    private final String TAG = "DEBUG_" + this.getClass().getSimpleName();

    private Api_Client mApi;

    private int mCount;

    private String mCurrency;
    private Float mSellRatio = 0.9F; // 10%
    private Float mSellRatioForMaxPrice = 0.98F;  // 2%
    private double mBuyingPrice;
    private Float mTotalBalance;
    private double mMaxPrice;

    public MyTrade(String currency, String connectKey, String secretKey) {
        mCurrency = currency;
        mApi = new Api_Client(connectKey, secretKey);
    }

    public String getCurrency() {
        return mCurrency;
    }

    public void setSellRatio(Float sellLimit) {
        mSellRatio = 1 - (sellLimit / 100F);
    }

    public void setSellRatioForMaxPrice(Float sellRatioForMaxPrice) {
        mSellRatioForMaxPrice = 1 - (sellRatioForMaxPrice / 100F);
    }

    public Float getTotalBalance() {
        return mTotalBalance;
    }

    public void initialize() {
        mTotalBalance = getMyBalance(mCurrency);
        Log.i(TAG, "mTotalBalance : " + mTotalBalance);
        mBuyingPrice = getMyBuyingPrice(mCurrency);
    }

    public Price runLoop() {
        mCount++;
        if (mCount > 10000) {
            Log.i(TAG, "Stop here");
            return null;
        }

        if (mTotalBalance < 0.0001) {
            Log.i(TAG, "Nothing to sell");
            return null;
        }

        Float currentPrice = getCurrentPrice(mCurrency);
        if (currentPrice < 0) {
            Log.i(TAG, "network error");
            return null;
        } else {
            if (currentPrice > mMaxPrice) {
                mMaxPrice = currentPrice;
            }

            if (currentPrice < mSellRatio * mBuyingPrice) {
                Log.i(TAG, "It's time to sell : " + currentPrice + " buying at : " + mBuyingPrice);
                Price p = new Price(currentPrice, 0, mBuyingPrice);
                p.setSellNow();
                return p;
            }

            Log.d(TAG, "mSellRatioForMaxPrice. " + mSellRatioForMaxPrice);
            if (currentPrice < mSellRatioForMaxPrice * mMaxPrice) {
                if (currentPrice > 1.02 * mBuyingPrice) {
                    Log.i(TAG, "Down 2% from max : " + mMaxPrice + " to : " + currentPrice);
                    Price p = new Price(currentPrice, 0, mBuyingPrice);
                    p.setSellNow();
                    return p;
                }
            }
        }

        return new Price(currentPrice, mMaxPrice, mBuyingPrice);
    }

    public String sellNow(float units) {
        String result;
        if (units <= 0) {
            result = sellNow(mCurrency, mTotalBalance);
        } else {
            result = sellNow(mCurrency, units);
        }

        mTotalBalance = getMyBalance(mCurrency);

        return result;
    }

    public String buyNow(float units) {
        final HashMap<String, String> rgParams = new HashMap<>();

//        float tradeUnit = Util.getTradingUnit(mCurrency);
        rgParams.put("units", Float.toString(units));
        rgParams.put("currency", mCurrency);

        try {
            String result = mApi.callApi("/trade/market_buy", rgParams);
            JSONObject json = new JSONObject(result);
            Log.v(TAG, "buy result : " + json);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                mTotalBalance = getMyBalance(mCurrency);
                return "buy OK";
            } else {
                return json.getString("message");
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return "Exception";
        }
    }

    public String buyWithPrice(float units, String price) {
        final HashMap<String, String> rgParams = new HashMap<>();

        rgParams.put("price", price);
        rgParams.put("units", Float.toString(units));
        rgParams.put("order_currency", mCurrency);
        rgParams.put("Payment_currency", "KRW");
        rgParams.put("type", "bid");

        try {
            String result = mApi.callApi("/trade/place", rgParams);
            JSONObject json = new JSONObject(result);
            Log.v(TAG, "buy result : " + json);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                mTotalBalance = getMyBalance(mCurrency);
                return "buy OK";
            } else {
                return json.getString("message");
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return "Exception";
        }
    }

    public String getAccountBalance() {
        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("currency", mCurrency);

        try {
            String result = mApi.callApi("/info/account", rgParams);

            JSONObject json = new JSONObject(result);
            Log.v(TAG, "getAccountBalance result : " + json);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");
                String balance = data.getString("balance");
                return balance;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "getAccountBalance Exception";
        }
    }

    private Float getMyBalance(String reqCurrency) {
        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/info/balance", rgParams);

            JSONObject json = new JSONObject(result);
            Log.v(TAG, "getMyBalance result : " + json);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");
                String total_currency = data.getString("total_" + reqCurrency.toLowerCase());
                String available_currency = data.getString("available_" + reqCurrency.toLowerCase());

                return Float.valueOf(available_currency);
            } else {
                return 0F;
            }
        } catch (Exception e) {
            return 0F;
        }
    }

    private int getMyBuyingPrice(String reqCurrency) {
        int count = 1;
        HashMap<String, String> rgParams = new HashMap<>();
        rgParams.put("offset", "0");
        rgParams.put("count", String.valueOf(count));
        rgParams.put("searchGb", "1");
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/info/user_transactions", rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                Log.v(TAG, "getMyBuyingPrice result : " + json);
                JSONArray data = json.getJSONArray("data");
                JSONObject first = data.getJSONObject(0);
                String buyingPrice = first.getString(reqCurrency.toLowerCase() + "1krw");

                return Integer.valueOf(buyingPrice);
            } else {
                return 0;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private String sellNow(final String reqCurrency, final float units) {
        final HashMap<String, String> rgParams = new HashMap<>();

        float floorUnits = (float)Math.floor(units * 10000) / 10000;
        String unitsParam = Float.toString(floorUnits);
        Log.v(TAG, "sell units : " + unitsParam);

        rgParams.put("units", unitsParam);
        rgParams.put("currency", reqCurrency);

        try {
            String result = mApi.callApi("/trade/market_sell", rgParams);
            JSONObject json = new JSONObject(result);
            Log.v(TAG, "sell result : " + json);

            return result;
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return "failed : " + e.getMessage();
        }
    }

    private Float getCurrentPrice(String reqCurrency) {
        HashMap<String, String> rgParams = new HashMap<>();

        try {
            String result = mApi.callApi("/public/ticker/" + reqCurrency, rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");
                String buy_price = data.getString("buy_price");
                Log.v(TAG, "getCurrentPrice result : " + buy_price);
                return Float.valueOf(buy_price);
            } else {
                return 0f;
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return -1f;
        }
    }

    public String getOrderbook() {
        HashMap<String, String> rgParams = new HashMap<>();

        rgParams.put("group_orders", "1");
        rgParams.put("count", "15");

        try {
            String result = mApi.callApi("/public/orderbook/" + mCurrency, rgParams);

            JSONObject json = new JSONObject(result);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");

                JSONArray bids = data.getJSONArray("bids");
                int length = bids.length();
//                for (int i = 0; i < length; i++) {
//                    Log.v(TAG, "bids " + i + " " + bids.getString(i));
//                }

                JSONArray asks = data.getJSONArray("asks");
                length = asks.length();
//                for (int i = 0; i < length; i++) {
//                    Log.v(TAG, "asks " + i + " " + asks.getString(i));
//                }

                return result;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.d(TAG, "failed : " + e.getMessage());
            return null;
        }
    }
}
