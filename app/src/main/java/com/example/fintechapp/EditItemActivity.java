package com.example.fintechapp;

import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditItemActivity extends AppCompatActivity {

    // UI components
    private EditText itemNameEditText, quantityEditText, pricePerItemEditText, discountEditText,
            totalDiscountEditText, Title, AmountPaid,itemid;
    private Button addItemButton, addTableButton, DeleteButton;
    private TableLayout itemTable;
    private TextView totalAmountTextView, payableAmountTextView;
    // Firebase and data
    private FirebaseAuth mAuth;
    private DatabaseReference itemsRef;
    private ArrayList<Map<String, Object>> itemList;
    // Totals
    private double totalAmount = 0.0, totalDiscount = 0.0, payableAmount = 0.0;
    // New variable for amount paid (default 0.0)
    private double amountPaid = 0.0;
    // For row editing
    private TableRow selectedRow = null;
    // For edit mode (updating an existing table)
    private boolean isEditMode = false;
    private String tableId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        // Use the current user's node
        itemsRef = FirebaseDatabase.getInstance().getReference("items")
                .child(mAuth.getCurrentUser().getUid());
        addTableHeader();

        // Check if launched for editing an existing table.
        if (getIntent().hasExtra("tableId")) {
            isEditMode = true;
            tableId = getIntent().getStringExtra("tableId");
            // Change button text to indicate update mode.
            addTableButton.setText("Update Table");
            loadTableData();
        }

        addItemButton.setOnClickListener(v -> handleAddOrUpdateItem());
        addTableButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateTableData();
            } else {
                addTableData();
            }
        });
        DeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedRow != null) {
                    int headerOffset = 1; // header row is at index 0
                    int rowIndex = itemTable.indexOfChild(selectedRow) - headerOffset;
                    // Get row's total price from cell index 5 (assuming that column holds total price)
                    double rowTotal = Double.parseDouble(((TextView) selectedRow.getChildAt(5)).getText().toString());
                    // Remove the selected row from the TableLayout
                    itemTable.removeView(selectedRow);
                    // Remove the corresponding item from itemList if valid index
                    if (rowIndex >= 0 && rowIndex < itemList.size()) {
                        itemList.remove(rowIndex);
                    }
                    // Update total amount and recalc payable amount
                    totalAmount -= rowTotal;
                    double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
                    payableAmount = basePayable - amountPaid;
                    updateTotalTextViews();
                    // Reindex rows so serial numbers update
                    reindexRows();
                    // Clear input fields
                    clearInputFields();
                    // Reset selectedRow and hide DeleteButton
                    selectedRow = null;
                    DeleteButton.setVisibility(View.GONE);
                    // Update the Firebase record for the table
                    updateFirebaseTable();
                }
            }
        });



        // When the user presses enter on the total discount field,
        // recalculate the payable amount.
        totalDiscountEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                applyTotalDiscount();
                return true;
            }
            return false;
        });
        itemid.setOnEditorActionListener((v, actionId, event) -> {
            itemNameEditText.requestFocus();
            return true;
        });

// When the user presses Enter in itemName, move focus to quantity.
        itemNameEditText.setOnEditorActionListener((v, actionId, event) -> {
            quantityEditText.requestFocus();
            return true;
        });

// When the user presses Enter in quantity, move focus to price per item.
        quantityEditText.setOnEditorActionListener((v, actionId, event) -> {
            pricePerItemEditText.requestFocus();
            return true;
        });

// When the user presses Enter in price per item, move focus to discount.
        pricePerItemEditText.setOnEditorActionListener((v, actionId, event) -> {
            discountEditText.requestFocus();
            return true;
        });

