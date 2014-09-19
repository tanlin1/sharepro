package utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.example.moment.MainActivity;
import utils.android.CameraActivity;
import utils.android.photo.ImageCompressUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	public static ArrayList<String> photoPath;
	// 提供 Bitmap 资源
	public static HashMap<Integer, Bitmap> hashBitmaps;

	// gridView 中每个Image 的宽度，根据MainActivity中的屏幕宽度确定后
	public static int photoEachWidth = 0;

	//
	public static String photoParentDirectory = "/storage/sdcard0/";

	public static HashMap<Integer, Boolean> selected;

	// 间隔当前屏幕的显示的 开始（/结束）位置开始删除资源，
	// 及时清理内存，保证运行
	final int DELETE_SPACE = 15;

	private Context context;
	// 文件数量，供类内部使用
	private int photoPathListLength = 0;

	//当前内存中的图片数量（最多）
	private final int hashBitmapMaxSize = 30;
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

		hashBitmaps = new HashMap<Integer, Bitmap>(hashBitmapMaxSize);
		photoEachWidth = (MainActivity.screenWidth - 8 * 3) / 3;
		photoPath = getImagePath();
		sortPhotoPathByTime(photoPath);
		initPhotoSelect(false);
		new LoadImageThread().start();
	}

	/**
	 * 内部类（加载图片线程）
	 */
	private class LoadImageThread extends Thread {
		// 零时图片路径
		String templePhotoPath;
		Bitmap templeBitmap;

		@Override
		public void run() {
			while (true) {
				try {
					HashMap<String, Integer> hashMap = FlagQueue.take();
					int startIndex = hashMap.get("start");
					int endIndex = hashMap.get("end");

					// 通用加载算法使用预算，当预算的初始位置比0还小，记为零。
					if (startIndex < 0) {
						startIndex = 0;
					}
					// 同理，超过最大值，几位最大值
					if (endIndex > photoPath.size()) {
						endIndex = photoPath.size();
					}
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 2;
					// 此时加载图片至内存（大约20张图片，否则将会出现OOM）
					for (int i = startIndex; i < photoPathListLength && i < endIndex; i++){
						templePhotoPath = photoPath.get(i);
						if (!hashBitmaps.containsKey(i)) {
							templeBitmap = ImageCompressUtil.zoomImage(BitmapFactory.decodeFile(templePhotoPath, options), photoEachWidth, photoEachWidth);
							hashBitmaps.put(i, templeBitmap);
							CameraActivity.sendMessage("notify", "yes");
							// 写入文件（缩略图）
							// 由于文件的路径为绝对路径，所以，在悬着保存文件的名字的时候将目录“/”符号更改为“-”（或者其他的）
							writeBitmapToFileCache(templePhotoPath.replace('/', '-'), templeBitmap);
						}
					}
					// 当内存中的图片数量达到上限的时候，删除离屏幕“较远”的位置
					// 开始删除图片，保证用户当前界面附近的图片都在内存，访问边界
					if (hashBitmaps.size() > hashBitmapMaxSize) {
						for (int i = startIndex - DELETE_SPACE; i >= 0; i--) {
							if (hashBitmaps.containsKey(i)) {
								hashBitmaps.remove(i);
							}
						}
						for (int j = endIndex + DELETE_SPACE; j < photoPathListLength; j++) {
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
	private boolean writeBitmapToFileCache(String path, Bitmap bitmap) {
		String dir = photoParentDirectory +  "/moment/.cache/";
		// 特殊的文件名字，应该是可以区分开来的
		String name = "tk2014" + path;
		File fileDir = new File(dir);
		File photo;
		if(!fileDir.exists()){
			fileDir.mkdirs();
		}

		photo = new File(fileDir, name);
// 创建缩略图 到指定路径 隐藏文件夹下面
// 考虑到，写入文件的图片尺寸应该是跟机器相关的，所以直接将显示在屏幕上的图片写入文件，
// 以后就节约了图片的缩放处理的时间
//		Bitmap newBitmap = ThumbnailUtils.extractThumbnail(bitmap, 100, 100);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// 直接将图片写入文件
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

		if(!photo.exists()) {
			try {
				// 文件输出流
				FileOutputStream fo = new FileOutputStream(photo);
				fo.write(outputStream.toByteArray());
				outputStream.close();
				fo.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	// 得到缩略图的路径
	private ArrayList<String> getImagePath() {
		ArrayList<String> list = new ArrayList<String>();
		// 缩略图ID，
		// 根据参数查找对应列
		String[] projection = new String[] {
				MediaStore.Images.Media._ID,
				MediaStore.Images.Media.BUCKET_ID, // 直接包含该图片文件的文件夹ID，防止在不同下的文件夹重名
				MediaStore.Images.Media.BUCKET_DISPLAY_NAME, // 直接包含该图片文件的文件夹名
				MediaStore.Images.Media.DISPLAY_NAME, // 图片文件名
				MediaStore.Images.Media.DATA // 图片绝对路径
		};
		Cursor cursor;// = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection, null, null, null);
		cursor = context.getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, "",
				null, "");
		// 其他列易需要再打开使用（）
//		int fileIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
//		int folderIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
//		int folderColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
//		int fileNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
//		int pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

		//String[] projection = {MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.IMAGE_ID, MediaStore.Images.Thumbnails.DATA};
		if (cursor.moveToFirst()) {
			int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			do {
				list.add(cursor.getString(dataColumn));
			} while (cursor.moveToNext());
		}
		// 找到本应用程序的照片，因为此应用没有将图片共享
		String dir = photoParentDirectory + "/moment/photo/";
		File file = new File(dir);
		String[] strings = file.list();
		for(String name:strings){
			// 依次添加至要显示的文件List中
			list.add(dir + name);
		}
		photoPathListLength = list.size();
		return list;
	}
	// 为后面的全选提供接口
	public static void initPhotoSelect(boolean flag) {
		// 初始化 map 长度
		selected = new HashMap<Integer, Boolean>(photoPath.size());
		for (int i = 0; i < photoPath.size(); i++) {
			// 初始状态均为没有选中
			selected.put(i, flag);
		}
	}
	private void sortPhotoPathByTime(ArrayList<String> list){
		ArrayList<photoObject> pathSorted = new ArrayList<photoObject>();
		int length = list.size();
		for(int i = 0; i < length; i++) {
			String name = list.get(i);
			photoObject temp = new photoObject();
			temp.name = name;
			temp.time = new File(name).lastModified();
			pathSorted.add(temp);
		}
		list.clear();
		Collections.sort(pathSorted, new CompareObject());
		for(int i = 0; i < length; i++){
			list.add(pathSorted.get(i).name);
		}
		pathSorted.clear();
	}

	// 排序 根据时间从大到小
	private class CompareObject implements Comparator<photoObject> {
		@Override
		public int compare(photoObject lhs, photoObject rhs) {
			if (lhs.time > rhs.time) {
				return -1;
			} else if (lhs.time < rhs.time) {
				return 1;
			}
			return 0;
		}
	}
	// 照片对象
	private class photoObject {
		long time;
		String name;
	}
}
