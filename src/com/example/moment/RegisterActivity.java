package com.example.moment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import utils.android.Read;
import utils.android.judgment.Login;
import utils.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by HP on 2014/7/18.
 */
public class RegisterActivity extends Activity {

	public static String SESSIONID = null;
	private String name;
	private String password;
	private String passwordConfirm;
	private String emailAddress;
	private CheckBox checkBox;

	private EditText nameFind;
	private EditText passwordFind;
	private EditText emailAddressFind;
	private EditText passwordConfirmFind;

	private Map<String, String> userInformation = new HashMap<String, String>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/**
		 * 设置无标题，全屏幕显示
		 */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.register);
		/**
		 * 找到各个编辑框
		 */
		nameFind = ((EditText) findViewById(R.id.register_name_edit));
		passwordFind = ((EditText) findViewById(R.id.register_password_edit));
		emailAddressFind = ((EditText) findViewById(R.id.register_email_edit));
		passwordConfirmFind = (EditText) findViewById(R.id.register_password_confirm_edit);

		checkBox = (CheckBox) findViewById(R.id.checkbox);
		name = nameFind.getText().toString();
		password = passwordFind.getText().toString();
		passwordConfirm = passwordConfirmFind.getText().toString();
		emailAddress = emailAddressFind.getText().toString();
		/**
		 * 获取注册按钮
		 */
		Button register = (Button) findViewById(R.id.app_register);
		register.setOnClickListener(new ButtonOnClickListener());


		checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				/**
				 * 打开新窗口用户协议给用户浏览
				 */
				TextView protocol = (TextView) findViewById(R.id.protocol);
				protocol.getScrollBarStyle();
				if (!checkBox.isChecked()) {
					String s = getProtocolMessage(getResources().openRawResource(R.raw.test));
					protocol.setMovementMethod(new ScrollingMovementMethod());
					protocol.setText(s);
				} else {
					protocol.setText("");
				}
			}
		});
		nameFind.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					name = nameFind.getText().toString();
					if (name.length() == 0) {
						Toast.makeText(getApplication(), R.string.name_null, Toast.LENGTH_LONG).show();
					}
				}
			}
		});

		passwordFind.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					password = passwordFind.getText().toString();
					if (password.length() == 0) {
						Toast.makeText(getApplication(), R.string.password_null, Toast.LENGTH_LONG).show();
					}
				}
			}
		});
		passwordConfirmFind.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					passwordConfirm = passwordConfirmFind.getText().toString();
					if (!password.equals(passwordConfirm)) {
						Toast.makeText(getApplication(), R.string.password_equal, Toast.LENGTH_LONG).show();
					}
				}
			}
		});
		//在填写邮箱的时候会连接服务器并检测此邮箱是否已经注册过本网站
		emailAddressFind.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
					emailAddress = emailAddressFind.getText().toString();
					//自动以藏输入键盘
					InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
					}
					if (!Login.isEmail(emailAddress)) {
						Toast.makeText(getApplication(), R.string.email_wrong, Toast.LENGTH_SHORT).show();
						emailAddressFind.setText("");
					} else {
						new emailCheckThread().start();
					}
					return true;
				}
				return false;
			}
		});
	}

	//处理子线程传回的数据（消息）
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			//注册成功
			if ("ok".equals(data.getString("register"))) {
				Toast.makeText(getApplicationContext(), "注册成功", Toast.LENGTH_SHORT).show();
				//进入个人主页（设置）
				//或者进入动态页
				System.out.println("正在尝试进入主页，敬请期待！");

			} else if ("yes".equals(data.getString("emailIsUsed"))) {
				//邮箱被注册过
				AlertDialog.Builder dialog = new AlertDialog.Builder(RegisterActivity.this);
				dialog.setMessage(R.string.email_used);
				dialog.setCancelable(true);
				dialog.setTitle(R.string.register_error);
				emailAddressFind.setText("");
				dialog.create().show();
			} else if ("false".equals(data.getString("edited"))) {
				Toast.makeText(RegisterActivity.this, R.string.name_null, Toast.LENGTH_SHORT).show();
			} else if ("yes".equals(data.getString("timeout"))) {
				Toast.makeText(RegisterActivity.this, R.string.timeout, Toast.LENGTH_SHORT).show();
			}
		}
	};

	/**
	 * 检测用户注册的时候，邮箱是否符合注册标准
	 * 必须在子线程中执行
	 */
	private class emailCheckThread extends Thread {
		public void run() {
			emailCheck();
		}
	}

	/**
	 * @param url 具体的url
	 *
	 * @return 此URL的HttpURLConnection连接
	 */
	private HttpURLConnection getUrlConnect(String url) {
		URL concreteUrl;
		HttpURLConnection connection = null;
		try {
			concreteUrl = new URL(url);
			connection = (HttpURLConnection) concreteUrl.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setConnectTimeout(3000);
			//设置请求头字段
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//          这个属性将被用于大文件传输，有效的提高效率
//			connection.setRequestProperty("Content-Type","multipart/form-data");
			//有相同的属性则覆盖
			connection.setRequestProperty("user-agent", "Android 4.0.1");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connection;
	}

	private void emailCheck() {
		String emailCheckUrl = ":8080/phone_isEmailUsed";
		HttpURLConnection connection;
		try {
			connection = getUrlConnect(emailCheckUrl);
			connection.connect();
			JSONObject object = new JSONObject();
			object.put("email", emailAddress);
			connection.getOutputStream().write(object.toString().getBytes());
			//读取服务器返回的消息
			JSONObject jsonObject = new JSONObject(Read.read(connection.getInputStream()));

			if (jsonObject.getString("isUsed").equals("yes")) {
				sendMessage("emailIsUsed", "yes");
				connection.disconnect();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param inputStream 一个输入流
	 * @return 从该输入流读取的内容
	 */
	private String getProtocolMessage(InputStream inputStream) {

		InputStreamReader inputStreamReader;
		String content = null;
		try {
			inputStreamReader = new InputStreamReader(inputStream, "utf-8");

			BufferedReader reader = new BufferedReader(inputStreamReader);
			StringBuilder sb = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			content = sb.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	/**
	 * 点击注册按钮
	 */
	private class ButtonOnClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			if (canRegister()) {
				new registerThread().start();
			}
		}
	}

	private void Register() {
		HttpURLConnection connection;
		JSONObject info = new JSONObject();

		String registerUrl = ":8080/phone_register";

		connection = getUrlConnect(registerUrl);

		userInformation.put("name", name);
		userInformation.put("email", emailAddress);
		userInformation.put("password", password);

		info.put("register", userInformation);
		try {
			connection.connect();
			OutputStream writeToServer = connection.getOutputStream();
			writeToServer.write(info.toString().getBytes());

			// 取得输入流，并使用Reader读取
			JSONObject object = Read.read(connection.getInputStream());
			if (null != object.getString("JSESSIONID")) {
				SESSIONID = object.getString("JSESSIONID");
				sendMessage("register", "ok");
			}
			System.out.println(object);
			// 断开连接
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			sendMessage("timeout", "yes");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			connection.disconnect();
		}
	}

	private class registerThread extends Thread {
		@Override
		public void run() {
			Register();
		}
	}

	private boolean canRegister() {
		if (name.length() == 0 || password.length() == 0 || emailAddress.length() == 0 || (!checkBox.isChecked())) {
			sendMessage("edited", "false");
			return false;
		}
		return true;
	}

	private void sendMessage(String key, String value) {
		Bundle data = new Bundle();
		Message msg = new Message();
		data.putString(key, value);
		msg.setData(data);
		handler.sendMessage(msg);
	}
}
