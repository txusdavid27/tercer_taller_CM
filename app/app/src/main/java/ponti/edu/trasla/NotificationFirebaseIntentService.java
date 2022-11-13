package ponti.edu.trasla;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

public class NotificationFirebaseIntentService extends JobIntentService {

    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    DatabaseReference myRef;
    private FirebaseUser user;
    private Usuario data;

    private static final int JOB_ID = 13;
    private List<Usuario> anteriores = new ArrayList<>();

    private static String CHANNEL_ID = "MyApp";
    private static final String PATH_USERS = "users/";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, NotificationFirebaseIntentService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        myRef = firebaseDatabase.getReference(PATH_USERS);
        user = firebaseAuth.getCurrentUser();
        initCurrentUser(user);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        myRef = firebaseDatabase.getReference(PATH_USERS);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot entity: dataSnapshot.getChildren()){
                    Usuario usuario = entity.getValue(Usuario.class);
                    usuario.setKey(entity.getKey());
                    boolean changeAux = stateChange(usuario);
                    if(firebaseAuth.getCurrentUser() != null && usuario.getDisponible() && !usuario.getKey().equals(data.getKey())){
                        buildAndShowNotification("Usuario Disponible", "El usuario: "+usuario.getNombre()+" se encuentra DISPONIBLE", usuario.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void llenarInicial(){
        myRef = firebaseDatabase.getReference(PATH_USERS);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot entity: dataSnapshot.getChildren()){
                    Usuario usuario = entity.getValue(Usuario.class);
                    usuario.setKey(entity.getKey());
                    anteriores.add(usuario);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void initCurrentUser(FirebaseUser user){
        if(user != null){
            myRef = firebaseDatabase.getReference(PATH_USERS + user.getUid());
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    data = dataSnapshot.getValue(Usuario.class);
                    data.setKey(dataSnapshot.getKey());
                    llenarInicial();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(NotificationFirebaseIntentService.this, "Data retriving failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean stateChange(Usuario userN){
        boolean change = false;
        int pos = -1;
        for(int i = 0; i < anteriores.size(); i++){
            if(anteriores.get(i).getKey().equals(userN.getKey())){
                if(!anteriores.get(i).getDisponible() && userN.getDisponible()){
                    change = true;
                    pos = i;
                    break;
                }
            }
        }
        if (pos != -1){
            anteriores.remove(pos);
            anteriores.add(userN);
        }

        return change;
    }

    private void buildAndShowNotification(String title, String message, String userKey){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mBuilder.setSmallIcon(R.drawable.img);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(message);
        mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent intent = new Intent(this, GoogleMapsActivity.class);
        intent.putExtra("key", userKey);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);

        int notificationId = 001;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, mBuilder.build());
    }
}