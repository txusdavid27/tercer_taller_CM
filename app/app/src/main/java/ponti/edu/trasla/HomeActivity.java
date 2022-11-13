package ponti.edu.trasla;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ponti.edu.trasla.model.Ubicacion;
import ponti.edu.trasla.model.Usuario;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String PATH_USERS = "users/";

    private GoogleMap mMap;
    private Switch swDisp;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private Usuario data;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private List<Ubicacion> locations;

    private static final int REQUEST_CHECK_SETTINGS = 99;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private String justificacion = "Se necesita el GPS para mostrar la ubicación del evento";

    private static String CHANNEL_ID = "MyApp";
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar mt = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mt);

        getSupportActionBar().setTitle("Home");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        locations = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }
        updateCurrentPosition();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();

        createNotificationChannel();
        Intent intent = new Intent(HomeActivity.this, NotificationFirebaseIntentService.class);
        NotificationFirebaseIntentService.enqueueWork(HomeActivity.this, intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        initCurrentUser(user);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            initLocations();
            mMap.setMyLocationEnabled(true);
            try {
                MapsInitializer.initialize(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateCurrentPosition();
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(4.65, -74.05), 12));

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode){
                    case CommonStatusCodes
                            .RESOLUTION_REQUIRED:
                        try{
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(HomeActivity.this,REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes
                            .SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    public static void request(final Activity activity, final String permissionCode , String justificacion, final int idCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                permissionCode)) {

            new AlertDialog.Builder(activity)
                    .setTitle("Se necesita un permiso")
                    .setMessage(justificacion)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{permissionCode}, idCode);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{permissionCode}, idCode);
        }
    }

    private void requestPermission(){

        request(this,
                Manifest.permission.ACCESS_FINE_LOCATION,
                justificacion,
                LOCATION_PERMISSION_CODE);

        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    startLocationUpdates();
                }
            });
        }
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    public void updateCurrentPosition(){
        mLocationCallback = new LocationCallback(){
            public void onLocationResult(LocationResult locationResult){
                Location location = locationResult.getLastLocation();
                if(location != null && data != null){
                    swDisp.setChecked(data.getDisponible());
                    LatLng myLocation = new LatLng(data.getLatitud(), data.getLongitud());
                    if(mMap != null){
                        mMap.clear();
                        initLocations();
                        mMap.addMarker(new MarkerOptions().position(myLocation).title("Current location").snippet("My Home").alpha(0.75f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        if(location.getLatitude() != data.getLatitud() && location.getLongitude() != data.getLongitud()){
                            data.setLatitud(location.getLatitude());
                            data.setLongitud(location.getLongitude());
                            mRef = mDatabase.getReference(PATH_USERS + user.getUid() + "/" + "latitud");
                            mRef.setValue(location.getLatitude());
                            mRef = mDatabase.getReference(PATH_USERS + user.getUid() + "/" + "longitud");
                            mRef.setValue(location.getLongitude());
                            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));
                        }
                    }
                }
            }
        };
    }

    private void initLocations() {
        try{
            JSONObject json = new JSONObject(loadJSONFromAsset());
            JSONArray loc = json.getJSONArray("locations");
            for(int i = 0; i < loc.length(); i++) {
                JSONObject jsonO = loc.getJSONObject(i);
                Ubicacion newLoc = new Ubicacion(jsonO.getString("name"), Double.parseDouble(jsonO.getString("latitud")), Double.parseDouble(jsonO.getString("longitud")));
                LatLng ub = new LatLng(newLoc.getLatitude(), newLoc.getLongitude());
                mMap.addMarker(new MarkerOptions().position(ub).title(newLoc.getName()).alpha(0.75f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                locations.add(newLoc);
            }

        }catch(JSONException ex){
            ex.printStackTrace();
        }
    }

    public String loadJSONFromAsset(){
        String json = null;
        try{
            InputStream is = this.getAssets().open("locations.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                if (resultCode == RESULT_OK) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this,
                            "Sin acceso a localización, hardware deshabilitado!", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem disp = menu.findItem(R.id.menuState);
        swDisp = (Switch) disp.getActionView();

        swDisp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(swDisp.isChecked()){
                    mRef = mDatabase.getReference(PATH_USERS + user.getUid() + "/" + "disponible");
                    mRef.setValue(true);
                } else {
                    mRef = mDatabase.getReference(PATH_USERS + user.getUid() + "/" + "disponible");
                    mRef.setValue(false);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menuLogOut) {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if(id == R.id.menuUsuarios){
            Intent intent = new Intent(HomeActivity.this, DisponiblesActivity.class);
            startActivity(intent);
        }
        return true;
    }

    public void initCurrentUser(FirebaseUser user){
        if(user != null){
            mRef = mDatabase.getReference(PATH_USERS + user.getUid());
            mRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    data = dataSnapshot.getValue(Usuario.class);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(HomeActivity.this, "Data retriving failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}