package com.toly1994.tolymusic.app.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import com.toly1994.tolymusic.R;

import java.io.*;


public class ImageUtils {
	private static final Uri sArtworkUri = Uri
			.parse("content://media/external/audio/albumart");
	private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();

	public static Bitmap getArtwork(Context context, String title,
			long song_id, long album_id, boolean allowdefault) {
		if (album_id < 0) {
			if (song_id >= 0) {
				Bitmap bm = getArtworkFromFile(context, song_id, -1);
				if (bm != null) {
					return bm;
				}
			}
			if (allowdefault) {
				return getDefaultArtwork(context);
			}
			return null;
		}
		if(context != null){
			ContentResolver res = context.getContentResolver();
			Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
			if (uri != null) {
				InputStream in = null;
				try {
					in = res.openInputStream(uri);
					Bitmap bmp = BitmapFactory.decodeStream(in, null,
							sBitmapOptions);
					if (bmp == null) {
						bmp = getDefaultArtwork(context);
					}
					return bmp;
				} catch (FileNotFoundException ex) {
					Bitmap bm = getArtworkFromFile(context, song_id, album_id);
					if (bm != null) {
						if (bm.getConfig() == null) {
							bm = bm.copy(Bitmap.Config.RGB_565, false);
							if (bm == null && allowdefault) {
								return getDefaultArtwork(context);
							}
						}
					} else if (allowdefault) {
						bm = getDefaultArtwork(context);
					}
					return bm;
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	private static Bitmap getArtworkFromFile(Context context, long songid,
			long albumid) {
		Bitmap bm = null;
		if (albumid < 0 && songid < 0) {
			throw new IllegalArgumentException(
					"Must specify an album or a song id");
		}
		try {
			if (albumid < 0) {
				Uri uri = Uri.parse("content://media/external/audio/media/"
						+ songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					FileDescriptor fd = pfd.getFileDescriptor();
					bm = BitmapFactory.decodeFileDescriptor(fd);
				}
			} else {
				Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					FileDescriptor fd = pfd.getFileDescriptor();
					bm = BitmapFactory.decodeFileDescriptor(fd);
				}
			}
		} catch (FileNotFoundException ex) {

		}
		return bm;
	}

	private static Bitmap getDefaultArtwork(Context context) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		return BitmapFactory.decodeStream(context.getResources()
				.openRawResource(R.drawable.default_music_icon), null, opts);
	}
	/**
	 * 压缩专辑封面大小每张在指定大小内
	 * @param image
	 * @return
	 */
	public static Bitmap compressBitmap(Bitmap image, int size) {
		Bitmap result = null;
		if(image != null){
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			image.compress(Bitmap.CompressFormat.JPEG, 85, out);
			float zoom = (float) Math.sqrt(size * 1024
					/ (float) out.toByteArray().length);

			Matrix matrix = new Matrix();
			matrix.setScale(zoom, zoom);

			result = Bitmap.createBitmap(image, 0, 0, image.getWidth(),
					image.getHeight(), matrix, true);

			out.reset();
			result.compress(Bitmap.CompressFormat.JPEG, 85, out);
			while (out.toByteArray().length > size * 1024) {
				matrix.setScale(0.9f, 0.9f);
				result = Bitmap.createBitmap(result, 0, 0, result.getWidth(),
						result.getHeight(), matrix, true);
				out.reset();
				result.compress(Bitmap.CompressFormat.JPEG, 85, out);
			}
		}
		
		return result;
	}

	/***
	 * 设置图片的颜色
	 * @param image
	 */
	public static Bitmap setBitmapColor(Bitmap image) {
		int width = image.getWidth();
		int height = image.getHeight();
		Bitmap grayImg = null;
		try {
			grayImg = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(grayImg);
			Paint paint = new Paint();
			ColorMatrix colorMatrix = new ColorMatrix();
			float[] colorArray = { 1, 0, 0, 100, 0, 0, 1, 100, 0, 0, 0, 0, 1,
					0, 0, 0, 0, 0, 1, 0 };
			colorMatrix.setSaturation(0);
			colorMatrix.set(colorArray);
			ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(
					colorMatrix);
			paint.setColorFilter(colorMatrixFilter);
			canvas.drawBitmap(image, 0, 0, paint);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grayImg;
	}

	/**
	 * 颜色加深处理
	 * @param RGBValues
	 *            RGB的值，由alpha（透明度）、red（红）、green（绿）、blue（蓝）构成，
	 *            Android中我们一般使用它的16进制，
	 *            例如："#FFAABBCC",最左边到最右每两个字母就是代表alpha（透明度）、
	 *            red（红）、green（绿）、blue（蓝）。每种颜色值占一个字节(8位)，值域0~255
	 *            所以下面使用移位的方法可以得到每种颜色的值，然后每种颜色值减小一下，在合成RGB颜色，颜色就会看起来深一些了
	 * @return
	 */
	public static int colorBurn(int RGBValues) {
		int red = RGBValues >> 16 & 0xFF;
		int green = RGBValues >> 8 & 0xFF;
		int blue = RGBValues & 0xFF;
		red = (int) Math.floor(red * (1 - 0.1));
		green = (int) Math.floor(green * (1 - 0.1));
		blue = (int) Math.floor(blue * (1 - 0.1));
		return Color.rgb(red, green, blue);
	}

	/**
	 * 图片模糊处理
	 * @param context
	 * @param bitmap
	 * @return
	 */
	public static Bitmap blurBitmap(Context context, Bitmap bitmap) {
		Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		RenderScript rs = RenderScript.create(context);
		ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs,
				Element.U8_4(rs));
		Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
		Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
		blurScript.setRadius(25.f);
		blurScript.setInput(allIn);
		blurScript.forEach(allOut);
		allOut.copyTo(outBitmap);
		// bitmap.recycle();
		rs.destroy();
		return outBitmap;
	}

	public static Bitmap bigBitmap(Bitmap bitmap) {
		Matrix matrix = new Matrix();
		matrix.postScale(5f, 5f); // 长和宽放大缩小的比例
		Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
				bitmap.getHeight(), matrix, true);
		return resizeBmp;
	}
}
