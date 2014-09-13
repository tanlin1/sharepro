package utils.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.example.moment.R;
import utils.ImageLoader;
import utils.android.photo.UploadPhoto;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;


/**
 * Created by Administrator on 2014/8/18.
 */
public class CameraActivity extends Activity {

	private static final int CAMERA_ASK = 1000;
	private static final int PICTURE_ASK = 1001;

	private String photoName;
	private File directory;

	private Button post;
	private Button beauty;

	// 当前屏幕显示第一张图片，在整个GridView中的位置
	private int start = 0;
	// 屏幕所能显示的最后一张图片，在整个 GridView中的位置
	private int end = 0;

	private static GridAdapter gridAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 无标题
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.camera_layout);

		//判断将要执行什么操作
		String select = getIntent().getStringExtra("what");
		//拍照
		if (select.equals("camera")) {
			//以日期命名jpg格式
			photoName = DateFormat.format("yyyy-MM-dd-hh-mm-ss",
					Calendar.getInstance(Locale.CHINA)).toString() + ".jpg";
			// SD 卡存在
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				//
				StorageManager manager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
				try {
					// 利用反射， 调用系统（主机）有几张 SD 卡
					Method methodMnt = manager.getClass().getMethod("getVolumePaths");
					String[] path = (String[]) methodMnt.invoke(manager);
					// 在SD card0 （内置）中创建目录
					directory = new File(path[0] + "/moment/photo/");
					if (!directory.exists()) {
						// 创建多级目录
						directory.mkdirs();
					}
					File photo = new File(directory, photoName);

					// 意图（调用相机）
					Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					takePhoto.addCategory(Intent.CATEGORY_DEFAULT);

					//指定你保存路径，不会在系统默认路径下（当然可以指定）
					takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
					//调用系统相机
					startActivityForResult(takePhoto, CAMERA_ASK);

				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		} else if (select.equals("picture")) {
			//选择图片上传
			handleSendMultipleImages();
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String path = "";
		Bitmap bitmap;
		post = (Button) findViewById(R.id.camera_button_photo_direct_post);
		beauty = (Button) findViewById(R.id.camera_button_handle_photo);
		//成功（虽然Intent为空，那是因为我们指定了保存路径，Intent返回的是一个内容提供者Content）
		if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_ASK) {
			//提交原图
			path = directory + "/" + photoName;
			bitmap = BitmapFactory.decodeFile(path);
			((ImageView) findViewById(R.id.camera_photo_scanning)).setImageBitmap(bitmap);

		}
		if (resultCode == Activity.RESULT_OK && requestCode == PICTURE_ASK) {
			sendMessage("selected", "yes");
		}
		final String sendPath = path;
		post.setOnClickListener(new View.OnClickListener() {
			//点击上传原图，就开启上传线程
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CameraActivity.this, UploadPhoto.class);
				if (requestCode == CAMERA_ASK) {
					intent.putExtra("photo_path", sendPath);
				}
				if (requestCode == PICTURE_ASK) {
					intent.putExtra("photo_path", sendPath);
				}
				startActivity(intent);
			}
		});
		//美化图片
		beauty.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("美化图片");
			}
		});
	}

	/**
	 * 用GridView显示多张图片
	 */
	private void handleSendMultipleImages() {
		ArrayList<String> imageUris = ImageLoader.list;
		if (imageUris != null) {
			gridAdapter = new GridAdapter(this, imageUris);
			View v = View.inflate(this, R.layout.multyimage, null);
			GridView gridView = (GridView) v.findViewById(R.id.gridView);
			gridView.setPadding(0, 2, 0, 0);
			// 设置 适配器
			gridView.setAdapter(gridAdapter);
			setContentView(v);

			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					System.out.println(position + "被点击" + view.getId());
					view.setBackgroundColor(Color.BLUE);
				}
			});
			gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					switch (scrollState) {
						// 当不滚动时（仅当屏幕没有滑动的时候才加载图片）
						case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
							// 滚动到显示区域底部
							try {
								// 如果队列中为空，向碎裂中添加数据（新需加载的位置信息）
								if (ImageLoader.FlagQueue.peek() == null) {
									HashMap<String,Integer> hashMap = new HashMap<String, Integer>();
									hashMap.put("start", start - 7);
									hashMap.put("end", end + 7);
									ImageLoader.FlagQueue.put(hashMap);
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							// 滑动至底部
							if (view.getLastVisiblePosition() == (view.getCount() - 1)) {
								System.out.println("滚动至底部，可以do something");
							}
							break;
						// 滑动中(不加载图片)
						case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
							break;
						// 手指在屏幕上（不加载图片）
						case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
							break;
					}
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					start = firstVisibleItem;
					end = start + visibleItemCount;
				}
				});
		}
	}

	/**
	 * 重写BaseAdapter
	 */
	private class GridAdapter extends BaseAdapter {

		private int width = ImageLoader.photoEachWidth;

		private Context context;
		private ArrayList<String> photoPathList;

		public GridAdapter(Context context, ArrayList<String> list) {
			photoPathList = list;
			this.context = context;
		}

		@Override
		public int getCount() {
			return photoPathList.size();
		}

		@Override
		public Object getItem(int position) {
			return photoPathList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view;
			ImageView image;
			view = View.inflate(context, R.layout.items, null);

			image = (ImageView) view.findViewById(R.id.image);
			image.setMinimumHeight(width);
			image.setMinimumWidth(width);
			image.setPadding(3, 2, 3, 2);

			image.setImageBitmap(ImageLoader.hashBitmaps.get(position));

			return view;
		}
	}

	private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			if ("yes".equals(data.getString("notify"))) {
				data.clear();
				gridAdapter.notifyDataSetChanged();
			}
		}
	};

	public static void sendMessage(String key, String value) {
		Bundle data = new Bundle();
		Message msg = new Message();
		data.putString(key, value);
		msg.setData(data);
		handler.sendMessage(msg);
	}
}