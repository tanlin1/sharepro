package utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.example.moment.Index;
import utils.android.photo.ImageCompressUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Administrator on 2014/9/8.
 */
public class ImageLoader {

	//public static BlockingQueue<Integer> FlagQueue = new ArrayBlockingQueue<Integer>(1);
	public static BlockingQueue<HashMap<String, Integer>> FlagQueue = new ArrayBlockingQueue<HashMap<String, Integer>>(2);
	public static ArrayList<String> pathList = new ArrayList<String>();
	public static HashMap<Integer, Bitmap> hashBitmaps;// = new HashMap<Integer, Bitmap>();

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
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("start", 0);
		map.put("end", 20);
		FlagQueue.add(map);
		getSimpleImagePath(context);
		hashBitmaps = new HashMap<Integer, Bitmap>();
		System.out.println(listSize + "---------------------------------------");
		new LoadImageThread().start();
	}

	private class LoadImageThread extends Thread {
		String path;
		int start = 0;
		int end = 0;
		HashMap<String, Integer> hashMap;

		@Override
		public void run() {
			hashMap = new HashMap<String, Integer>();
			while (true) {
				try {
					hashMap = FlagQueue.take();
					start = hashMap.get("start");
					end = hashMap.get("end");
					if(start < 0){
						start = 0;
					}
					if(end > listSize){
						end = listSize;
					}
					for (int i = start; i < listSize && i < end; i++) {

						path = pathList.get(i);
						//此map中如果不存在key，则向其中添加键值对（路径，图片）
						if (!hashBitmaps.containsKey(i)) {
							System.out.println("------------------  线程将第  " + i + "存入 map 中");
							hashBitmaps.put(i, ImageCompressUtil.zoomImage(
									BitmapFactory.decodeFile(path), Index.width, Index.width));
						}
					}
					if (hashBitmaps.size() > 50) {
						System.out.println("--------------线程清除数据中 -- - --" + "start: " + start + "end" + end);
						for(int i = 0; i <= start - 40 ; i++){
							if (hashBitmaps.containsKey(i)) {
								System.out.println("----start---------------  线程将第  " + i + "从 map 中 丢弃");
								hashBitmaps.remove(i);
							}
						}
						for(int j = end; j >= end - 10 && j < listSize; j--){
							if (hashBitmaps.containsKey(j)) {
								System.out.println("-----end-----------------  线程将第  " + j + "从 map 中 丢弃");
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