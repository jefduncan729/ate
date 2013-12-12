package com.axway.ate.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import android.util.Log;

public class Utilities {
	private static final String TAG = Utilities.class.getSimpleName();
	
	public static final String STRING_SEPARATOR = "|";
	public static final Random random = new Random(System.currentTimeMillis());
	public static final String MESSAGE_DIGEST_ALGORITHM = "SHA";
	public static final String ANDROID_TEST_CLASSNAME = "android.os.Build";

	private static Boolean androidTestResult = null;
	
	public static long strToLongDef(String s, long defVal) {
		long rv = defVal;
		if (s != null) {
	    	try {
	    		rv = Long.parseLong(s);
	    	}
	    	catch (NumberFormatException nfe) {
	    		rv = defVal;
	    	}
		}
    	return rv;
	}
	
	public static int strToIntDef(String s, int defVal) {
		int rv = defVal;
		if (s != null) {
	    	try {
	    		rv = Integer.parseInt(s);
	    	}
	    	catch (NumberFormatException nfe) {
	    		rv = defVal;
	    	}
		}
    	return rv;
	}
//	
//	private static String paramsToString(Map params, String lineEnd) {
//		StringBuffer sb = new StringBuffer();
//		if (params != null) {
//			Set keys = params.keySet();
//			Iterator i = keys.iterator();
//			String key;
//			while (i.hasNext()) {
//				key = (String)i.next(); 
//				sb.append(key + "=" + (String)params.get(key));
//				sb.append(lineEnd);
//			}
//		}
//		return sb.toString();
//	}
//	
//	public static String paramsToString(Map params) {
//		return paramsToString(params, "\n");
//	}
//	
//	public static String paramsToString(Map params, boolean asHtml) {
//		String lineEnd = "\n";
//		if (asHtml) {
//			lineEnd = "<br />";
//		}
//		return paramsToString(params, lineEnd);
//	}
	
	public static String elapsedTimeString(long elapsed) {
		StringBuffer sb = new StringBuffer();
		long secs = elapsed / 1000;
		long mins = secs / 60;
		long hrs = mins / 60;
//		mins = mins % 60;
		sb.append(hrs);
		sb.append(" hour");
		sb.append(hrs == 1 ? "" : "s");
		sb.append(" and ");
		sb.append(mins);
		sb.append(" minute");
		sb.append(mins == 1 ? "" : "s");
		
		return sb.toString();
	}
	
	public static float elapsedByQuarterHour(long elapsed) {
		float rv = 0.0f;
		long secs = elapsed / 1000;
		long mins = secs / 60;
		long hrs = mins / 60;
		rv = hrs;
//		mins = mins % 60;
		if (mins >= 0 && mins <= 15) {
			rv += 0.25;
		}
		else if (mins >= 16 && mins <= 30) {
			rv += 0.5;
		}
		else if (mins >= 31 && mins <= 45) {
			rv += 0.75;
		}
		else {
			rv += 1.0;
		}
		return rv;
	}
	
	public static long calculateElapsedMillis(long start, long stop) {
		return start - stop;
	}
	
	public static float calculateBillableHours(long start, long stop) {
		float rv = 0.0f;
		long secs = (stop - start) / 1000;
		long mins = secs / 60;
		long hrs = mins / 60;
		rv = hrs;
//		mins = mins % 60;
		if (mins >= 0 && mins <= 15) {
			rv += 0.25;
		}
		else if (mins >= 16 && mins <= 30) {
			rv += 0.5;
		}
		else if (mins >= 31 && mins <= 45) {
			rv += 0.75;
		}
		else {
			rv += 1.0;
		}
		return rv;
	}
	
	public static String inputStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuffer sb = new StringBuffer();
		try{
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		}
		catch(Exception e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		finally {
			try {
				is.close();
			}
			catch(Exception ex){}
		}
		return sb.toString();
	}
	
	public static InputStream stringToInputStream(String input) {
		InputStream rv = null;
		if (input != null) {
			input = input.trim();
			try {
				rv = new ByteArrayInputStream(input.getBytes("UTF-8"));
			}
			catch(Exception ex){
			}
		} 
		return rv;
	}
	
