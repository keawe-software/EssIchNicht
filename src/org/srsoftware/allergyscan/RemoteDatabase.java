package org.srsoftware.allergyscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.ContentValues;
import android.util.Log;

public class RemoteDatabase {
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de/";
	private static String allergenList="listAllergens.php";
	private static String create="create.php?device=";
	private static String check="checkValidation.php?device=";
	private static String update="update.php?";
	private static String UNICODE="UTF-8";
	static Integer missingCredits=null;
	private static String getproductname="getproduct.php?barcode=";
	
	public static TreeMap<Integer, String> getAvailableAllergens() throws IOException {
		Log.d(TAG, "getAvailableAllergens");
		URL url=new URL(adress+allergenList);
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
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
		URL url=new URL(adress+create+MainActivity.deviceid+"&allergen="+encode(allergen));
		//Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		reader.close();
	}


	public static Integer storeProduct(String productCode, String productName) throws IOException {
		URL url=new URL(adress+create+MainActivity.deviceid+"&code="+productCode+"&product="+encode(productName));
		//Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		String line=null;
		Integer result=null;
		if ((line=reader.readLine())!=null) result=Integer.parseInt(line.trim());
		reader.close();
		return result;
	}


	public static void storeAllergenInfo(int allergenId, Integer productId, boolean b) throws IOException {
		URL url=new URL(adress+create+MainActivity.deviceid+"&aid="+allergenId+"&pid="+productId+"&contained="+b);
		Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		reader.close();
	}


	public static boolean deviceEnabled() throws IOException {
		URL url=new URL(adress+check+MainActivity.deviceid);
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		
		boolean result=false;
		String line=reader.readLine();
		if (line==null) return result;
		line=line.trim();
		try {
			missingCredits = 10;
			missingCredits = Integer.parseInt(line);
		} catch (NumberFormatException nfe){			
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


	private static void updateContent(Set<Integer> myAllergens, AllergyScanDatabase database) throws IOException {
		Log.d(TAG, "updateContent");
		int lastCID=database.getLastCID();		
		URL url=new URL(adress+update+"cid="+lastCID+"&allergens="+encode(myAllergens));		
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
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


	private static String encode(Object o) throws UnsupportedEncodingException {
	  return URLEncoder.encode(o.toString().replace(", ",","),UNICODE);
  }


	private static void updateProducts(Set<Integer> myAllergens, AllergyScanDatabase database) throws NumberFormatException, IOException {
		Log.d(TAG, "updateProducts");
		TreeSet<Integer> existingPIDs=database.getAllPIDs();
		TreeSet<Integer> referencedPIDs=database.getReferencedPIDs();
		referencedPIDs.removeAll(existingPIDs);
		if (referencedPIDs.isEmpty()) return;
		URL url=new URL(adress+update+"pids="+encode(referencedPIDs));
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
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


	public static String getProductName(String productBarCode) throws IOException {		
		Log.d(TAG, "getProductName");
		URL url=new URL(adress+getproductname+productBarCode);
		Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		String line=null;
		if ((line=reader.readLine())!=null) {
			line=line.trim();
			if (line.length()<1) line=null;
		}
		reader.close();		
		return line;
	}

}
