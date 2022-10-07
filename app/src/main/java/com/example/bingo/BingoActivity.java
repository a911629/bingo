package com.example.bingo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.bingo.databinding.ActivityBingoBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingoActivity extends AppCompatActivity implements ValueEventListener {

    private static final int NUMBER_COUNT = 25;
    private static final String TAG = BingoActivity.class.getSimpleName();
    private String roomKey;
    private boolean creator;
    private TextView info;
    private RecyclerView recyclerView;
    private List<NumberBall> numbers;
    private List<Button> buttons;
    Map<Integer, Integer> numberPosition = new HashMap<>();

    private com.example.bingo.databinding.ActivityBingoBinding binding;
    private NumberAdapter adapter;

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    private boolean myTurn = false;

    private ValueEventListener statusListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.getValue() == null)
                return;
            long status = (long) snapshot.getValue();
            switch ((int) status) {
                case Room.STATS_INIT:
                    info.setText("等待對手進入遊戲室");
                    break;
                case Room.STATS_JOINED:
                    info.setText("對手加入");
                    if (isCreator()) {
                        setMyTurn(true);
                        Log.d(TAG, "onDataChange: set true 1");
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("status")
                                .setValue(Room.STATS_CREATORS_TURN);
                    }
                    break;
                case Room.STATS_CREATORS_TURN:
                    info.setText(isCreator() ? "請選擇號碼" : "等待對手選號");
                    break;
                case Room.STATS_JOINERS_TURN:
                    info.setText(!isCreator() ? "請選擇號碼" : "等待對手選號");
                        setMyTurn(true);
                        Log.d(TAG, "onDataChange: set true 2");
                    break;
                case Room.STATS_CREATOR_BINGO:
                    String msg = isCreator() ? "你賓果了!" : "對方賓果了!";
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("賓果")
                            .setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finishGame();
                                }
                            }).show();
                    break;
                case Room.STATS_JOINER_BINGO:
                    String msg2 = !isCreator() ? "你賓果了!" : "對方賓果了!";
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("賓果")
                            .setMessage(msg2)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finishGame();
                                }
                            }).show();
                    break;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };

    private void finishGame() {
//        if (isCreator()) {
        Log.d(TAG, "finishGame: remove game start");
        Log.d(TAG, "finishGame: game roomkey | " + roomKey + " |");
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .removeValue();
        Log.d(TAG, "finishGame: remove game done");
//        }
//        finish();
        System.exit(0);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBingoBinding.inflate(getLayoutInflater());
//        setContentView(R.layout.activity_bingo); //更換成下面getRoot
        setContentView(binding.getRoot());
        // 這裡收到roomKey
        roomKey = getIntent().getStringExtra("ROOM_KEY");
        creator = getIntent().getBooleanExtra("IS_CREATOR", false);

        //generate random numbers
        generateRandom();

        if (isCreator()) {
            for (int i = 0; i < NUMBER_COUNT; i++) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomKey)
                        .child("numbers")
                        .child((i + 1) + "")
                        .setValue(false);
            }
        } else { //joiner
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomKey)
                    .child("status")
                    .setValue(Room.STATS_JOINED);
        }
        findViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("numbers")
                .addValueEventListener(this);
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("status")
                .addValueEventListener(statusListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("status")
                .removeEventListener(statusListener);
    }

    private void findViews() {
        info = findViewById(R.id.info);
        recyclerView = findViewById(R.id.bingo_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        adapter = new NumberAdapter();
        recyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void generateRandom() {
        //generate random
        numbers = new ArrayList<>();
        buttons = new ArrayList<>();
        for (int i = 1; i <= NUMBER_COUNT; i++) {
            numbers.add(new NumberBall(i));
        }
        Collections.shuffle(numbers);
        for (int i = 0; i < NUMBER_COUNT; i++) {
            Button button = new Button(this);
            button.setText(numbers + "");
            buttons.add(button);
            numberPosition.put(numbers.get(i).getNumber(), i);
        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        int[] nums = new int[NUMBER_COUNT];
        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
            boolean picked = (boolean) snapshot1.getValue();
            int num = Integer.parseInt(snapshot1.getKey());
            nums[numberPosition.get(num)] = picked ? 1 : 0;
            if (picked) {
                numbers.get(numberPosition.get(num)).setPicked(true);
            }
            adapter.notifyDataSetChanged();
            //bingo process
//            Log.d(TAG, "onDataChange: " + nums);
            int bingo = 0;
            for (int i = 0; i < 5; i++) {
                int sum = 0;
                for (int j = 0; j < 5; j++) {
                    sum += nums[i * 5 + j];
                }
                if (sum == 5)
                    bingo++;
                sum = 0;
                for (int j = 0; j < 5; j++) {
                    sum += nums[i + j * 5];

                }
                if (sum == 5)
                    bingo++;
            }
//            Log.d(TAG, "onDataChange: bingo:" + bingo);
            if (bingo > 0) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomKey)
                        .child("status")
                        .setValue(isCreator() ? Room.STATS_CREATOR_BINGO : Room.STATS_JOINER_BINGO);
            }
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError error) {

    }

    class NumberAdapter extends RecyclerView.Adapter<NumberAdapter.NumberHolder> {
        @NonNull
        @Override
        public NumberHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.number_item, parent, false);
            return new NumberHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NumberHolder holder, @SuppressLint("RecyclerView") int position) {
            holder.button.setText(numbers.get(position).getNumber() + "");
            holder.button.setTag(position);
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myTurn) {
                        Log.d(TAG, "onClick: number:" + numbers.get(position));
                        setMyTurn(false);
                        Log.d(TAG, "onClick: set false 1");
                        holder.button.setEnabled(false);
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("numbers")
                                .child(numbers.get(position).getNumber() + "")
                                .setValue(true);
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("status")
                                .setValue(isCreator() ? Room.STATS_JOINERS_TURN : Room.STATS_CREATORS_TURN);
                    }
                }
            });
            holder.button.setEnabled(!numbers.get(position).isPicked());
        }

        @Override
        public int getItemCount() {
            return NUMBER_COUNT;
        }

        class NumberHolder extends RecyclerView.ViewHolder {
            Button button;

            public NumberHolder(@NonNull View itemView) {
                super(itemView);
                this.button = itemView.findViewById(R.id.number_button);
            }
        }
    }

    public boolean isCreator() {
        return creator;
    }

    public void setCreator(boolean creator) {
        this.creator = creator;
    }
}