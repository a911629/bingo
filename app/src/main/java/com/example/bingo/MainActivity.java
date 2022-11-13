package com.example.bingo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bingo.databinding.ActivityMainBinding;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, ValueEventListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private TextView nickText;
    private Member member;
    private ImageView avator;
    private Group avatorGroup;
    private RecyclerView recyclerView;
    private EditText nicknameEdit;
    int[] avatars = {R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6};
    private FirebaseRecyclerAdapter<Room, RoomHolder> adapter;
    private DatabaseReference Ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: test");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        findViews();


        setSupportActionBar(binding.toolbar);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText titleEdit = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Room title")
                    .setView(titleEdit)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String roomTitle = titleEdit.getText().toString();
                            DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("rooms").push();
                            Room room = new Room(roomTitle, member);
                            roomRef.setValue(room);
                            String key = roomRef.getKey();
                            Log.d(TAG, "onClick: Room key | " + key);
                            roomRef.child("key").setValue(key);

                            Intent bingo = new Intent(MainActivity.this, BingoActivity.class);
                            bingo.putExtra("ROOM_KEY", key);
                            bingo.putExtra("IS_CREATOR", true);
                            startActivity(bingo);
                        }
                    }).setNeutralButton("Cancel", null)
                    .show();
            }
        });

        binding.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .removeValue();
            }
        });

        auth = FirebaseAuth.getInstance();
    }

    private void findViews() {
        nickText = findViewById(R.id.nickname);
        avator = findViewById(R.id.avatar);
        avatorGroup = findViewById(R.id.avatar_group);
        recyclerView = findViewById(R.id.recycler);
        avator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avatorGroup.setVisibility(avatorGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);

        //recyclerview  顯示資料庫資料，並設定顯示項目點擊事件
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = FirebaseDatabase.getInstance().getReference("rooms").orderByKey();

        FirebaseRecyclerOptions<Room> options = new FirebaseRecyclerOptions.Builder<Room>()
                .setQuery(query, Room.class).build();
        Log.d(TAG, "findViews: calvin : " + options.getSnapshots());

        //這裡recyclerview不自建adapter是因為要使用firebase的adapter，需要再建立firebaseAdapter時，輸入自訂的class與holder
        adapter = new FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull Room model) {
                holder.title.setText(model.getTitle() + " ");
                holder.image.setImageResource(avatars[model.getCreator().getAvatar()]);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent bingo = new Intent(MainActivity.this, BingoActivity.class);
                        Log.d(TAG, "onClick: name" + model.getTitle());
                        bingo.putExtra("ROOM_KEY", model.getKey());
                        bingo.putExtra("IS_CREATOR", false);
                        startActivity(bingo);
                    }
                });
            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View view = getLayoutInflater().inflate(R.layout.item_room, parent, false);
                return new RoomHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    class RoomHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;

        public RoomHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.room_image);
            title = itemView.findViewById(R.id.room_title);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
        adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_signout:
                auth.signOut();
                break;
        }
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        user = firebaseAuth.getCurrentUser();
        Log.d(TAG, "onAuthStateChanged: calvin user : " + user);
        if (user != null) {
            Log.d(TAG, "onAuthStateChanged: go if");
            Member member = new Member();
            member.setNickName(user.getDisplayName());
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(user.getUid());
                FirebaseDatabase.getInstance().getReference("users")
                        .child(user.getUid())
                        .addValueEventListener(this);
        } else {
            Log.d(TAG, "onAuthStateChanged: go else start");

            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build()
            )).setIsSmartLockEnabled(false).build(),
                RC_SIGN_IN);
            Log.d(TAG, "onAuthStateChanged: go else end");
        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        member = snapshot.getValue(Member.class);
        Log.d(TAG, "onDataChange: ");
        if (member.getNickName() == null) {
            EditText nickEdit = new EditText(this);
            new AlertDialog.Builder(this)
                .setTitle("NickName")
                .setView(nickEdit)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickname = nickEdit.getText().toString();
                        member.setNickName(nickname);
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid())
                                .setValue(member);
                    }
                }).setNeutralButton("Cancel", null)
                .show();
        } else {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            //member data change
                            member = snapshot.getValue(Member.class);
                            nickText.setText(member.getNickName());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
            nickText.setText(member.getNickName());
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    @Override
    public void onClick(View v) {

    }
}
