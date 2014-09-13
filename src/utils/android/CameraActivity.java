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
import com.example.moment.Index;
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
	//
	private ArrayList<Bitmap> bitmaps;// = ImageLoader.bitmapArray;

	private static final int CAMERA_ASK = 1000;
	private static final int PICTURE_ASK = 1001;

	private String photoName;
	private File directory;

	private Button post;
	private Button beauty;
	private String photoPath;
	private ImageLoader imageLoader;

	ArrayList<String> imageUris;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_layout);
		imageLoader = Index.imageLoader;
		String select = getIntent().getStringExtra("what");


		imageUris = ImageLoader.pathList;
		gridAdapter = new GridAdapter(this, imageUris);


		if (select.equals("camera")) {
			//以日期命名jpg格式
			photoName = DateFormat.format("yyyy-MM-dd-hh-mm-ss",
					Calendar.getInstance(Locale.CHINA)).toString() + ".jpg";

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

				StorageManager manager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
				try {
					Method methodMnt = manager.getClass().getMethod("getVolumePaths");

					String[] path = (String[]) methodMnt.invoke(manager);

					String filePath = path[0] + "/moment/photo/";
					directory = new File(filePath);
					if (!directory.exists()) {
						directory.mkdirs();
					}
					File photo = new File(directory, photoName);

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
			handleSendMultipleImages();
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String path = null;
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
			//path = getImagePath(data);
		}
		final String finalPath = path;
		post.setOnClickListener(new View.OnClickListener() {
			//点击上传原图，就开启上传线程
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CameraActivity.this, UploadPhoto.class);
				if (requestCode == CAMERA_ASK) {
					intent.putExtra("photo_path", finalPath);
				}
				if (requestCode == PICTURE_ASK) {
					intent.putExtra("photo_path", finalPath);
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
	public static GridAdapter gridAdapter;

	private void handleSendMultipleImages() {

		if (imageUris != null) {

			final View v = View.inflate(this, R.layout.multyimage, null);
			GridView gridView = (GridView) v.findViewById(R.id.gridView);
			gridView.setPadding(0, 2, 0, 0);
			//gridView.setNumColumns(GridView.AUTO_FIT);

			System.out.println(gridAdapter);
			gridView.setAdapter(gridAdapter);
			setContentView(v);

			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					System.out.println(position + "被点击" + view.getId());
					view.setBackgroundColor(Color.BLUE);
					gridAdapter.notifyDataSetChanged();
				}
			});

			gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
				HashMap<String, Integer> map = new HashMap<String, Integer>();

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					switch (scrollState) {
						// 当不滚动时（仅当屏幕没有滑动的时候才加载图片）
						case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
							// 滚动到显示区域底部
							gridAdapter.notifyDataSetChanged();
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
							//gridAdapter.notifyDataSetChanged();
							break;
					}
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					//gridAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	/**
	 * 重写BaseAdapter
	 */

	public class GridAdapter extends BaseAdapter {
		private Context mContext;
		private ArrayList<String> photoPathList;

		public GridAdapter(Context Context, ArrayList<String> list) {
			photoPathList = list;
			this.mContext = Context;
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
			Bitmap bitmap;
			View view;
			ImageView image;

			view = View.inflate(mContext, R.layout.items, null);

			image = (ImageView) view.findViewById(R.id.image);
			image.setMinimumHeight(Index.width);
			image.setMinimumWidth(Index.width);
			image.setPadding(Index.base, 2, Index.base, 2);

			bitmap = ImageLoader.bitmaps[position];

//			new FadeInBitmapDisplayer(10) {
//				@Override
//				public Bitmap display(Bitmap bitmap, ImageView imageView) {
//
//					return bitmap;
//				}
//
//			}.display(bitmap,image);
			image.setImageBitmap(bitmap);
			System.out.println("------------  " + position + " ----------------- ok!   ");

			//image.setImageBitmap(bitmap);
//			//添加至map里面

			if(position == 11){
				sendMessage("notify","yes");
			}
			return view;
		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			if ("yes".equals(data.getString("show"))) {
				Bitmap bm = BitmapFactory.decodeFile(photoPath);
				((ImageView) findViewById(R.id.camera_photo_scanning)).setImageBitmap(bm);
			}
			if("yes".equals(data.getString("notify"))){
				gridAdapter.notifyDataSetChanged();
			}
		}
	};

	private void sendMessage(String key, String value) {
		Bundle data = new Bundle();
		Message msg = new Message();
		data.putString(key, value);
		msg.setData(data);
		handler.sendMessage(msg);
	}
}