	public static String generateUniqueFilename(String path, String prefix, String ext) {
		String rv = "";
		if (prefix == null) {
			prefix = "";
		}
		if (ext == null) {
			ext = "";
		}
		if (ext.length() > 0 && !ext.startsWith(".")) {
			ext = "." + ext;
		}
		if (path.length() > 0 && !path.endsWith(File.separator)) {
			path = path + File.separator;
		}
		File f;
		do {
			f = null;
			rv = path + prefix + Long.toString(System.currentTimeMillis() % 1000) + ext;
			f = new File(rv);
		} while (f != null && f.exists());
		if (f != null) {
			rv = f.getAbsolutePath();
		}
		return rv;
	}	

	public static void copyFile(String input, String output) {
		copyFile(new File(input), new File(output));
	}
	
	public static void copyFile(File input, File output) {
		if (input != null && output != null) {
			output.getParentFile().mkdirs();
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new FileInputStream(input);
				out = new FileOutputStream(output);
		        byte[] buf = new byte[1024];
		        int len;
		        while ((len = in.read(buf)) > 0) {
		            out.write(buf, 0, len);
		        }
		        in.close();
		        in = null;
		        out.flush();
		        out.close();
		        out = null;
			} 
			catch (FileNotFoundException e) {
				Log.e(TAG, e.getLocalizedMessage());
			} 
			catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage());
			}
			finally {
				if (in != null) {
					try {
						in.close(); 
					} catch (IOException e) {}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {}
				}
			}
		}
	}
	
	public static int getRandomPosInt() {
		return getRandomPosInt(Integer.MAX_VALUE-1);
	}
	
	public static int getRandomPosInt(int max) {
		return Math.abs(getRandomInt(max));
	}
	
	public static int getRandomInt() {
		return getRandomInt(Integer.MAX_VALUE-1);
	}
	
	public static int getRandomInt(int max) {
		return random.nextInt(max);
	}
	
	public static long getRandomLong() {
		return getRandomLong(Long.MAX_VALUE-1);
	}

	public static Random getRandom() {
		return random;
	}
	
	public static long getRandomLong(long max) {
		long rv = Math.abs(random.nextLong());
		for (int i = 10; rv >= max && i > 0; i--) {
			rv = rv - (rv / i);
		}
		return rv;
	}
	
	public static String Md5(String input) {
		String rv = null;
		if (input != null) {
			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
				byte[] hashBytes = digest.digest(input.getBytes());
				rv = new String(hashBytes);
			} 
			catch (NoSuchAlgorithmException e) {
				Log.e(TAG, e.getLocalizedMessage());
			}
		}
		return rv;
	}
	
	public static boolean isAndroid() {
		boolean rv = false;
		if (androidTestResult == null) {
			try {
				Class<?> clz = Class.forName(ANDROID_TEST_CLASSNAME);
				rv = true;
			} 
			catch (ClassNotFoundException e) {
				rv = false;
			}
			androidTestResult = new Boolean(rv);
		}
		else {
			rv = androidTestResult.booleanValue();
		}
		return rv;
	}
	
//	public static List<String> getFilenames(String inputDir) {
//		return getFilenames(inputDir, null);
//	}
//	
//	public static List<String> getFilenames(String inputDir, String extensions) {
//		List<String> rv = null;
//		FileExtensionFilter filter = new FileExtensionFilter();
//		if (extensions != null) {
//			filter.addExtension(extensions);
//		}
//		filter.setAllowDirectories(false);
//		File dir = new File(inputDir);
//		if (dir.exists()) {
//			Log.d(TAG, "searching directory: " + dir + " using filter: " + filter);
//			File[] files = dir.listFiles(filter);
//			if (files != null && files.length > 0) {
//				rv = new ArrayList<String>(files.length);
//				for (int i = 0; i < files.length; i++) {
//					rv.add(files[i].getName());
//				}
//			}
//		}
//		else {
//			Log.w(TAG, "directory does not exist: " + inputDir + " (resolves to " + dir.getAbsolutePath() + ")");
//		}
//		return rv;
//	}
	
	public static File createTempFile(String extension) {
		File rv = null;
		try {
			rv = File.createTempFile("tng", extension);
		} 
		catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
			rv = null;
		}
		return rv;
	}
	
	public static BufferedWriter getBufferedWriter(File f) {
		BufferedWriter rv = null;
		if (f != null) {
			try {
				rv = new BufferedWriter(new FileWriter(f));
			} 
			catch (IOException e) {
				Log.e(TAG, e.getLocalizedMessage());
				rv = null;
			}
		}
		return rv;
	}
	
	public static boolean isEmpty(List<?> list) {
		boolean rv = true;
		if (list != null && list.size() > 0)
			rv = false;
		return rv;
	}
}
