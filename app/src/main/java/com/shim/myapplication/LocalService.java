package com.shim.myapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class LocalService extends Service {
    private final String TAG = "DEBUG_" + this.getClass().getSimpleName();

    private final int CMD_INIT = 0;
    private final int CMD_RUN_LOOP = 1;
    private final int CMD_BUY_NOW = 2;
    private final int CMD_SELL_NOW = 3;
    private final int CMD_GET_ORDERBOOK = 4;

    private MyTrade mMyTrade;
    private Handler mMainHandler;
    private Handler mThreadHandler;
    private boolean mIsStarted;
    private int mReqInterval = 5;
    private Float mSellLimit = 2F;
    private int mSellLimitIndex = 0;
    private Float mSellLimitForMaxPrice = 2F;
    private int mSellLimitForMaxPriceIndex = 0;
    private static Float[] sSellLimitArray = {2F, 3F, 5F, 10F};

    private boolean mSellWithoutConfirm;

    private INotifyOrderbook mOrderbookResult;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(100, mBuilder.build());

        startForeground(100, mBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() action : " + intent.getAction() + " startId : " + startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        Toast.makeText(this, "Local service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind() action : " + intent.getAction());
        return mBinder;
    }

    public void setMainHandler(Handler handler) {
        mMainHandler = handler;
    }

    public int setReqInterval() {
        if (mReqInterval == 5) {
            mReqInterval = 1;
        } else {
            mReqInterval++;
        }
        return mReqInterval;
    }

    public Float setSellLimit() {
        if (mMyTrade != null) {
            mSellLimit = sSellLimitArray[mSellLimitIndex];
            mSellLimitIndex++;

            if (mSellLimitIndex == sSellLimitArray.length) {
                mSellLimitIndex = 0;
            }

            mMyTrade.setSellRatio(mSellLimit);
        }
        return mSellLimit;
    }

    public Float setSellLimitForMaxPrice() {
        if (mMyTrade != null) {
            mSellLimitForMaxPrice = sSellLimitArray[mSellLimitForMaxPriceIndex];
            mSellLimitForMaxPriceIndex++;

            if (mSellLimitForMaxPriceIndex == sSellLimitArray.length) {
                mSellLimitForMaxPriceIndex = 0;
            }

            mMyTrade.setSellRatioForMaxPrice(mSellLimitForMaxPrice);
        }
        return mSellLimitForMaxPrice;
    }

    public boolean getSellWithOutConfirm() {
        Log.i(TAG, "getSellWithOutConfirm() : " + mSellWithoutConfirm);
        return mSellWithoutConfirm;
    }

    public void setSellWithOutConfirm(boolean flag) {
        Log.i(TAG, "setSellWithOutConfirm() : " + flag);
        mSellWithoutConfirm = flag;
    }

    public Float restartLoop() {
        Float stopLossRatio = 50F;

        mMyTrade.setSellRatio(stopLossRatio);
        mThreadHandler.sendEmptyMessageDelayed(CMD_RUN_LOOP, 1000L);

        return stopLossRatio;
    }

    public void start(String currency) {
        if (!mIsStarted) {
            Log.i(TAG, "Thread start...");
            HandlerThread mThread = new HandlerThread("MyLoop");
            mThread.start();
            mThreadHandler = new Handler(mThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case CMD_INIT:
                            mMyTrade.initialize();
                            mThreadHandler.sendEmptyMessageDelayed(CMD_RUN_LOOP, 1000L);
                            break;
                        case CMD_RUN_LOOP:
                            Price result = mMyTrade.runLoop();
                            if (result == null) {
                                printLog("Stop trading");
                            } else if (result.currentPrice < 0) {
                                printLog("Sold");

                            } else if (result.isSellNow()) {
                                printLog("Time to sell");
                                if (mSellWithoutConfirm) {
                                    mMyTrade.sellNow(0);
                                } else {
                                    Message message = Message.obtain(mMainHandler, 3);
                                    message.sendToTarget();
                                }
                            } else {
                                printLog("\nbalance : " + mMyTrade.getTotalBalance() +
                                        "\nbuying : " + result.buyingPrice +
                                        "\ncurrent : " + result.currentPrice +
                                        " max : " + result.maxPrice +
                                        "\ndiff : " + result.getDiff() + " ratio : " + result.getRatio() + "%" +
                                        "\nresult : " + Math.floor(result.getDiff() * mMyTrade.getTotalBalance()));

                                mThreadHandler.sendEmptyMessageDelayed(CMD_RUN_LOOP, mReqInterval * 1000L);
                            }
                            break;
                        case CMD_BUY_NOW:
                            if (mMyTrade != null) {
                                String buyResult = mMyTrade.buyNow(0);
                                printLog("buy : " + buyResult);
                            }
                            break;
                        case CMD_SELL_NOW:
                            if (mMyTrade != null) {
                                float units = (float)msg.obj;
                                String sellResult = mMyTrade.sellNow(units);
                                printLog("sell : " + sellResult);

                                mThreadHandler.sendEmptyMessageDelayed(CMD_RUN_LOOP, 2000L);
                            }
                            break;
                        case CMD_GET_ORDERBOOK:
                            if (mMyTrade != null) {
                                String orderbook = mMyTrade.getOrderbook();
                                notifyOrderbookResult(orderbook);
                            }

                            mThreadHandler.sendEmptyMessageDelayed(CMD_GET_ORDERBOOK, 5000L);
                            break;
                        default:
                            break;
                    }
                }
            };
            mIsStarted = true;
            printLog("start HandlerThread : " + currency);

            String connectKey = getResources().getString(R.string.connect_key);
            String secretKey = getResources().getString(R.string.secret_key);
            Log.d(TAG, "connectKey: " + connectKey + ", secretKey: " + secretKey);

            mMyTrade = new MyTrade(currency, connectKey, secretKey);
            mThreadHandler.sendEmptyMessage(CMD_INIT);
        } else {
            Log.w(TAG, "Thread already started...");
        }
    }

    public void stop() {
        if (mIsStarted) {
            Log.i(TAG, "Thread stop...");
            mThreadHandler.getLooper().quitSafely();
            mIsStarted = false;
            printLog("stop HandlerThread");
        } else {
            Log.w(TAG, "Thread not started...");
        }
    }

    public void buyNow() {
        if (mThreadHandler != null) {
            mThreadHandler.sendEmptyMessage(CMD_BUY_NOW);
        }
    }

    public void sellNow(float unit) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(CMD_RUN_LOOP);

            Message msg = Message.obtain();
            msg.what = CMD_SELL_NOW;
            msg.obj = unit;
            mThreadHandler.sendMessage(msg);
        }
    }

    public void getOrderbook(INotifyOrderbook orderbookResult) {
        mOrderbookResult = orderbookResult;

        if (mThreadHandler != null) {
            mThreadHandler.sendEmptyMessage(CMD_GET_ORDERBOOK);
        }
    }

    public interface INotifyOrderbook {
        void onNotifyOrderbook(String buy, String sell);
    }

    private void notifyOrderbookResult(String orderbook) {
        StringBuilder sbBuy = new StringBuilder();
        StringBuilder sbSell = new StringBuilder();

        try {
            JSONObject json = new JSONObject(orderbook);
            String status = json.getString("status");
            if ("0000".equals(status)) {
                JSONObject data = json.getJSONObject("data");

                JSONArray bids = data.getJSONArray("bids");
                int length = bids.length();
                for (int i = 0; i < 5; i++) {
//                    Log.v(TAG, "bids " + i + " " + bids.getString(i));
                    sbBuy.append(bids.getJSONObject(i).getString("quantity") + " : " + bids.getJSONObject(i).getString("price"));
                    sbBuy.append("\n");
                }

                JSONArray asks = data.getJSONArray("asks");
                length = asks.length();
                for (int i = 0; i < 5; i++) {
//                    Log.v(TAG, "asks " + i + " " + asks.getString(i));
                    sbSell.append(asks.getJSONObject(i).getString("quantity") + " : " + asks.getJSONObject(i).getString("price"));
                    sbSell.append("\n");
                }
            } else {
            }
        } catch (Exception e) {
//            Log.d(TAG, "failed : " + e.getMessage());
            printLog("failed : " + e.getMessage());
        }

        mOrderbookResult.onNotifyOrderbook(sbBuy.toString(), sbSell.toString());
    }

    private void printLog(String msg) {
        Message message = Message.obtain(mMainHandler, 0, msg);
        message.sendToTarget();
    }
}
