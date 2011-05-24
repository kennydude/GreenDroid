package greendroid.image;

/**
 * @author Sergi Juanola
 */
import greendroid.util.Config;
import greendroid.util.Md5Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class ImageCacheHardStore {

	private static ImageCacheHardStore INSTANCE = null;
	private File fullCacheDir;
	private static BitmapFactory.Options sOptions;

	private ImageCacheHardStore() {

		if (sOptions == null) {
			sOptions = new BitmapFactory.Options();
			sOptions.inDither = true;
			sOptions.inScaled = true;
		}

		fullCacheDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
				"/Android/data/uk.co.senab.blueNotify/cache");
		if (!fullCacheDir.exists()) {
			if (Config.GD_INFO_LOGS_ENABLED) {
				Log.i("CACHE", "Directory doesn't exist");
			}
			cleanCacheStart();
			return;
		}
	}

	private void cleanCacheStart() {
		fullCacheDir.mkdirs();
		File noMedia = new File(fullCacheDir.toString(), ".nomedia");
		try {
			noMedia.createNewFile();
			if (Config.GD_INFO_LOGS_ENABLED) {
				Log.i("CACHE", "Cache created");
			}
		} catch (IOException e) {
			Log.e("CACHE", "Couldn't create .nomedia file");
			e.printStackTrace();
		}
	}

	private synchronized static void createInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ImageCacheHardStore();
		}
	}

	public static ImageCacheHardStore getInstance() {
		if (INSTANCE == null) createInstance();
		return INSTANCE;
	}

	public void saveCacheFile(String cacheUri, InputStream in) {
		String fileLocalName = getFileNameFromURL(cacheUri);

		File fileUri = new File(fullCacheDir.toString(), fileLocalName);
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(fileUri);

			byte[] buffer = new byte[4096];
			int len = in.read(buffer);
			while (len != -1) {
				outStream.write(buffer, 0, len);
				len = in.read(buffer);
			}

			buffer = null;
			outStream.flush();
			in.close();
			in = null;
			outStream.close();
			outStream = null;

			if (Config.GD_INFO_LOGS_ENABLED) {
				Log.i("CACHE", "Saved " + cacheUri + " to: " + fileUri.toString());
			}
		} catch (FileNotFoundException e) {
			if (Config.GD_INFO_LOGS_ENABLED) {
				Log.i("CACHE", "Error: File " + cacheUri + " was not found!");
			}
			e.printStackTrace();
		} catch (IOException e) {
			if (Config.GD_INFO_LOGS_ENABLED) {
				Log.i("CACHE", "Error: File could not be stuffed!");
			}
			e.printStackTrace();
		}
	}

	public Bitmap getCacheFile(String cacheUri) {

		File fileUri = new File(fullCacheDir.getAbsolutePath(), getFileNameFromURL(cacheUri));
		if (!fileUri.exists()) return null;

		if ((fileUri.lastModified() + 172800000) < System.currentTimeMillis() && cacheUri.contains("graph.facebook.com")) {
			if (fileUri.delete()) {
				return null;
			}
		}

		if (Config.GD_INFO_LOGS_ENABLED) {
			Log.i("CACHE", "File " + cacheUri + " has been found in the Cache");
		}

		return BitmapFactory.decodeFile(fileUri.getAbsolutePath(), sOptions);
	}

	private static String getFileNameFromURL(String url) {
		return Md5Util.md5(url);
	}

	public Bitmap decodeByteArrayToBitmap(byte[] image) {
		return BitmapFactory.decodeByteArray(image, 0, image.length, sOptions);
	}

	public int deleteCache() {

		int ret = 0;
		if (fullCacheDir.exists()) {
			ret = deleteRecursive(fullCacheDir);
		}
		this.cleanCacheStart();

		return ret;

	}

	public static int deleteRecursive(File path) {

		int numberFilesDeleted = 0;

		if (path.exists()) {
			if (path.isDirectory()) {
				for (File f : path.listFiles()) {
					numberFilesDeleted += deleteRecursive(f);
				}
			}
			path.delete();
			numberFilesDeleted++;
		}

		return numberFilesDeleted;
	}

}