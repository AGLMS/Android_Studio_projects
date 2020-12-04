package com.example.localmart10;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.localmart10.Model.Cart;
import com.example.localmart10.Prevalent.Prevalent;
import com.example.localmart10.ViewHolder.CartViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApiNotAvailableException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Button NextProcessBtn;
    private TextView txtTotalAmount , txtMsg1;

    private int overTotalPrice = 0;

    DatabaseReference cartListRef;

    private FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter;
    private FirebaseRecyclerOptions<Cart> options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);



        NextProcessBtn = findViewById(R.id.next_process_btn);
        txtTotalAmount = findViewById(R.id.total_price);
        txtMsg1 = findViewById(R.id.msg1);
        Log.i("CartActivity", "oncreate:");
        //onStart();

        NextProcessBtn.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view) {
                txtTotalAmount.setText("Total Price = "+String.valueOf(overTotalPrice));
                Intent intent = new Intent(CartActivity.this,ConfirmFinalOrderActivity.class);
                intent.putExtra("Total Price",String.valueOf(overTotalPrice));
                startActivity(intent);
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        CheckOrderState();
        txtTotalAmount.setText("Total Price = "+String.valueOf(overTotalPrice));
        final String user = Prevalent.currentOnlineUser.getPhone();
        final DatabaseReference cart_list = FirebaseDatabase.getInstance().getReference().child("Cart List").child("User View")
                .child(user).child("products");
        FirebaseRecyclerOptions<Cart> options =
                new FirebaseRecyclerOptions.Builder<Cart>()
                        .setQuery(cart_list,Cart.class).build();

        FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull final Cart model) {

                holder.txtProductQuantity.setText("Quantity = " + model.getQuantity());
                holder.txtProductPrice.setText("Price = " + model.getPrice()+" ₹");
                holder.txtProductName.setText(model.getPname());
                //String temp = model.getPrice();
                overTotalPrice = overTotalPrice + (  ((Integer.valueOf(model.getPrice()))) * ((Integer.valueOf(model.getQuantity()))) );

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CharSequence options[] = new CharSequence[]{
                          "Edit" ,
                          "Remove"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(CartActivity.this);
                        builder.setTitle("Cart Options:");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(i == 0)
                                {
                                    Intent intent = new Intent(CartActivity.this,ProductDetailsActivity.class);
                                    intent.putExtra("pid",model.getPid());
                                    startActivity(intent);

                                }
                                if(i==1)
                                {
                                    //cart_list is the database ref for this current online user upto products
                                    cart_list.child(model.getPid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                Toast.makeText(CartActivity.this,"Item removed successfully", Toast.LENGTH_SHORT);

                                                Intent intent = new Intent(CartActivity.this,HomeActivity.class);
                                                startActivity(intent);
                                            }
                                        }
                                    });

                                }
                            }
                        });
                        builder.show();

                    }
                });

            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_items_layout,parent,false);
                CartViewHolder holder = new CartViewHolder(view);
                return holder;
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void CheckOrderState()
    {
        DatabaseReference ordersRef;
        ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders").child(Prevalent.currentOnlineUser.getPhone());

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    String shippingState = snapshot.child("state").getValue().toString();
                    String userName =snapshot.child("name").getValue().toString();

                    if(shippingState.equals("shipped"))
                    {
                        txtTotalAmount.setText("Dear "+userName + "\n order is   shipped successfully.");
                        recyclerView.setVisibility(View.GONE);

                        txtMsg1.setVisibility(View.VISIBLE);
                        txtMsg1.setText("Congradulations , Your final order has been placed successfully , soon you will receive your order") ;
                        NextProcessBtn.setVisibility(View.GONE);

                        Toast.makeText(CartActivity.this,"you can purchase more products, once you received your first final order",Toast.LENGTH_LONG);
                    }
                    else if(shippingState.equals("not shipped"))
                    {
                        txtTotalAmount.setText("Shipping State = Not shipped");
                        recyclerView.setVisibility(View.GONE);

                        txtMsg1.setVisibility(View.VISIBLE);
                        NextProcessBtn.setVisibility(View.GONE);

                        Toast.makeText(CartActivity.this,"you can purchase more products, once you received your first final order",Toast.LENGTH_LONG);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}

//        Log.i("CartActivity", "onStart: 60 line");
//         cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");
//         String user = Prevalent.currentOnlineUser.getPhone();
//         options = new FirebaseRecyclerOptions.Builder<Cart>().setQuery(cartListRef.child("User View")
//                .child(user),Cart.class).build();
//        Toast.makeText(CartActivity.this, "text"+Prevalent.currentOnlineUser.getPhone(), Toast.LENGTH_SHORT).show();
//        Log.i("CartActivity", "onStart: 64 line");
//         adapter = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
//
//            @Override
//            protected void onBindViewHolder(@NonNull CartViewHolder cartViewHolder, int i, @NonNull Cart model)
//            {
//
//                cartViewHolder.txtProductQuantity.setText("Quatity = " + model.getQuantity());
//                cartViewHolder.txtProductPrice.setText("Price = " + model.getPrice()+"₹");
//                cartViewHolder.txtProductName.setText(model.getPname());
//                Log.i("CartActivity", "onStart: 74 line");
//            }
//
//            @NonNull
//            @Override
//            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_items_layout,parent,false);
//                CartViewHolder holder = new CartViewHolder(view);
//                Log.i("CartActivity", "onStart: 82 line");
//                return holder;
//            }
//
//        };
//        adapter.startListening();
//        recyclerView.setAdapter(adapter);
//
//        Log.i("CartActivity", "onStart: 89 line");
//}
//
//}