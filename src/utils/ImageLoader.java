package utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.example.moment.MainActivity;
import utils.android.CameraActivity;
import utils.android.photo.ImageCompressUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Administrator on 2014/9/8.
 */
public class ImageLoader {

	// 阻塞队列，当队列有数据时，通知线程加载图片，队列初始化长度（1）
	// 程序运行中不应该一直耗用CPU资源
	public static BlockingQueue<HashMap<String, Integer>> FlagQueue = new ArrayBlockingQueue<HashMap<String, Integer>>(1);
	// 储存文件名字（绝对路径）的一个链表数组（因为不确定文件数量）
	public static ArrayList<String> list = new ArrayList<String>();
	// 提供 Bitmap 资源
	public static HashMap<Integer, Bitmap> hashBitmaps;

	public static int photoEachWidth = 0;
	//当前内存中的图片数量（最多）
	static int HASH_BITMAPS_MAXSIZE = 50;
	// 间隔当前屏幕的显示的 开始（/结束）位置开始删除资源，
	// 及时清理内存，保证运行
	final int DELETE_SPACE = 15;

	private Context context;
	// 文件数量，供类内部使用
	private int listLength = 0;



	// 保证单例
	private ImageLoader(Context context) {
		this.context = context;
	}
	// 得到一个实例
	public static ImageLoader getInstance(Context context) {
		return new ImageLoader(context);
	}

	/**
	 * 初始可加载图片
	 */
	public void enable() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("start", 0);
		map.put("end", 20);
		FlagQueue.add(map);
		getSimpleImagePath(context);
		hashBitmaps = new HashMap<Integer, Bitmap>(HASH_BITMAPS_MAXSIZE);
		photoEachWidth = (MainActivity.screenWidth - 8 * 3) / 3;
		new LoadImageThread().start();
	}

	/**
	 * 内部类（加载图片线程）
	 */
	private class LoadImageThread extends Thread {
		// 零时图片路径
		String templePhotoPath;

		@Override
		public void run() {
			while (true) {
				try {
					HashMap<String, Integer> hashMap = FlagQueue.take();
					int startIndex = hashMap.get("start");
					int endIndex = hashMap.get("end");

					// 通用加载算法使用预算，当预算的初始位置比0还小，记为零。
					if(startIndex < 0){
						startIndex = 0;
					}
					// 同理，超过最大值，几位最大值
					if(endIndex > listLength){
						endIndex = listLength;
					}
					// 此时加载图片至内存（大约20张图片，否则将会出现OOM）
					for (int i = startIndex; i < listLength && i < endIndex; i++) {

						templePhotoPath = list.get(i);
						//此map中如果不存在key，则向其中添加键值对（路径，图片）
						if (!hashBitmaps.containsKey(i)) {
							hashBitmaps.put(i, ImageCompressUtil.zoomImage(
									BitmapFactory.decodeFile(templePhotoPath), photoEachWidth, photoEachWidth));
							CameraActivity.sendMessage("notify","yes");
						}
					}
					// 当内存中的图片数量达到上限的时候，删除离屏幕“较远”的位置
					// 开始删除图片，保证用户当前界面附近的图片都在内存，访问边界
					if (hashBitmaps.size() > HASH_BITMAPS_MAXSIZE) {
						for(int i = startIndex - DELETE_SPACE; i >= 0 ; i--){
							if (hashBitmaps.containsKey(i)) {
								hashBitmaps.remove(i);
							}
						}
						for(int j = endIndex + DELETE_SPACE; j < listLength; j++){
							if (hashBitmaps.containsKey(j)) {
								hashBitmaps.remove(j);
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 得到缩略图的路径
	public void getSimpleImagePath(Context context) {
		// 缩略图ID，
		// 根据参数查找对应列
		String[] projection = {MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
		Cursor cursor = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, null);
		if (cursor.moveToFirst()) {
			int dataColumn = cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
			do {
				list.add(cursor.getString(dataColumn));
			} while (cursor.moveToNext());
		}
		listLength = list.size();
	}
}