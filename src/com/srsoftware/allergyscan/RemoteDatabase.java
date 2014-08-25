package com.srsoftware.allergyscan;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RemoteDatabase {
	protected static String TAG = "AllergyScan";
	private static String adress = "http://allergy.srsoftware.de?action=";
	static Integer missingCredits = null;

	public static TreeMap<Integer, String> getAvailableAllergens() throws IOException {
		Log.d(TAG, "getAvailableAllergens");
		BufferedReader reader = postData("allergenList", null);
		String line;
		TreeMap<Integer, String> result = new TreeMap<Integer, String>();
		while ((line = reader.readLine()) != null) {
			int space = line.indexOf(" ");
			if (space > -1) {
				String name = line.substring(space).trim();
				int id = Integer.parseInt(line.substring(0, space).trim());
				result.put(id, name);
			}
		}
		reader.close();
		return result;
	}

	private static BufferedReader postData(String action) throws IOException {
		return postData(action, null);
	}

	private static BufferedReader postData(String action, TreeMap<String, String> data) throws IOException {
		URL url = new URL(adress + action);
		Log.d(TAG, "trying to send POST data to " + url + ":");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);

		if (data != null && !data.isEmpty()) {
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			for (Iterator<Entry<String, String>> it = data.entrySet().iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				out.writeBytes(entry.getKey() + "=" + entry.getValue());
				Log.d(TAG, entry.getKey() + "=" + entry.getValue());
				if (it.hasNext()) {
					out.write('&');
				}
			}
			out.close();
		}

		BufferedReader result = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return result;
	}

	private static BufferedReader postData(String action, String key, Object value,String deviceid) throws IOException {
		TreeMap<String, String> data = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		if (key!=null && value !=null){
			if (value instanceof TreeMap) {
				data.put(key, createJsonArray((TreeMap<?, ?>) value));
			} else {
				data.put(key, value.toString());
			}
		}
		if (deviceid!=null){
			data.put("device", deviceid);
		}
		return postData(action, data);
	}
	
	private static BufferedReader postData(String action, String key, Object value) throws IOException {
		return postData(action, key, value, null);
	}

	private static String createJsonArray(TreeMap<?, ?> value) throws UnsupportedEncodingException {
		StringBuffer result = new StringBuffer();
		if (value == null || value.isEmpty()) {
			return "{}";
		}
		result.append('{');
		for (Entry<?, ?> entry : value.entrySet()) {
			Object entryValue = entry.getValue();
			if (entryValue instanceof TreeMap) {
				TreeMap<?,?> map = (TreeMap<?,?>) entryValue;
				entryValue=createJsonArray(map); // recursive!
			} else {
				entryValue=encode(entryValue);
			}
			result.append(encode(entry.getKey()) + ':' + entryValue + ',');		
		}
		result.deleteCharAt(result.length() - 1);
		result.append('}');
		return result.toString();
	}

	private static String encode(Object text) throws UnsupportedEncodingException {
		return '"' + URLEncoder.encode(text.toString(), "UTF-8") + '"';
	}

	public static JSONObject getNewProducts() throws IOException {
		try {
			Log.d(TAG, "RemoteDatabase.getNewProducts(...)");
			BufferedReader reader = postData("getNewProducts");			
			JSONObject array = new JSONObject(reader.readLine());
			reader.close();
			return array;
		} catch (JSONException e) { // usually happens with empty reply
			e.printStackTrace();
			return null;
		}
	}

	public static void storeNewProducts(TreeMap<Long, String> products) throws IOException {
		Log.d(TAG, "RemoteDatabase.storeNewProducts(...)");
		if (products == null || products.isEmpty()) {
			return;
		}
		BufferedReader reader = postData("storeNewProducts", "products", products);
		System.out.println(reader.readLine());
		reader.close();
	}

	public static JSONObject getNewAllergens(AllergenList allergens) throws IOException {
		Log.d(TAG, "RemoteDatabase.getNewAllergens(...)");
		TreeSet<Integer> remoteIds = new TreeSet<Integer>();
		TreeSet<String> newNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		for (Allergen allergen : allergens.values()) {
			if (allergen.aid == 0) {
				newNames.add(encode(allergen.name));
			} else {
				remoteIds.add(allergen.aid);
			}
		}

		TreeMap<String, String> data = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		data.put("aids", remoteIds.toString());
		data.put("names", newNames.toString());
		try {
			BufferedReader reader = postData("getNewAllergens", data);
			JSONObject array = new JSONObject(reader.readLine());
			reader.close();
			return array;
		} catch (JSONException e) { // usually happens with empty reply
			e.printStackTrace();
			return null;
		}

	}

	public static JSONObject getInfo(AllergenList allergens) throws IOException {
		Log.d(TAG, "RemoteDatabase.getInfo(...)");
		if (allergens==null || allergens.isEmpty()) return null;
		TreeSet<Integer> remoteAids = new TreeSet<Integer>();
		for (Allergen allergen : allergens.values()) {
			remoteAids.add(allergen.aid);
		}
		try {
			BufferedReader reader = postData("getInfo", "aids", remoteAids);
			JSONObject array = new JSONObject(reader.readLine());
			reader.close();
			return array;
		} catch (JSONException e) { // usually happens with empty reply
			e.printStackTrace();
			return null;
		}
	}

	public static boolean setInfo(String deviceid, TreeMap<Integer, TreeMap<Long, Integer>> containments) throws IOException {
		Log.d(TAG, "RemoteDatabase.setInfo(...)");
		if (deviceid == null || deviceid.isEmpty()) {
			return false;
		}
		BufferedReader reader = postData("setInfo", "content", containments, deviceid);
		String reply=reader.readLine();
		reader.close();
		System.out.println(reply);
		return reply.equals("ENABLE");
	}

}
