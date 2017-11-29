package com.shim.myapplication;

public class Price {
    public Float currentPrice;
    public double maxPrice;
    public double buyingPrice;
    private boolean mIsSellNow;

    public Price(Float price, double max, double buying) {
        currentPrice = price;
        maxPrice = max;
        buyingPrice = buying;
    }

    public double getDiff() {
        return (currentPrice - buyingPrice);
    }

    public double getRatio() {
        double percentage = (currentPrice - buyingPrice) / buyingPrice * 100;
        return Math.round(percentage * 100d) / 100d;
    }

    public boolean isSellNow() {
        return mIsSellNow;
    }

    public void setSellNow() {
        mIsSellNow = true;
    }
}
