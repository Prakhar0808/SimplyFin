// MainActivity.java (only the relevant parts are shown)
package com.example.fintechapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowInsets;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference itemsRef;
    private FloatingActionButton fab;
    private RecyclerView recyclerViewTables;
    private TableAdapter tableAdapter;
    private List<TableItem> tableItemList;
    private List<TableItem> fullTableItemList;
    private EditText search;
    private Button sort ,backButton;
    private LinearLayout selectionOptionsLayout;
    private ImageButton deleteButton;
    private Button mergeButton;
    private String title = "default_title";

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } else {
            itemsRef = FirebaseDatabase.getInstance().getReference("items")
                    .child(currentUser.getUid());
            loadTableItems();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide system bars for Android R+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        }

        ImageView logoutIcon = findViewById(R.id.logout_icon);
        fab = findViewById(R.id.fab);
        recyclerViewTables = findViewById(R.id.recyclerViewTables);
        search = findViewById(R.id.search);
        sort = findViewById(R.id.sort);

        // New: Find selection options layout and buttons.
        selectionOptionsLayout = findViewById(R.id.selection_options_layout);
        deleteButton = findViewById(R.id.delete_button);
        mergeButton = findViewById(R.id.merge_button);

        recyclerViewTables.setLayoutManager(new LinearLayoutManager(this));
        tableItemList = new ArrayList<>();
        fullTableItemList = new ArrayList<>();

        // Initialize adapter with selection listener.
        tableAdapter = new TableAdapter(tableItemList, this, count -> {
            if (count > 0) {
                selectionOptionsLayout.setVisibility(View.VISIBLE);
            } else {
                selectionOptionsLayout.setVisibility(View.GONE);
            }
        });
        recyclerViewTables.setAdapter(tableAdapter);

        logoutIcon.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
            intent.putExtra("title", title);
            startActivity(intent);
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Delete button click: delete selected tables.
        deleteButton.setOnClickListener(v -> {
            List<TableItem> selectedItems = tableAdapter.getSelectedItems();
            if (!selectedItems.isEmpty()) {
               TableItem.TableManager.deleteTables(itemsRef, selectedItems, MainActivity.this);
                tableAdapter.clearSelections();
            }
        });

        // Merge button click: merge selected tables.
        mergeButton.setOnClickListener(v -> {
            List<TableItem> selectedItems = tableAdapter.getSelectedItems();
            if (selectedItems.size() < 2) {
                Toast.makeText(MainActivity.this, "Select at least 2 tables to merge", Toast.LENGTH_SHORT).show();
            } else {
                TableItem.TableManager.mergeTables(itemsRef, selectedItems, MainActivity.this);
                tableAdapter.clearSelections();
            }
        });
        sort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, sort);
                popup.getMenuInflater().inflate(R.menu.sort_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(menuItem -> {
                    int id = menuItem.getItemId();
                    if (id == R.id.sort_alphabetical) {
                        Collections.sort(fullTableItemList, new Comparator<TableItem>() {
                            @Override
                            public int compare(TableItem a, TableItem b) {
                                return a.getTitle().compareToIgnoreCase(b.getTitle());
                            }
                        });
                    } else if (id == R.id.sort_last_added) {
                        Collections.sort(fullTableItemList, new Comparator<TableItem>() {
                            @Override
                            public int compare(TableItem a, TableItem b) {
                                return b.getModifiedTime().compareTo(a.getModifiedTime());
                            }
                        });
                    } else if (id == R.id.sort_first_added) {
                        Collections.sort(fullTableItemList, new Comparator<TableItem>() {
                            @Override
                            public int compare(TableItem a, TableItem b) {
                                return a.getModifiedTime().compareTo(b.getModifiedTime());
                            }
                        });
                    } else if (id == R.id.sort_payable) {
                        Collections.sort(fullTableItemList, new Comparator<TableItem>() {
                            @Override
                            public int compare(TableItem a, TableItem b) {
                                return Double.compare(b.getPayableAmount(), a.getPayableAmount());
                            }
                        });
                    }

                    // Refresh the list (apply the current search filter)
                    applySearchFilter(search.getText().toString());
                    return true;
                });
                popup.show();
            }
        });
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            tableAdapter.clearSelections(); // Clear selected items in the adapter.
        });


    }

    public void loadTableItems() {
        itemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullTableItemList.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String tableTitle = childSnapshot.child("title").getValue(String.class);
                    String key = childSnapshot.getKey();
                    // Read the modifiedTime field
                    String modifiedTime = childSnapshot.child("modifiedTime").getValue(String.class);
                    Double payable = childSnapshot.child("payableAmount").getValue(Double.class);
                    Double total = childSnapshot.child("totalAmount").getValue(Double.class);
                    if (tableTitle != null) {
                        fullTableItemList.add(new TableItem(key, tableTitle,
                                modifiedTime != null ? modifiedTime : "",
                                payable != null ? payable : 0.0,
                                total != null ? total : 0.0));
                    }
                }
                // Sort so that most recent modifiedTime comes first.
                Collections.sort(fullTableItemList, new Comparator<TableItem>() {
                    @Override
                    public int compare(TableItem a, TableItem b) {
                        return b.getModifiedTime().compareTo(a.getModifiedTime());
                    }
                });
                applySearchFilter(search.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load table titles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applySearchFilter(String query) {
        tableItemList.clear();
        if (query.isEmpty()) {
            tableItemList.addAll(fullTableItemList);
        } else {
            for (TableItem item : fullTableItemList) {
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    tableItemList.add(item);
                }
            }
        }
        tableAdapter.notifyDataSetChanged();
    }
}
