package com.shim.myapplication.bit;

import java.util.HashMap;

public class Main {
    public static void main(String args[]) {
		Api_Client api = new Api_Client("api connect key",
			"api secret key");
	
		HashMap<String, String> rgParams = new HashMap<String, String>();
		rgParams.put("order_currency", "BTC");
		rgParams.put("payment_currency", "KRW");
	
	
		try {
		    String result = api.callApi("/info/balance", rgParams);
		    System.out.println(result);
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
    }
}

