package utils.internet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Administrator on 2014/9/5.
 */
public class CheckInternetTool {
	public static boolean checkInternet(Context context) {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		System.out.println(wifiInfo + "----" + mobile);

		if((wifiInfo.getState() == NetworkInfo.State.DISCONNECTED) && (mobile.getState() == NetworkInfo.State.DISCONNECTED)){
			return false;
		}
		return true;
	}
}
