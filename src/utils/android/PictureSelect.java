package utils.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import com.example.moment.R;
import utils.android.photo.UploadPhoto;

/**
 * Created by Administrator on 2014/8/21.
 */
public class PictureSelect extends Activity {

	private static final int PICTURE_ASK = 1001;
	private String photoPath = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera_layout);
		Intent pictureSelect = new Intent(Intent.ACTION_GET_CONTENT);
		pictureSelect.setType("image/*");

		//调用系统相册
		startActivityForResult(pictureSelect, PICTURE_ASK);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && requestCode == PICTURE_ASK) {

			sendMessage("selected","yes");
			handlePictureSelect(data);
		}
	}

	//处理选择的图片
	private void handlePictureSelect(Intent data) {

		Uri imgUri;
		Cursor cursor = null;
		String[] pojo;

		// 照片的原始资源地址
		if (data != null) {
			imgUri = data.getData();
			pojo = new String[]{MediaStore.Images.Media.DATA};
			cursor = managedQuery(imgUri, pojo, null, null, null);
			System.out.println("" + imgUri.getEncodedPath());

		}
		if (cursor != null) {
			int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			photoPath = cursor.getString(columnIndex);
			cursor.close();
		}
		if (photoPath != null){
			sendMessage("show", "yes");
		}else {
			sendMessage("show", "no");
		}
	}

	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			if("yes".equals(data.getString("show"))){
				Bitmap bm = BitmapFactory.decodeFile(photoPath);
				((ImageView)findViewById(R.id.camera_photo_scanning)).setImageBitmap(bm);
				Intent intent = new Intent(PictureSelect.this, UploadPhoto.class);
				intent.putExtra("photo",bm);
			}
			if("yes".equals(data.getString("selected"))){

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
