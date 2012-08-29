package org.srsoftware.allergyscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.TreeMap;

import android.util.Log;

public class RemoteDatabase {
	protected static String TAG="AllergyScan";
	private static String adress="http://allergy.srsoftware.de/";
	private static String allergenList="listAllergens.php";

	
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


	public static void storeAllergen(String allergen) {
		// TODO Auto-generated method stub
		Log.d(TAG, "here, \""+allergen+"\" should be stored");
	}

}
