package com.shim.myapplication;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity implements SellNowDialogFragment.NoticeDialogListener {
    private final String TAG = "DEBUG_" + this.getClass().getSimpleName();

    private Context mAppContext;
    private TextView mTextView;
    private TextView mTextViewBuy;
    private TextView mTextViewSell;
    private Button mButtonBTC;
    private Button mButtonBCH;
    private Button mButtonETH;
    private Button mButtonDASH;
    private Button mButtonETC;
    private Button mButtonLTC;
    private Button mButtonInterval;
    private Button mButtonSellLimit;
    private Button mButtonStart;
    private Button mButtonStop;
    private Button mSellNow;
    private Button mOrderbook;
    private CheckBox mNoPopupCheckBox;

    private String mCurrency = "BTC";

    private boolean mIsBoundByStart; // bound by START button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppContext = getApplicationContext();
        mTextView = findViewById(R.id.textView);
        mTextViewBuy = findViewById(R.id.textView2);
        mTextViewSell = findViewById(R.id.textView3);
        mButtonBTC = findViewById(R.id.button1);
        mButtonBTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "BTC";
            }
        });
        mButtonBCH = findViewById(R.id.button2);
        mButtonBCH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "BCH";
            }
        });
        mButtonETH = findViewById(R.id.button3);
        mButtonETH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "ETH";
            }
        });
        mButtonDASH = findViewById(R.id.button4);
        mButtonDASH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "DASH";
            }
        });
        mButtonETC = findViewById(R.id.button9);
        mButtonETC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "ETC";
            }
        });
        mButtonLTC = findViewById(R.id.button10);
        mButtonLTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrency = "LTC";
            }
        });
        mNoPopupCheckBox = findViewById(R.id.checkBox);
        mNoPopupCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.getId() == R.id.checkBox) {
                    if (isChecked) {
                        mBoundService.setSellWithOutConfirm(true);
                    } else {
                        mBoundService.setSellWithOutConfirm(false);
                    }
                }
            }
        });

        mButtonInterval = findViewById(R.id.button5);
        mButtonInterval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsBound) {
                    int interval = mBoundService.setReqInterval();
                    mButtonInterval.setText(String.valueOf(interval) + " s");
                }
            }
        });

        mButtonSellLimit = findViewById(R.id.button11);
        mButtonSellLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsBound) {
                    Float limit = mBoundService.setSellLimit();
                    mButtonSellLimit.setText("stop loss:" + String.valueOf(limit) + "%");
                }
            }
        });

        mButtonStart = findViewById(R.id.button6);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsBound) {
                    mBoundService.start(mCurrency);
                } else {
                    mAppContext.startService(new Intent(mAppContext, LocalService.class));

                    mIsBoundByStart = true;
                    doBindService();
                }
            }
        });

        mButtonStop = findViewById(R.id.button7);
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsBound) {
                    mBoundService.stop();
                    doUnbindService();
                }

                mAppContext.stopService(new Intent(mAppContext, LocalService.class));
            }
        });

        mSellNow = findViewById(R.id.button8);
        mSellNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SellNowDialogFragment().show(getFragmentManager(), "sell_now");
            }
        });

        mOrderbook = findViewById(R.id.button);
        mOrderbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsBound) {
                    mBoundService.getOrderbook(callback);
                }
            }
        });

        mAppContext.startService(new Intent(mAppContext, LocalService.class));

        doBindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        if (mIsBound) {
            doUnbindService();
        }
    }

    private void doBindService() {
        mAppContext.bindService(new Intent(mAppContext, LocalService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        mAppContext.unbindService(mConnection);
        mIsBound = false;
    }

    private LocalService mBoundService;
    private boolean mIsBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((LocalService.LocalBinder) service).getService();
            Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_SHORT).show();

            mBoundService.setMainHandler(mUiHandler);

            if (mIsBoundByStart) {
                mBoundService.start(mCurrency);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
            Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
        }
    };

    private LocalService.INotifyOrderbook callback = new LocalService.INotifyOrderbook() {
        @Override
        public void onNotifyOrderbook(String buy, String sell) {
            Message message = Message.obtain(mUiHandler, 1, buy);
            message.sendToTarget();

            message = Message.obtain(mUiHandler, 2, sell);
            message.sendToTarget();
        }
    };

    private MyUiHandler mUiHandler = new MyUiHandler(this);

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                mTextView.setText(mCurrency + "\n" + (String) msg.obj);
                break;
            case 1:
                mTextViewBuy.setText("Buy" + "\n" + (String) msg.obj);
                break;
            case 2:
                mTextViewSell.setText("Sell" + "\n" + (String) msg.obj);
                break;
            case 3:
                new SellNowDialogFragment().show(getFragmentManager(), "sel_now");
                break;
            default:
                break;
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        Log.v(TAG, "sell now ok");
        if (mIsBound) {
            mBoundService.sellNow();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.v(TAG, "sell now cancel");

        mBoundService.restartLoop();
    }

    public static class MyUiHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyUiHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            } else {
                Log.w("DEBUG_MyUiHandler", "activity has already gone");
            }
        }
    }
}
