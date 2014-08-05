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
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de?action=";
	static Integer missingCredits=null;
	
	public static TreeMap<Integer, String> getAvailableAllergens() throws IOException {
		Log.d(TAG, "getAvailableAllergens");
		BufferedReader reader=postData("allergenList", null);
		String line;
		TreeMap<Integer, String> result=new TreeMap<Integer, String>();
		while ((line=reader.readLine())!=null){
			int space=line.indexOf(" ");
			if (space>-1){
				String name=line.substring(space).trim();
				int id=Integer.parseInt(line.substring(0,space).trim());
				result.put(id, name);
			}
		}
		reader.close();		
		return result;
	}
	
	private static BufferedReader postData(String action) throws IOException {
		return postData(action, null);
	}
	
	private static BufferedReader postData(String action, TreeMap<String, String> data) throws IOException{
		URL url=new URL(adress+action);
		Log.d(TAG, "trying to send POST data to "+url+":");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
    connection.setDoInput(true);

    if (data!=null && !data.isEmpty()){
      DataOutputStream out = new DataOutputStream(connection.getOutputStream());
      for (Iterator<Entry<String, String>> it=data.entrySet().iterator(); it.hasNext();){
    		Entry<String, String> entry = it.next();    	
    		out.writeBytes(entry.getKey()+"="+entry.getValue());
    		Log.d(TAG,entry.getKey()+"="+entry.getValue());
    		if (it.hasNext()){
    			out.write('&');
    		}
    	}    
    	out.close();
    }
    
    BufferedReader result=new BufferedReader(new InputStreamReader(connection.getInputStream()));
    return result;
	}
	
	private static BufferedReader postData(String action,String key,Object value) throws IOException{
		TreeMap<String, String> data=new TreeMap<String, String>(ObjectComparator.get());
		if (value instanceof TreeSet){
			data.put(key,createJsonArray((TreeSet<?>) value));
		} else if (value instanceof TreeMap){
			data.put(key, createJsonArray((TreeMap<?,?>) value));
		} else {
			data.put(key,value.toString());
		}
		return postData(action, data);
	}

	private static String createJsonArray(TreeSet<?> value) {
		return value.toString();//.replace('[', '{').replace(']','}');
	}
	
	private static String createJsonArray(TreeMap<?,?> value) throws UnsupportedEncodingException {
		StringBuffer result=new StringBuffer();
		if (value==null || value.isEmpty()){
			return "{}";
		}
		result.append('{');
		for (Entry<?, ?> entry:value.entrySet()){
			result.append("\""+encode(entry.getKey())+"\":\""+encode(entry.getValue())+"\",");			
		}
		result.deleteCharAt(result.length()-1);
		result.append('}');
		return result.toString();		
	}	

	private static String encode(Object text) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		return URLEncoder.encode(text.toString(),"UTF-8");
	}

	public static JSONObject getNewProducts() throws IOException, JSONException {
		try {
			Log.d(TAG,"RemoteDatabase.getNewProducts(...)");
			BufferedReader reader=postData("getNewProducts");		
			JSONObject array=new JSONObject(reader.readLine());
			reader.close();
			return array;
		} catch (JSONException e){ // usually happens with empty reply
			Log.e(TAG, e.getMessage());
			return null;
		}
	}

	public static void storeNewProducts(TreeMap<Long, String> products) throws IOException {
		Log.d(TAG, "RemoteDatabase.storeNewProducts(...)");
		if (products==null || products.isEmpty()){
			return;
		}
		BufferedReader reader=postData("storeNewProducts", "products", products);
		System.out.println(reader.readLine());
		reader.close();
	}

}
