package utils.android.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2014/9/3.
 */
public class ImageCompressUtil {

	public static Bitmap compressByQuality(String path, int maxSize){

		Bitmap bitmap = BitmapFactory.decodeFile(path);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int quality = maxSize;

		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

		return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length);
	}
	/**
	 *
	 * @param pathName
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 * @throws IOException
	 */
	public static Bitmap compressBySize(String pathName, int targetWidth, int targetHeight) throws IOException {

		BitmapFactory.Options opts = new BitmapFactory.Options();
		// 不去真的解析图片，只是获取图片的头部信息，包含宽高等；
		opts.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(pathName, opts);

		// 得到图片的宽度、高度；
		int imgWidth = opts.outWidth;
		int imgHeight = opts.outHeight;

		// 分别计算图片宽度、高度与目标宽度、高度的比例；取大于等于该比例的最小整数；
		int widthRatio = (int) Math.ceil(imgWidth / (float) targetWidth);
		int heightRatio = (int) Math.ceil(imgHeight / (float) targetHeight);

		if (widthRatio > 1 || heightRatio > 1) {
			if (widthRatio > heightRatio) {
				opts.inSampleSize = heightRatio;
			} else {
				opts.inSampleSize = widthRatio;
			}
		}
		// 设置好缩放比例后，加载图片进内容；
		opts.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(pathName, opts);
	}

	public static Bitmap zoomImage(Bitmap oldBitmap, double newWidth, double newHeight) {
		Bitmap bitmap;
		// 获取这个图片的宽和高
		float width = oldBitmap.getWidth();
		float height = oldBitmap.getHeight();
		// 创建操作图片用的matrix对象
		Matrix matrix = new Matrix();
		// 计算宽高缩放率
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// 缩放图片动作
		matrix.postScale(scaleWidth, scaleHeight);
		bitmap = Bitmap.createBitmap(oldBitmap, 0, 0, (int) width, (int) height, matrix, true);
		oldBitmap.recycle();
		return bitmap;
	}
}
