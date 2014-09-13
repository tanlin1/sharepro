package utils.makejson;

import utils.json.JSONObject;

/**
 * Created by HP on 2014/7/28.
 */
public class JsonTool {

	public static  String createJsonString(String key, String value) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(key, value);
		return jsonObject.toString();
	}
}
