package utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.widget.BaseAdapter;
import com.example.moment.Index;
import utils.android.photo.ImageCompressUtil;

import java.util.ArrayList;

/**
 * Created by Administrator on 2014/9/8.
 */
public class ImageLoader {

	//public static BlockingQueue<Integer> FlagQueue = new ArrayBlockingQueue<Integer>(1);
	//public static BlockingQueue<HashMap<String, Integer>> FlagQueue = new ArrayBlockingQueue<HashMap<String, Integer>>(1);
	public static Bitmap[] bitmaps;// = new ArrayList<Bitmap>();

	public static ArrayList<String> pathList = new ArrayList<String>();


	private boolean allowLoad = true; //初始化允许加载图片（进入界面，用户并没有滑动）
	private Context context;
	private int listSize = 0;

	private ImageLoader(Context context) {
		this.context = context;
	}

	public static ImageLoader getInstance(Context context) {
		return new ImageLoader(context);
	}

	/**
	 * 恢复为初始可加载图片的状态
	 */
	public void restore() {
		this.allowLoad = true;
	}

	/**
	 * 此状态下不允许加载图片
	 */
	public void disable() {
		this.allowLoad = false;
	}

	/**
	 * 此状态可以加载图片
	 */
	public void enable() {

		getSimpleImagePath(context);
		bitmaps = new Bitmap[listSize];
		new LoadImageThread().start();
	}

	BaseAdapter gtest;

	private class LoadImageThread extends Thread {

		@Override
		public void run() {
			Bitmap bitmap;
			String path;
			for (int i = 0; i < listSize; i++) {
				path = pathList.get(i);
				bitmap = ImageCompressUtil.compressByQuality(path, 100);

				bitmaps[i] = ImageCompressUtil.zoomImage(bitmap, Index.width, Index.width);
			}

		}
	}

	public void getSimpleImagePath(Context context) {
		// 缩略图ID，
		// 根据参数查找对应列
		String[] projection = {MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
		Cursor cursor = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, null);
		if (cursor.moveToFirst()) {
			int dataColumn = cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
			do {
				pathList.add(cursor.getString(dataColumn));
			} while (cursor.moveToNext());
		}
		listSize = pathList.size();
	}
}