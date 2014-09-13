package utils.android;

import utils.json.JSONObject;

import java.io.*;

/**
 * Created by HP on 2014/8/2.
 */
public class Read {
	public static JSONObject read(InputStream in) throws IOException {
		String temp;
		StringBuffer sb = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(in,"utf-8"));
		while((temp = br.readLine()) != null){
			sb.append(temp);
		}
		br.close();
		JSONObject obj = new JSONObject(sb.toString());
		return obj;
	}
}