// When the user presses Enter in discount, simulate clicking the add row button.
        discountEditText.setOnEditorActionListener((v, actionId, event) -> {
            addItemButton.performClick();
            return true;
        });
        // New: When the user presses enter on the AmountPaid field,
        // update the amount paid and recalculate payable amount.
        AmountPaid.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                applyAmountPaid();
                return true;
            }
            return false;
        });
    }

    // Initialize UI components.
    private void initializeViews() {
        itemid = findViewById(R.id.itemid);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        pricePerItemEditText = findViewById(R.id.pricePerItemEditText);
        discountEditText = findViewById(R.id.discountPerItem);
        totalDiscountEditText = findViewById(R.id.TotalDiscount);
        Title = findViewById(R.id.Title);
        addItemButton = findViewById(R.id.addRowButton);
        addTableButton = findViewById(R.id.addTableButton);
        itemTable = findViewById(R.id.itemTable);
        totalAmountTextView = findViewById(R.id.totalAmount);
        payableAmountTextView = findViewById(R.id.PayableAmount);
        AmountPaid = findViewById(R.id.AmountPaid);
        DeleteButton = findViewById(R.id.DeleteButton);
        itemList = new ArrayList<>();
    }

    // If in edit mode, load existing table data from Firebase.
    private void loadTableData() {
        if (tableId == null) return;
        itemsRef.child(tableId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Load table-level fields.
                    String titleStr = snapshot.child("title").getValue(String.class);
                    Double tAmount = snapshot.child("totalAmount").getValue(Double.class);
                    Double tDiscount = snapshot.child("totalDiscount").getValue(Double.class);
                    Double pAmount = snapshot.child("payableAmount").getValue(Double.class);
                    // New: load amount paid (if stored)
                    Double aPaid = snapshot.child("amountPaid").getValue(Double.class);
                    Title.setText(titleStr);
                    totalAmount = tAmount != null ? tAmount : 0.0;
                    totalDiscount = tDiscount != null ? tDiscount : 0.0;
                    payableAmount = pAmount != null ? pAmount : 0.0;
                    amountPaid = aPaid != null ? aPaid : 0.0;
                    updateTotalTextViews();

                    // Load the items list.
                    itemList.clear();
                    DataSnapshot itemsSnapshot = snapshot.child("items");
                    for (DataSnapshot child : itemsSnapshot.getChildren()) {
                        Map<String, Object> itemData = (Map<String, Object>) child.getValue();
                        itemList.add(itemData);
                    }
                    populateItemTable();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditItemActivity.this, "Failed to load table data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Populate the table layout with rows from itemList.
    private void populateItemTable() {
        itemTable.removeAllViews();
        addTableHeader();
        for (int i = 0; i < itemList.size(); i++) {
            Map<String, Object> item = itemList.get(i);
            String itemName = String.valueOf(item.get("itemName"));
            int quantity = Integer.parseInt(String.valueOf(item.get("quantity")));
            double pricePerItem = Double.parseDouble(String.valueOf(item.get("pricePerItem")));
            double discount = Double.parseDouble(String.valueOf(item.get("discount")));
            double totalPrice = Double.parseDouble(String.valueOf(item.get("totalPrice")));
            addItemRow(itemName, quantity, pricePerItem, discount, totalPrice, i);
        }
    }

    // Add a row to the table layout (for edit mode, we supply the index).
    private void addItemRow(String itemName, int quantity, double pricePerItem, double discount, double totalPrice, int index) {
        TableRow newRow = new TableRow(this);
        int rowIndex = index + 1; // header is at index 0.
        newRow.setBackgroundColor(rowIndex % 2 == 0 ? Color.parseColor("#FFF3E0") : Color.WHITE);

        TextView serialNoTextView = createTableCell(String.valueOf(rowIndex));
        TextView itemNameTextView = createTableCell(itemName);
        TextView itemQuantityTextView = createTableCell(String.valueOf(quantity));
        TextView itemPriceTextView = createTableCell(String.format("%.2f", pricePerItem));
        TextView discountTextView = createTableCell(discount + "%");
        TextView itemTotalPriceTextView = createTableCell(String.format("%.2f", totalPrice));

        newRow.addView(serialNoTextView);
        newRow.addView(itemNameTextView);
        newRow.addView(itemQuantityTextView);
        newRow.addView(itemPriceTextView);
        newRow.addView(discountTextView);
        newRow.addView(itemTotalPriceTextView);

        // Set a click listener that passes the index.
        newRow.setOnClickListener(v -> selectRowForEditing(newRow, index));
        itemTable.addView(newRow);
    }

    // Called when the user clicks "Add Item" (or "Update Row" if a row is selected).
    private void handleAddOrUpdateItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String quantityStr = quantityEditText.getText().toString().trim();
        String priceStr = pricePerItemEditText.getText().toString().trim();
        String discountStr = discountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(EditItemActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        int quantity = Integer.parseInt(quantityStr);
        double pricePerItem = Double.parseDouble(priceStr);
        double discount = TextUtils.isEmpty(discountStr) ? 0 : Double.parseDouble(discountStr);
        if (discount > 100) {
            Toast.makeText(EditItemActivity.this, "Discount cannot be more than 100%", Toast.LENGTH_SHORT).show();
            return;
        }
        double totalPrice = pricePerItem * quantity * (1 - discount / 100);

        if (selectedRow == null) {
            // New item: add row and update the total.
            addItemToTable(itemName, quantity, pricePerItem, discount, totalPrice);
            totalAmount += totalPrice;
        } else {
            // Updating an existing row.
            updateSelectedRow(itemName, quantity, pricePerItem, discount, totalPrice);
            // Note: updateSelectedRow() already adjusts totalAmount.
        }
        // Recalculate payable amount (if amountPaid was already entered).
        double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
        payableAmount = basePayable - amountPaid;
        updateTotalTextViews();
        clearInputFields();
    }

    // (For add mode) Add a new row and update itemList.
    private void addItemToTable(String itemName, int quantity, double pricePerItem, double discount, double totalPrice) {
        TableRow newRow = new TableRow(this);
        // Calculate rowIndex based on current table rows excluding header.
        int rowIndex = itemTable.getChildCount() - 1;
        newRow.setBackgroundColor(rowIndex % 2 == 0 ? Color.parseColor("#FFF3E0") : Color.WHITE);

        TextView serialNoTextView = createTableCell(String.valueOf(rowIndex));
        TextView itemNameTextView = createTableCell(itemName);
        TextView itemQuantityTextView = createTableCell(String.valueOf(quantity));
        TextView itemPriceTextView = createTableCell(String.format("%.2f", pricePerItem));
        TextView discountTextView = createTableCell(discount + "%");
        TextView itemTotalPriceTextView = createTableCell(String.format("%.2f", totalPrice));

        newRow.addView(serialNoTextView);
        newRow.addView(itemNameTextView);
        newRow.addView(itemQuantityTextView);
        newRow.addView(itemPriceTextView);
        newRow.addView(discountTextView);
        newRow.addView(itemTotalPriceTextView);

        // Add the new item data to itemList first.
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemName", itemName);
        itemData.put("quantity", quantity);
        itemData.put("pricePerItem", pricePerItem);
        itemData.put("discount", discount);
        itemData.put("totalPrice", totalPrice);
        itemList.add(itemData);
        // Now set the row tag to the correct index (itemList size - 1).
        int index = itemList.size() - 1;
        newRow.setTag(index);

        newRow.setOnClickListener(v -> selectRowForEditing(newRow, (int) newRow.getTag()));
        itemTable.addView(newRow);
    }

    // When a row is clicked, highlight it and fill the input fields.
    // The index (position in itemList) is stored as the row tag.
    private void selectRowForEditing(TableRow row, int index) {
        if (selectedRow != null) {
            selectedRow.setBackgroundColor(Color.WHITE);
        }
        selectedRow = row;
        selectedRow.setBackgroundColor(Color.LTGRAY);
        String itemName = ((TextView) row.getChildAt(1)).getText().toString();
        String quantity = ((TextView) row.getChildAt(2)).getText().toString();
        String pricePerItem = ((TextView) row.getChildAt(3)).getText().toString();
        String discount = ((TextView) row.getChildAt(4)).getText().toString().replace("%", "");
        itemNameEditText.setText(itemName);
        quantityEditText.setText(quantity);
        pricePerItemEditText.setText(pricePerItem);
        discountEditText.setText(discount);
        // Save the index in the row tag.
        row.setTag(index);
        DeleteButton.setVisibility(View.VISIBLE);
    }

    // Update the selected row and the corresponding itemList entry.
    private void updateSelectedRow(String itemName, int quantity, double pricePerItem, double discount, double newTotalPrice) {
        if (selectedRow != null) {
            int index = (int) selectedRow.getTag();
            // Subtract the old total from totalAmount.
            double oldTotalPrice = Double.parseDouble(((TextView) selectedRow.getChildAt(5)).getText().toString());
            totalAmount -= oldTotalPrice;
            DeleteButton.setVisibility(View.VISIBLE);
            // Update the row's values.
            ((TextView) selectedRow.getChildAt(1)).setText(itemName);
            ((TextView) selectedRow.getChildAt(2)).setText(String.valueOf(quantity));
            ((TextView) selectedRow.getChildAt(3)).setText(String.format("%.2f", pricePerItem));
            ((TextView) selectedRow.getChildAt(4)).setText(discount + "%");
            ((TextView) selectedRow.getChildAt(5)).setText(String.format("%.2f", newTotalPrice));
            // Update itemList with new data.
            Map<String, Object> updatedItem = new HashMap<>();
            updatedItem.put("itemName", itemName);
            updatedItem.put("quantity", quantity);
            updatedItem.put("pricePerItem", pricePerItem);
            updatedItem.put("discount", discount);
            updatedItem.put("totalPrice", newTotalPrice);
            itemList.set(index, updatedItem);
            // Add the new total price.
            totalAmount += newTotalPrice;
            selectedRow.setBackgroundColor(Color.WHITE);
            selectedRow = null;
        }
    }

    // Create a table cell (TextView)
    private TextView createTableCell(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);
        // Force a single line and ellipsize if text is too long
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        return textView;
    }


    // Apply discount to compute payable amount.
    // Updated to subtract any amount paid that was entered.
    private void applyTotalDiscount() {
        String discountStr = totalDiscountEditText.getText().toString().trim();
        totalDiscount = TextUtils.isEmpty(discountStr) ? 0.0 : Double.parseDouble(discountStr);
        double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
        payableAmount = basePayable - amountPaid;
        updateTotalTextViews();
    }

    // New method: Apply the amount paid.
    private void applyAmountPaid() {
        String amountPaidStr = AmountPaid.getText().toString().trim();
        amountPaid = TextUtils.isEmpty(amountPaidStr) ? 0.0 : Double.parseDouble(amountPaidStr);
        double enteredAmountPaid = TextUtils.isEmpty(amountPaidStr) ? 0.0 : Double.parseDouble(amountPaidStr);
        double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
        if (enteredAmountPaid > basePayable) {
            Toast.makeText(EditItemActivity.this,
                    "Amount paid cannot be greater than the payable amount (" + String.format("%.2f", basePayable) + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        payableAmount = basePayable - amountPaid;
        updateTotalTextViews();
    }

    // Update the totals in the UI.
    private void updateTotalTextViews() {
        totalAmountTextView.setText(String.format("%.2f", totalAmount));
        totalDiscountEditText.setText(String.format("%.2f", totalDiscount));
        payableAmountTextView.setText(String.format("%.2f", payableAmount));
        // Optionally update the AmountPaid field display (if you want to show formatted value).
        AmountPaid.setText(String.format("%.2f", amountPaid));
    }

    // Clear the item input fields.
    private void clearInputFields() {
        itemNameEditText.setText("");
        quantityEditText.setText("");
        pricePerItemEditText.setText("");
        discountEditText.setText("");
    }

    // In add mode: create a new table record in Firebase.
    private void addTableData() {
        if (itemList.isEmpty()) {
            Toast.makeText(EditItemActivity.this, "No items to add to the table", Toast.LENGTH_SHORT).show();
            return;
        }
        String newTableId = itemsRef.push().getKey();
        if (newTableId == null) {
            Toast.makeText(EditItemActivity.this, "Error generating table ID", Toast.LENGTH_SHORT).show();
            return;
        }
        String titleStr = Title.getText().toString().trim();
        if (TextUtils.isEmpty(titleStr)) {
            Toast.makeText(EditItemActivity.this, "Please add title to table", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("items", itemList);
        tableData.put("totalAmount", totalAmount);
        tableData.put("totalDiscount", totalDiscount);
        tableData.put("payableAmount", payableAmount);
        tableData.put("title", titleStr);

        // New: store amountPaid as an attribute of the table.
        tableData.put("amountPaid", amountPaid);
        itemsRef.child(newTableId).setValue(tableData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditItemActivity.this, "Table added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditItemActivity.this, "Failed to add table", Toast.LENGTH_SHORT).show();
                });
    }

    // In edit mode: update the existing table record in Firebase.
    private void updateTableData() {
        if (itemList.isEmpty()) {
            Toast.makeText(EditItemActivity.this, "No items to update in the table", Toast.LENGTH_SHORT).show();
            return;
        }
        String titleStr = Title.getText().toString().trim();
        if (TextUtils.isEmpty(titleStr)) {
            Toast.makeText(EditItemActivity.this, "Please add title to table", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("items", itemList);
        tableData.put("totalAmount", totalAmount);
        tableData.put("totalDiscount", totalDiscount);
        tableData.put("payableAmount", payableAmount);
        tableData.put("title", titleStr);
        tableData.put("modifiedTime", currentTime);

        // New: store amountPaid as an attribute.
        tableData.put("amountPaid", amountPaid);
        itemsRef.child(tableId).updateChildren(tableData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditItemActivity.this, "Table updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditItemActivity.this, "Failed to update table", Toast.LENGTH_SHORT).show();
                });
    }

    private void addTableHeader() {
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#FD6500")); // Orange header
        headerRow.setPadding(8, 8, 8, 8);
        headerRow.setClickable(true);
        headerRow.setLongClickable(true);
        headerRow.setDescendantFocusability(TableRow.FOCUS_BLOCK_DESCENDANTS);

        String[] headers = {"S. No", "Item Name", "Quantity", "Price", "Discount", "Total"};
        for (String header : headers) {
            TextView textView = createTableCell(header);
            textView.setTextColor(Color.WHITE);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerRow.addView(textView);
        }

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Launch full-screen activity with current table content.
                Intent intent = new Intent(EditItemActivity.this, FullScreenTableActivity.class);
                // Pass the current itemList (make sure your ArrayList and HashMap are Serializable)
                intent.putExtra("tableContent", itemList);
                startActivity(intent);
                return true;
            }
        });

        headerRow.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        itemTable.addView(headerRow);
    }


    private void reindexRows() {
        int childCount = itemTable.getChildCount();
        for (int i = 1; i < childCount; i++) { // index 0 is header.
            TableRow row = (TableRow) itemTable.getChildAt(i);
            TextView serialText = (TextView) row.getChildAt(0);
            serialText.setText(String.valueOf(i));
        }
    }
    private void updateFirebaseTable() {
        if (tableId == null) return;
        String titleStr = Title.getText().toString().trim();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("items", itemList);
        tableData.put("totalAmount", totalAmount);
        tableData.put("totalDiscount", totalDiscount);
        tableData.put("payableAmount", payableAmount);
        tableData.put("title", titleStr);
        tableData.put("modifiedTime", currentTime);
        tableData.put("amountPaid", amountPaid);
        itemsRef.child(tableId).updateChildren(tableData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(EditItemActivity.this, "Table updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(EditItemActivity.this, "Failed to update table", Toast.LENGTH_SHORT).show());
    }

}