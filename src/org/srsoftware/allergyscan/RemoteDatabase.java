package org.srsoftware.allergyscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.TreeMap;

import android.util.Log;

public class RemoteDatabase {
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de/";
	private static String allergenList="listAllergens.php";
	private static String create="create.php?device=";
	private static String check="checkValidation.php?device=";
	private static String UNICODE="UTF-8";
	
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
		URL url=new URL(adress+create+MainActivity.deviceid+"&allergen="+URLEncoder.encode(allergen,UNICODE));
		//Log.d(TAG, url.toString());
		BufferedReader reader=new BufferedReader(new InputStreamReader(url.openStream()));
		reader.close();
	}


	public static Integer storeProduct(String productCode, String productName) throws IOException {
		URL url=new URL(adress+create+MainActivity.deviceid+"&code="+productCode+"&product="+URLEncoder.encode(productName,UNICODE));
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
		String line=reader.readLine();
		Log.d(TAG, ""+line);
		result=line.trim().equals("enabled");
		reader.close();
		return result;
  }

}
