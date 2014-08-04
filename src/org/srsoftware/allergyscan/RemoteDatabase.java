package org.srsoftware.allergyscan;

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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.ContentValues;
import android.util.Log;

public class RemoteDatabase {
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de?action=";
	private static String UNICODE="UTF-8";
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

	public static void storeAllergen(String allergen) throws IOException {		
		TreeMap<String,String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put("allergen", URLEncoder.encode(allergen,UNICODE));
		data.put("device",MainActivity.deviceid);
		//Log.d(TAG, url.toString());
		BufferedReader reader=postData("createAllergen", data);
		reader.close();
	}

	public static Integer storeProduct(String productCode, String productName) throws IOException {
		TreeMap<String,String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put("code", productCode);
		data.put("product", URLEncoder.encode(productName, UNICODE));
		data.put("device",MainActivity.deviceid);		
		BufferedReader reader=postData("createProduct", data);
		String line=null;
		Integer result=null;
		if ((line=reader.readLine())!=null) result=Integer.parseInt(line.trim());
		reader.close();
		return result;
	}


	public static void storeAllergenInfo(int allergenId, Integer productId, boolean contained) throws IOException {
		Log.d(TAG, "storeAllergenInfo");
		TreeMap<String, String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put("device", MainActivity.deviceid);
		data.put("aid", ""+allergenId);
		data.put("pid", ""+productId);
		data.put("contained", contained?"1":"0");		
		BufferedReader reader=postData("storeinfo", data);
		reader.close();
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
	
	private static BufferedReader postData(String action,String key,String value) throws IOException{
		TreeMap<String, String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put(key,value);
		return postData(action, data);
	}

	public static boolean deviceEnabled() throws IOException {
		BufferedReader reader=postData("validate", "device", MainActivity.deviceid);		
		boolean result=false;
		String line=null;
		line=reader.readLine();		
		if (line==null) return result;		
		line=line.trim();

		try {
			missingCredits = 10; // initially, set to 10
			missingCredits = Integer.parseInt(line); // try to overwrite with parsed value
		} catch (NumberFormatException nfe){ // if parsing fails, missing credits will remain 10
			return false;
		}
		result=line.equals("0");
		reader.close();
		return result;
  }
	
	public static int missingCredits(){
		return missingCredits;
	}


	public static void update(AllergyScanDatabase database) throws IOException {
		Set<Integer> myAllergens = database.getAllergenList().keySet();
		updateContent(myAllergens,database);
		updateProducts(myAllergens,database);
  }

	

	/**
	 * this method retrieves all information on substance-in-products 
	 * @param myAllergens
	 * @param database
	 * @throws IOException
	 */
	private static void updateContent(Set<Integer> myAllergens, AllergyScanDatabase database) throws IOException {
		Log.d(TAG, "updateContent");
		int lastCID=database.getLastCID();		
		TreeMap<String, String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put("cid", ""+lastCID);		
		data.put("allergens", encodeArrayString(myAllergens));		
		BufferedReader reader=postData("update", data);
		String line=null;		
		if ((line=reader.readLine())!=null) {
			String[] keys = line.split("\t");
			int keyNumber=keys.length;
			while ((line=reader.readLine())!=null) {
				String[] dummy = line.split("\t");
				ContentValues values=new ContentValues();
				for (int index=0;index<keyNumber; index++){					
					String key=keys[index];
					int value=Integer.parseInt(dummy[index]);
					values.put(key, value);
				}
				database.updateContent(values);
			}
		}
		reader.close();		
  }


	private static String encodeArrayString(Set<? extends Object> array) throws UnsupportedEncodingException {
		return URLEncoder.encode(array.toString().replace(", ",",").replace("[", "").replace("]", ""),UNICODE);
	}

	private static void updateProducts(Set<Integer> myAllergens, AllergyScanDatabase database) throws NumberFormatException, IOException {
		Log.d(TAG, "updateProducts");
		TreeSet<Integer> existingPIDs=database.getAllPIDs(); // obtain the list of all pids known to the local database
		TreeSet<Integer> referencedPIDs=database.getReferencedPIDs(); // obtain the list of all pids referenced in the content table of the local database
		// as the content table may have been updated, there may be references to products that are not yet in the local products table
		referencedPIDs.removeAll(existingPIDs); // reduce the list of products to those that are unknown, yet
		if (referencedPIDs.isEmpty()) return;
		TreeMap<String, String> data=new TreeMap<String, String>(ObjectComparator.get());
		data.put("pids", encodeArrayString(referencedPIDs));		
		BufferedReader reader=postData("update", data);
		String line=null;		
		if ((line=reader.readLine())!=null) {
			String[] keys = line.split("\t");
			int keyNumber=keys.length;
			while ((line=reader.readLine())!=null) {
				String[] dummy = line.split("\t");
				ContentValues values=new ContentValues();
				Integer aid=null;
				Integer pid=null;
				for (int index=0;index<keyNumber; index++){					
					String key=keys[index];
					String value=dummy[index];
					try{
						int intValue=Integer.parseInt(value);
						if (key.equals("aid")) aid=intValue;
						if (key.equals("pid")) pid=intValue;
						values.put(key, intValue);
					} catch (NumberFormatException nfe){
						values.put(key, value);
					}					
				}
				database.removeContent(aid,pid);
				database.updateProducts(values);
			}
		}
		reader.close();		
		
  }


	public static ProductData getProduct(String productBarCode) throws IOException {		
		Log.d(TAG, "getProduct");
		BufferedReader reader=postData("getproduct", "barcode", productBarCode);
		String name=null;
		ProductData product=null;
		if ((name=reader.readLine())!=null) {
			name=name.trim();
			if (name.length()>0){
				String pidString=null;
				if ((pidString=reader.readLine())!=null){
					pidString=pidString.trim();
					int pid=Integer.parseInt(pidString);
					product=new ProductData(pid, productBarCode, name);
				}
			}
		}
		reader.close();		
		return product;
	}

}
