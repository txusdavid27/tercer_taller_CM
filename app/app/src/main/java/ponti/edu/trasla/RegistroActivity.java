package ponti.edu.trasla;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import ponti.edu.trasla.databinding.ActivityRegistroBinding;
import ponti.edu.trasla.model.Usuario;

public class RegistroActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{

    /*Binding*/
    ActivityRegistroBinding binding;
    private FirebaseAuth mAuth;

    /*LOCATION*/
    private static final int ACCESS_LOCATION_ID = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LatLng myLocation;
    private SensorManager sensorManager;
    private Boolean accesoGPS=true;


    /*FOTO DE PERFIL*/
    private final int CAMERA_PERMISSION_ID = 101;
    private final int GALLERY_PERMISSION_ID = 102;
    String cameraPerm = Manifest.permission.CAMERA;
    String galPerm = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static boolean accessCamera = false, accessGal = false;

    /*FIREBASE VALUES*/
    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private Bitmap foto;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    ////image path
    private Uri filePath;
    private static final String PATH_IMAGE = "images/";
    //https://www.geeksforgeeks.org/android-how-to-upload-an-image-on-firebase-storage/

    /**
     * COPIAR TODA LA FUNCIÓN DE UPLOAD.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //////
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //////
        binding.botonRegistrar.setEnabled(false);
        //////
        mAuth = FirebaseAuth.getInstance();
        database= FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        myRef= database.getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        ////

        /**
         * Botones de Captura de Imágen.
         */
        binding.btnImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessGal = requestPermission(RegistroActivity.this, galPerm, "Permiso para utilizar la galeria", GALLERY_PERMISSION_ID);
                if(accessGal){
                    startGallery();
                }
            }
        });

        binding.btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessCamera = requestPermission(RegistroActivity.this, cameraPerm, "Permiso para utilizar la camara", CAMERA_PERMISSION_ID);
                if(accessCamera){
                    startCamera();
                }
            }
        });

        /**
         * Opciones de Localización.
         */
        mLocationRequest = createLocationRequest();
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if(location != null){
                    usePermission();
                }
            }
        };

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        solicitPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, "Permission to Access Location", ACCESS_LOCATION_ID);
        usePermission();

        /**
         * Botón de Registro y proceso de validación.
         */
        binding.botonRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
                myRef=database.getReference("message");
                myRef.setValue("Registro Completo.");
            }
        });


    }

    private boolean requestPermission(Activity context, String permission, String justification, int id){
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.CAMERA)) {
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(context, new String[]{permission}, id);
            return false;
        }
        return true;
    }

    private void startCamera(){
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(pictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(pictureIntent, CAMERA_PERMISSION_ID);
        }
    }

    public void startGallery(){
        Intent pickGalleryImage = new Intent(Intent.ACTION_PICK);
        pickGalleryImage.setType("image/*");
        startActivityForResult(pickGalleryImage, GALLERY_PERMISSION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_PERMISSION_ID: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startCamera();
                } else {
                    Toast.makeText(getApplicationContext(), "Access denied to camera", Toast.LENGTH_LONG).show();
                }
                break;
            }

            case GALLERY_PERMISSION_ID: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startGallery();
                } else {
                    Toast.makeText(getApplicationContext(), "Access denied to image gallery", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    /**
     * VALIDAR TODO CAMPO.
     * @return
     */
    private boolean validateForm() {
        boolean valid = true;
        String email = binding.inputEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            binding.inputEmail.setError("Required.");
            valid = false;
        } else {
            if(isEmailValid(binding.inputEmail.getText().toString())){
                binding.inputEmail.setError(null);
            }else{
                binding.inputEmail.setError("Dirección Erronea.");
            }
        }
        String password = binding.inputPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            binding.inputPassword.setError("Required.");
            valid = false;
        } else {
            binding.inputPassword.setError(null);
        }
        String nombre = binding.inputNombre.getText().toString();
        if (TextUtils.isEmpty(nombre)) {
            binding.inputNombre.setError("Required.");
            valid = false;
        } else {
            binding.inputNombre.setError(null);
        }
        String apellido = binding.inputApellido.getText().toString();
        if (TextUtils.isEmpty(apellido)) {
            binding.inputApellido.setError("Required.");
            valid = false;
        } else {
            binding.inputApellido.setError(null);
        }

        String identificacion = binding.inputID.getText().toString();
        if (TextUtils.isEmpty(identificacion)) {
            binding.inputID.setError("Required.");
            valid = false;
        } else {
            binding.inputID.setError(null);
        }

        return valid;
    }


    /**
     * SOLO CORREO
     * @param email
     * @return
     */
    private boolean isEmailValid(String email) {
        if (!email.contains("@") ||
                !email.contains(".") ||
                email.length() < 5)
            return false;
        return true;
    }

    public static final String PATH_USERS="users/";



    private void registerUser() {
        if (validateForm()) {
            mAuth.createUserWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
// Sign in success, update UI
                                Log.d("INFO", "Register:success");
                                FirebaseUser user = mAuth.getCurrentUser();

                                if(user!=null){ //Update user Info
                                    UserProfileChangeRequest.Builder upcrb = new UserProfileChangeRequest.Builder();
                                    upcrb.setDisplayName(binding.inputNombre.getText().toString()+" "+binding.inputApellido.getText().toString());
                                    user.updateProfile(upcrb.build());
                                    updateUI(user);

                                    //ATRIBUTOS-EXTRA
                                    Usuario myUser = new Usuario();
                                    myUser.setNombre(binding.inputNombre.getText().toString());
                                    myUser.setApellido(binding.inputApellido.getText().toString());
                                    myUser.setId(Integer.parseInt(binding.inputID.getText().toString()));
                                    myUser.setDisponible(true);
                                    try{
                                        myUser.setLatitud(myLocation.latitude);
                                        myUser.setLongitud(myLocation.longitude);
                                    }catch (Exception e){}
                                    //myUser.setKey(user.getUid());
                                    myRef=database.getReference(PATH_USERS+user.getUid());
                                    myRef.setValue(myUser);
                                    //Foto:

                                    StorageReference imgRef = storageReference.child(PATH_IMAGE + user.getUid() + "/" + "profile.png");
                                    imgRef.putBytes(uploadImage()).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                        {
                                            Toast.makeText(RegistroActivity.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e)
                                        {
                                            Toast.makeText(RegistroActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                updateUI(user);
                            } else {
                                Log.w("INFO", "signInWithEmail:failure", task.getException());
                                Toast.makeText(RegistroActivity.this, "Register failed.",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        }
    }


    private byte[] uploadImage(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        foto.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private void updateUI(FirebaseUser currentUser){
        if(currentUser!=null){
            Intent intent = new Intent(getBaseContext(), HomeActivity.class);
            intent.putExtra("user", currentUser.getEmail());
            startActivity(intent);
        } else {
            binding.inputEmail.setText("");
            binding.inputPassword.setText("");
            binding.textView7.setText("Usuario o Contraseña \nINCORRECTOS");
        }
    }

    private void activarBotonRegistro(){
        if(accesoGPS){
            binding.botonRegistrar.setEnabled(true);
            binding.botonRegistrar.setTextColor(Color.parseColor("#000000"));
        }else{
            Toast.makeText(getApplicationContext(), "ACTIVAR UBICACIÓN PARA REGISTRO", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * CAPTURA DE IMAGEN EN GALERIA O CAMARA.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case CAMERA_PERMISSION_ID: {
                if(resultCode == RESULT_OK){
                    Bundle extras = data.getExtras();
                    /**
                     * IMPORTANTE: AQUÍ QUEDA LA IMAGEN.
                     */
                    filePath=data.getData();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    /**
                     * Éste valor Bitmap se guarda en firebase
                     **/
                    foto=imageBitmap;
                    /****/
                    binding.ivCamara.setImageBitmap(imageBitmap);
                    activarBotonRegistro();
                }
                break;
            }
            case GALLERY_PERMISSION_ID: {
                if(resultCode == RESULT_OK){
                    try{
                        final Uri imageUri = data.getData();
                        final InputStream is = getContentResolver().openInputStream(imageUri);
                        final Bitmap selected = BitmapFactory.decodeStream(is);
                        /**
                         * IMPORTANTE: AQUÍ QUEDA LA IMAGEN.
                         */
                        filePath=data.getData();
                        /**
                         * Éste valor Bitmap se guarda en firebase
                         **/
                        foto=selected;
                        /***/
                        binding.ivCamara.setImageBitmap(selected);
                        activarBotonRegistro();
                    }catch(FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }

    private void usePermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if (location != null) {
                        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        binding.txtGPS.setText("Lat: "+myLocation.latitude+"  Lon: "+myLocation.longitude);
                    }else{
                        myLocation = new LatLng(4.6269938175930525, -74.06389749953162);
                    }
                }
            });
        }
    }

    //Código vital:

    protected LocationRequest createLocationRequest(){
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    private void solicitPermission(Activity context, String permit, String justification, int id) {
        if (ContextCompat.checkSelfPermission(context, permit) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permit)) {
                Toast.makeText(this, justification, Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(context, new String[]{permit}, id);
        }
    }


    /**
     * void
     */
    private void startLocationUpdates(){
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    /**
     * void
     */
    private void stopLocationUpdates(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * void
     */
    public void settingsLocation(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode){
                    case CommonStatusCodes.RESOLUTION_REQUIRED: {
                        try{
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult( RegistroActivity.this, REQUEST_CHECK_SETTINGS);
                        }catch(IntentSender.SendIntentException sendEx) {}
                        break;
                    }

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                        break;
                    }
                }
            }
        });
    }


    @Override
    protected void onResume(){
        super.onResume();
        settingsLocation();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
    }
}