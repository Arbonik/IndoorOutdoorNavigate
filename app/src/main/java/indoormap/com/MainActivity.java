package indoormap.com;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.OnEngineInitListener;
//import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapPolygon;
import com.here.android.mpa.mapping.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private SupportMapFragment mapFragment = null;

    /**
     * permissions request code
     */
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }
    private void initialize() {
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);

        // Set up disk cache path for the map service for this application
        // It is recommended to use a path under your application folder for storing the disk cache
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "{indoortestservice}"); /* ATTENTION! Do not forget to update {YOUR_INTENT_NAME} */

        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();
                        // Set the map center to the Vancouver region (no animation)
                        map.setCenter(new GeoCoordinate(53.339700, 83.768700, 0.0),
                                Map.Animation.NONE);
                        // Set the zoom level to the average between min and max
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                        requestIndoorLayer();

                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");
                    }
                }
            });
        }
    }
    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

     public void requestIndoorLayer () {
        String indoorUrl = "https://xyz.api.here.com/hub/spaces/OSAkxUKL/iterate?access_token=AaqZ9Nh_p2S2ETqZeT483XA";
                RequestQueue queue = Volley.newRequestQueue(this);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, indoorUrl,
                        new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.O )
                            @Override
                            public void onResponse(String response) {
                                JSONObject jsonObject = null;

                                try {


                                    
                                    jsonObject = new JSONObject(response);
                                }

                                catch (JSONException e){
                                    e.printStackTrace();
                                }
                                try {
                                    JSONArray jsonArray = jsonObject.getJSONArray("features");

                                    String coords = "";

                                    for(int i = 0; i <jsonArray.length(); i++) {
                                        JSONObject jo = jsonArray.getJSONObject(i);
                                        JSONObject geometry = jo.getJSONObject("geometry");
                                        String geometryType = geometry.getString("type");

                                        JSONArray coordsArray = geometry.getJSONArray("coordinates").getJSONArray(0);
                                        JSONArray venue  = coordsArray.getJSONArray(0);

                                        List<GeoCoordinate> testPoints = new ArrayList<>();
                                        for (int k = 0; k < venue.length(); k++) {
                                            JSONArray points = venue.getJSONArray(k);
                                            double latitude = (Double) points.get(1);
                                            double longitude = (Double) points.get(0);

                                            coords += " " + points.get(1).toString();

                                            testPoints.add(new GeoCoordinate(latitude, longitude, 0));
                                        }

                                        GeoPolygon polygon = new GeoPolygon(testPoints);
                                        MapPolygon mapPoligon = new MapPolygon(polygon);

                                        mapPoligon.setLineColor(Color.RED);

                                        mapPoligon.setFillColor(Color.argb(0.2f,6.0f,184.0f, 124.0f));

                                        map.addMapObject(mapPoligon);
                                    }
                                    //textView.setText(coords);
                                    Log.d("COORDUNATE", coords.toString());
                                }catch (JSONException e) {
                                    e.printStackTrace();
                                }
                        }
                }, new Response.ErrorListener(){
                    @Override
            public  void onErrorResponse(VolleyError error){
                        //textView.setText("That didnt work!");
                        Log.d("ERROR","onErrorResponse");
                    }
                        });
queue.add(stringRequest);
    }
}





