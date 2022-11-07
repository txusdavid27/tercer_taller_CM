package ponti.edu.trasla;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import ponti.edu.trasla.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onResume() {
        binding.textView7.setText("TALLER#3");
        binding.inputUsername.setText("");
        binding.inputPassword.setText("");
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        try {
            int extras = getIntent().getFlags();
            Integer ie= extras;
            Log.i("FLAG_INTENT", ie.toString());
            mAuth.signOut();

        }catch (Exception e){}



        Toast.makeText(this, "Bienvenid@", Toast.LENGTH_SHORT).show();
        Log.i("APP", "Done");

        binding.textView6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this,"Registro...", Toast.LENGTH_SHORT).show();
                Log.i("APP", "Registro...");
                Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });

        binding.inputUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.textView7.setText("TALLER#3");
            }
        });

        binding.button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInUser(binding.inputUsername.getText().toString(), binding.inputPassword.getText().toString());
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    private void updateUI(FirebaseUser currentUser){
        if(currentUser!=null){
            Intent intent = new Intent(getBaseContext(), GoogleMapsActivity.class);
            intent.putExtra("user", currentUser.getEmail());
            startActivity(intent);
        } else {
            binding.inputUsername.setText("");
            binding.inputPassword.setText("");
            binding.textView7.setText("Usuario o Contraseña \nINCORRECTOS");
        }
    }

    private boolean validateForm() {
        boolean valid = true;
        String email = binding.inputUsername.getText().toString();
        if (TextUtils.isEmpty(email)) {
            binding.inputUsername.setError("Required.");
            valid = false;
        } else {
            if(isEmailValid(binding.inputUsername.getText().toString())){
                binding.inputUsername.setError(null);
            }else{
                binding.inputUsername.setError("Dirección Erronea.");
            }
        }
        String password = binding.inputPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            binding.inputPassword.setError("Required.");
            valid = false;
        } else {
            binding.inputPassword.setError(null);
        }
        return valid;
    }
    /*
    CERRAR SESIOON.
     */


    private void signInUser(String email, String password) {
        if (validateForm()) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
// Sign in success, update UI
                                Log.d("INFO", "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);
                            } else {
// If sign in fails, display a message to the user.
                                Log.w("INFO", "signInWithEmail:failure", task.getException());
                                Toast.makeText(MainActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        }
                    });
        }
    }

    private boolean isEmailValid(String email) {
        if (!email.contains("@") ||
                !email.contains(".") ||
                email.length() < 5)
            return false;
        return true;
    }
}