package org.srsoftware.allergyscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Set;
import java.util.TreeMap;

import android.util.Log;

public class RemoteDatabase {
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de/";
	private static String allergenList="listAllergens.php";
	private static String create="create.php?device=";
	private static String check="checkValidation.php?device=";
	private static String update="update.php?cid=";
	private static String UNICODE="UTF-8";
	private static Integer missingCredits=null;
	
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
		//Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		reader.close();
	}


	public static boolean deviceEnabled() throws IOException {
		URL url=new URL(adress+check+MainActivity.deviceid);
		Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		
		boolean result=false;
		String line=reader.readLine().trim();
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
		int lastCID=database.getLastCID();		
		URL url=new URL(adress+update+lastCID+"&allergens="+encode(myAllergens));
		Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		String line=null;
		String keys=null;
		
		// TODO: the following implementation is rather bad.
		// the values should be read into an ContentValues object and then passed to an insert method instead
		
		if ((line=reader.readLine())!=null) {
			keys=line.trim().replace("\t", ",");
		}
		while ((line=reader.readLine())!=null) {
			line=line.trim().replace("\t", ",");
			String query="INSERT INTO content ("+keys+") VALUES ("+line+")";
			Log.d(TAG, query);
			database.query(query);
		}
		reader.close();
  }


	private static String encode(Object o) throws UnsupportedEncodingException {
	  return URLEncoder.encode(o.toString(),UNICODE);
  }


	private static void updateProducts(Set<Integer> myAllergens, AllergyScanDatabase database) {
		int lastPID=database.getLastPID();	  
  }

}
