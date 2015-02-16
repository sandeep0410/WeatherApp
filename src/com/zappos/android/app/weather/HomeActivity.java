package com.zappos.android.app.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity implements LocationListener{
	TextView mlocation;
	TextView mDateText;
	TextView mTemperature;
	TextView mHumidity;
	TextView mPressure;
	TextView mDesc;
	ImageView mWeatherIcon;
	ProgressDialog dialog;
	TextView mMaxTemp;
	TextView mMinTemp;

	LocationManager locationManager;	
	Handler handler;
	private static HomeActivity _instance;

	public static HomeActivity getInstance(){
		if(_instance==null)
			_instance = new HomeActivity();
		return _instance; 

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mlocation = (TextView)findViewById(R.id.location);
		mDateText = (TextView)findViewById(R.id.date_time);
		mTemperature = (TextView)findViewById(R.id.temp);
		mHumidity = (TextView)findViewById(R.id.humidity);
		mPressure = (TextView)findViewById(R.id.pressure);
		mDesc = (TextView)findViewById(R.id.desc);
		mWeatherIcon = (ImageView)findViewById(R.id.weather_icon);
		mMaxTemp = (TextView)findViewById(R.id.max_temp);
		mMinTemp = (TextView)findViewById(R.id.min_temp);
		handler = new Handler();

		dialog = new ProgressDialog(HomeActivity.this);
		dialog.setMessage("Loading Weather Data...");
		getCurrentLocationUpdates();

	}

	@Override
	public void onLocationChanged(Location location) {
		try {
			Geocoder gc = new Geocoder(this, Locale.getDefault());
			List<Address> loc  = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			mlocation.setText(loc.get(0).getAddressLine(1));
			updateWeatherData(loc.get(0).getAddressLine(1));
			stopUpdatingLocation();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);	    
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.cur_loc:
			getCurrentLocationUpdates();
			break;
		case R.id.cus_loc:
			EnterCityDialog dialog = new EnterCityDialog();
			dialog.show(getFragmentManager(), "dialog");
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {


	}

	@Override
	public void onProviderEnabled(String provider) {


	}

	@Override
	public void onProviderDisabled(String provider) {


	}

	private void getCurrentLocationUpdates(){
		boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if(gps_enabled){

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10, this);
			dialog.show();
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					long cur = System.currentTimeMillis();				
					while(System.currentTimeMillis() - cur<10000){}
					stopUpdatingLocation();
				}
			});
			t.start();
		}else{
			EnterCityDialog dialog = new EnterCityDialog(getResources().getString(R.string.location_disabled), getResources().getString(R.string.enter_city));
			dialog.show(getFragmentManager(), "dialog");
		}
	}

	protected void stopUpdatingLocation() {
		locationManager.removeUpdates(this);
		dialog.dismiss();
	}


	private void updateWeatherData(final String city){
		dialog.show();
		new Thread(){
			public void run(){

				final JSONObject json = getJSONfile(city);
				if(json == null){
					handler.post(new Runnable(){
						public void run(){
							Toast.makeText(HomeActivity.this, getResources().getString(R.string.place_not_found), 
									Toast.LENGTH_LONG).show(); 
						}
					});
					dialog.dismiss();
				} else {
					handler.post(new Runnable(){
						public void run(){
							updateUI(json);
						}
					});
				}               
			}
		}.start();
	}
	
