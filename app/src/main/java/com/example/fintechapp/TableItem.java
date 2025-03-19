package com.example.fintechapp;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TableItem {
    private String key;
    private String title;
    private String modifiedTime; // e.g., "yyyy-MM-dd HH:mm:ss"
    private double payableAmount; // already exists
    private double totalAmount;   // NEW FIELD

    public TableItem() {}

    public TableItem(String key, String title, String modifiedTime, double payableAmount, double totalAmount) {
        this.key = key;
        this.title = title;
        this.modifiedTime = modifiedTime;
        this.payableAmount = payableAmount;
        this.totalAmount = totalAmount;
    }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public String getModifiedTime() { return modifiedTime; }
    public double getPayableAmount() { return payableAmount; }
    public double getTotalAmount() { return totalAmount; }

    public void setKey(String key) { this.key = key; }
    public void setTitle(String title) { this.title = title; }
    public void setModifiedTime(String modifiedTime) { this.modifiedTime = modifiedTime; }
    public void setPayableAmount(double payableAmount) { this.payableAmount = payableAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }



    public static class TableManager {

        // Delete the selected tables from Firebase.
        public static void deleteTables(DatabaseReference itemsRef, List<TableItem> selectedTables, Context context) {
            for (TableItem table : selectedTables) {
                if (table.getKey() != null) {
                    itemsRef.child(table.getKey()).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "Deleted table: " + table.getTitle(), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to delete: " + table.getTitle(), Toast.LENGTH_SHORT).show());
                }
            }
        }  // <-- This closing brace was missing

        // Merge selected tables into one new table.
        public static void mergeTables(DatabaseReference itemsRef, List<TableItem> selectedTables, Context context) {
            // Prepare accumulators. Note: totalDiscount is intentionally left at 0.
            final double[] totalAmount = {0.0};
            final double totalDiscount = 0.0; // always 0 during merge
            final double[] amountPaid = {0.0};
            final StringBuilder mergedTitle = new StringBuilder("Merged: ");
            final List<Map<String, Object>> mergedItems = new java.util.ArrayList<>();
            final int[] count = {0};

            // Iterate over each selected table to merge.
            for (TableItem table : selectedTables) {
                if (table.getKey() != null) {
                    itemsRef.child(table.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            count[0]++;
                            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                            if (data != null) {
                                // Accumulate totalAmount and amountPaid.
                                totalAmount[0] += data.get("totalAmount") != null
                                        ? ((Number) data.get("totalAmount")).doubleValue() : 0.0;
                                amountPaid[0] += data.get("amountPaid") != null
                                        ? ((Number) data.get("amountPaid")).doubleValue() : 0.0;

                                // Append each table's title.
                                String title = data.get("title") != null ? data.get("title").toString() : "";
                                mergedTitle.append(title).append(", ");

                                // Merge the items list from each table.
                                DataSnapshot itemsSnapshot = snapshot.child("items");
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    Map<String, Object> item = (Map<String, Object>) itemSnapshot.getValue();
                                    if (item != null) {
                                        mergedItems.add(item);
                                    }
                                }
                            }

                            // When all selected tables have been processed…
                            if (count[0] == selectedTables.size()) {
                                // Remove the trailing comma and space from the merged title.
                                if (mergedTitle.length() > 8) {
                                    mergedTitle.setLength(mergedTitle.length() - 2);
                                }

                                // Get the current timestamp.
                                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(new Date());

                                // With discount set to 0, the base payable is just totalAmount.
                                double basePayable = totalAmount[0];
                                double mergedPayable = basePayable - amountPaid[0];

                                // Prepare the merged table data.
                                Map<String, Object> mergedData = new HashMap<>();
                                mergedData.put("title", mergedTitle.toString());
                                mergedData.put("totalAmount", totalAmount[0]);
                                mergedData.put("totalDiscount", totalDiscount); // remains 0
                                mergedData.put("payableAmount", mergedPayable);
                                mergedData.put("amountPaid", amountPaid[0]);
                                mergedData.put("modifiedTime", currentTime);
                                mergedData.put("items", mergedItems);

                                // Write the merged table to Firebase.
                                String newTableId = itemsRef.push().getKey();
                                if (newTableId != null) {
                                    itemsRef.child(newTableId).setValue(mergedData)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(context, "Merged tables created", Toast.LENGTH_SHORT).show();
                                                // Optionally delete original tables (code commented out)
                                                //                                    for (TableItem t : selectedTables) {
                                                //                                        if (t.getKey() != null) {
                                                //                                            itemsRef.child(t.getKey()).removeValue();
                                                //                                        }
                                                //                                    }
                                                // Force a refresh of the table view.
                                                if (context instanceof MainActivity) {
                                                    ((MainActivity) context).loadTableItems();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Failed to merge tables", Toast.LENGTH_SHORT).show());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Error merging tables", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }
}

