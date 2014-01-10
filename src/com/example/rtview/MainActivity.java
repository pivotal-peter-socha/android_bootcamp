package com.example.rtview;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends Activity {
	
	private String base_url = "http://api.rottentomatoes.com/api/public/v1.0/movies.json?page_limit=10&page=1&apikey=g6cqn75znnpt294mvqbbaxsz";
	private String box_office_url = "http://api.rottentomatoes.com/api/public/v1.0/lists/movies/box_office.json?limit=16&country=us&apikey=g6cqn75znnpt294mvqbbaxsz";
	private String url = base_url;
	private ListView listview;
	private static final String DEBUG_TAG = "HttpExample";
	RefreshThread refreshThread = new RefreshThread();
	private Button button;
	private EditText searchfield;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listview = (ListView) findViewById(R.id.listView1);
		
		button = (Button) findViewById(R.id.button1);
		searchfield = (EditText) findViewById(R.id.editText1);
		
		url = box_office_url;
		
		if (checkConnection()) {		
			new DownloadWebpageTask().execute(url);		
        }
		
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	refreshThread.isStop = true;
            	
            	hideSoftKeyboard();
                
                if (checkConnection()) {
                    	
                    	String searchText = searchfield.getText().toString();
                    	
                    	Log.d(DEBUG_TAG, "The search string is " + searchText);
                    	 
                    	if (searchText.length() == 0) {
                    		url = box_office_url;
                    	}
                    	else {
                    		url = base_url + "&q=" + searchText.replace(" ", "+");
                    	}
                    	
                        new DownloadWebpageTask().execute(url);
                        
                    } else {
                    	Toast.makeText(getApplicationContext(), "Network connection failed", Toast.LENGTH_LONG).show();
                    }  
                
                refreshThread.isStop = false;
            	
            }
        });
		
        
        refreshThread.isStop = false;
        refreshThread.start();
            
    }
	
	protected void onStop() {
		super.onStop();
		refreshThread.isStop = true;
	}
	
	protected void onPause() { 
		super.onPause();
		refreshThread.isStop = true;
	}

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
    	
		List<String> titles = new ArrayList<String>();
		List<String> descriptions = new ArrayList<String>();
		List<Bitmap> images = new ArrayList<Bitmap>();
    	
        protected String doInBackground(String... urls) {  
            try {
            	
                String json_data =  downloadUrl(urls[0]);   
                
                JSONObject jObject = new JSONObject(json_data);				
				
				JSONArray movies = jObject.getJSONArray("movies");
								
				for (int i = 0; i < movies.length(); i++) {
					JSONObject currentMovie = new JSONObject(movies.getString(i));
					
					titles.add(currentMovie.getString("title"));
					
					if (currentMovie.has("critics_consensus")) {
						descriptions.add(currentMovie.getString("critics_consensus"));		
					}
					else {
						descriptions.add("");
					}
					
					URL imgurl = new URL(currentMovie.getJSONObject("posters").getString("thumbnail"));
					Bitmap bmp = BitmapFactory.decodeStream(imgurl.openConnection().getInputStream());
					images.add(bmp);
					
				}
                
                return "Download successful";
                
            } catch (IOException e) {
				e.printStackTrace();
				return "download failed";
			} catch (JSONException e) {
				e.printStackTrace();
				return "download failed";
			}
    }
     
        protected void onPostExecute(String result) {
        	
        	CustomList adapter = new CustomList(MainActivity.this, titles, descriptions, images);
	        listview.setAdapter(adapter);
	        
	      Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
	      listview.startAnimation(animation);  

        }
    }
    
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
            
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            String contentAsString = readIt(is);
            return contentAsString;
            
        } finally {
            if (is != null) {
                is.close();
            } 
        }
    }
    
    public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
    	
    	Scanner s = new Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    
    public class CustomList extends ArrayAdapter<String>{
    	
    	private final Activity context;
    	private final List<String> title;
    	private final List<Bitmap> image;
    	private final List<String> description;
    	
    	public CustomList(Activity context, List<String> web, List<String> desc, List<Bitmap> imageId) {
    		super(context, R.layout.list_item, web);
    		this.context = context;
    		this.title = web;
    		this.image = imageId;
    		this.description = desc;
    	}
    	
    	@Override
    	public View getView(int position, View view, ViewGroup parent) {
    		LayoutInflater inflater = context.getLayoutInflater();
    		View rowView= inflater.inflate(R.layout.list_item, null, true);
    		TextView txtTitle = (TextView) rowView.findViewById(R.id.title);
    		TextView criticsconsensus = (TextView) rowView.findViewById(R.id.desc);
    		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);   		
    		
    		txtTitle.setText(title.get(position));
    		criticsconsensus.setText(description.get(position));    		
            imageView.setImageBitmap(image.get(position));  
    		
    		return rowView;
    	}
    }
    
    private class RefreshThread extends Thread{
    	public boolean isStop = false;
    	
        public void run(){
            try{
                while(!isStop){                                             
                    MainActivity.this.runOnUiThread(new Runnable() {                   
                        @Override
                        public void run() {
                        	new DownloadWebpageTask().execute(url);
                        }
                    });                         
                    Thread.sleep(30 * 1000);
                }
        }catch(Exception e){}
      }
    }  
    
    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }
    
    private boolean checkConnection() {
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
        	return true;
        }
        else {
        	return false;
        }
    }
    
}

    


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