/*	JSON output sample:
 * {"coord":{"lon":139,"lat":35},
		"sys":{"country":"JP","sunrise":1369769524,"sunset":1369821049},
		"weather":[{"id":804,"main":"Clouds","description":"overcast clouds","icon":"04n"}],
		"main":{"temp":289.5,"humidity":89,"pressure":1013,"temp_min":287.04,"temp_max":292.04},
		"wind":{"speed":7.31,"deg":187.002},
		"rain":{"3h":0},
		"clouds":{"all":92},
		"dt":1369824698,
		"id":1851632,
		"name":"Shuzenji",
		"cod":200}*/
	private void updateUI(JSONObject json){
		Log.d("sandeep","dtaa: " +json);
		try {
			
			JSONObject weatherJSON = json.getJSONArray("weather").getJSONObject(0);
			JSONObject mainJSON = json.getJSONObject("main");
			String dateString = "Last updated: " +DateFormat.getDateTimeInstance().format(new Date(json.getLong("dt")*1000));			
			String description = weatherJSON.getString("description").toUpperCase(Locale.US);
			String cityName = json.getString("name").toUpperCase(Locale.US) + ", " +json.getJSONObject("sys").getString("country");
			String pressure = mainJSON.getString("pressure") + " hPa";
			String temperature = String.format("%.2f", (32+1.8*mainJSON.getDouble("temp")))+ " ºF";			
			String humidity =  mainJSON.getString("humidity") + "%" ;
			String maxTemp = String.format("%.2f", (32+1.8*mainJSON.getDouble("temp_max")))+ " ºF";
			String minTemp = String.format("%.2f", (32+1.8*mainJSON.getDouble("temp_min")))+ " ºF";
		
			updateDetailsonUI(dateString,
					description,
					cityName,
					pressure,
					temperature,
					humidity,
					maxTemp,
					minTemp);
			setImageWeather(weatherJSON.getInt("id"),json.getJSONObject("sys").getLong("sunrise") * 1000, json.getJSONObject("sys").getLong("sunset") * 1000);

		}catch(Exception e){
			Log.e("sandeep", "One or more fields not found in the JSON data");
		}finally{
			dialog.dismiss();
		}
	}

	private void updateDetailsonUI(String dateString, String description,
			String cityName, String pressure, String temperature,
			String humidity, String maxTemp, String minTemp) {
		mDateText.setText(dateString);
		mDesc.setText(description);
		mlocation.setText(cityName);
		mPressure.setText(pressure);
		mTemperature.setText(temperature);
		mHumidity.setText(humidity);
		mMaxTemp.setText(maxTemp);
		mMinTemp.setText(minTemp);		
	}

	public static JSONObject getJSONfile(String city){
		try {
			URL url = new URL(String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric", city));           
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.addRequestProperty("x-api-key", "12345");
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer sb = new StringBuffer(1024);
			String temp="";
			while((temp=reader.readLine())!=null){
				sb.append(temp).append("\n");
			}
			reader.close();
			JSONObject result = new JSONObject(sb.toString());
			if(result.getInt("cod") != 200){
				return null;
			}
			return result;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	} 
	private class EnterCityDialog extends DialogFragment{
		String title;
		String body ;
		public EnterCityDialog(String title, String body) {
			this.title = title;
			this.body = body;
		}
		public  EnterCityDialog() {
			title = getApplicationContext().getResources().getString(R.string.enter_city);
			body = " ";
		}
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(title);
			builder.setMessage(body);
			final EditText input = new EditText(getApplicationContext());
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			input.setTextColor(getResources().getColor(android.R.color.black));
			builder.setView(input);		    
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					updateWeatherData(input.getText().toString());
				}
			});

			return builder.create();
		}
	}


	private void setImageWeather(int id, long day, long night ) {
		// TODO Auto-generated method stub
		long curTime = new Date().getTime();
		if(id == 800 ){
			if(curTime>=day && curTime<night)
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d01));
			else
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.n01));
			return;
		}

		if(id == 801 ){
			if(curTime>=day && curTime<night)
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d02));
			else
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.n02));
			return;
		}

		switch(id/100) {
		case 2: 
			mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d11));
			break;
		case 3:
			mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d09));
			break;
		case 5:
			switch(id/10){
			case 50:
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d10));
				break;
			case 51:
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d13));
				break;
			default:
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d09));
			}
			break;
		case 6:
			mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d09));	
			break;
		case 7:
			mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d50));
			break;
		case 8:
			switch(id){
			case 802:
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d03));
				break;
			case 803:
			case 804:
			default:
				mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d04));	
			}
			break;
		default:
			mWeatherIcon.setBackground(getResources().getDrawable(R.drawable.d50));	
		}
	}
}
