package utils.android;


import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by HP on 2014/8/2.
 */
public class Write {
	public static boolean write(Context context, String fileName, String content) throws IOException {
		FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
		out.write(content.getBytes());

		return true;
	}
}
