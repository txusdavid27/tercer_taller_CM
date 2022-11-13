package ponti.edu.trasla;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import ponti.edu.trasla.model.Usuario;

public class DisponiblesActivity extends AppCompatActivity {

    private static final String PATH_USERS = "users/";

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private Usuario data;

    private DisponiblesAdapter adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disponibles);

        //getSupportActionBar().setTitle("Disponibles");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference(PATH_USERS);
        listView = findViewById(R.id.lvLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        user = mAuth.getCurrentUser();
        initCurrentUser(user);
    }

    public void initCurrentUser(final FirebaseUser user){
        if(user != null){
            mRef = mDatabase.getReference(PATH_USERS + user.getUid());
            mRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    data = dataSnapshot.getValue(Usuario.class);
                    data.setKey(user.getUid());
                    initDisponibles();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(DisponiblesActivity.this, "Data recollection failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initDisponibles(){
        mRef = mDatabase.getReference(PATH_USERS);
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Usuario> disponibles = new ArrayList<>();
                for(DataSnapshot entity: dataSnapshot.getChildren()){
                    Usuario usuario = entity.getValue(Usuario.class);
                    if(usuario.getDisponible()){
                        usuario.setKey(entity.getKey());
                        disponibles.add(usuario);
                    }
                }
                adapter = new DisponiblesAdapter(DisponiblesActivity.this, disponibles, data.getKey());
                listView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}