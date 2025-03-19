package com.example.fintechapp;

import android.content.Intent;
import android.graphics.Color;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    // UI components
    private EditText itemNameEditText, quantityEditText, pricePerItemEditText,
            discountEditText, totalDiscountEditText, Title, amountPaidEditText,itemid;
    private Button addItemButton, addTableButton,DeleteButton;
    private TableLayout itemTable;
    private TextView totalAmountTextView, payableAmountTextView;
    // Firebase & Data
    private FirebaseAuth mAuth;
    private DatabaseReference itemsRef;
    private ArrayList<Map<String, Object>> itemList;
    // Totals and amount paid
    private double totalAmount = 0.0, totalDiscount = 0.0, payableAmount = 0.0;
    private double amountPaid = 0.0;
    private TableRow selectedRow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        initializeViews();
        mAuth = FirebaseAuth.getInstance();
        itemsRef = FirebaseDatabase.getInstance()
                .getReference("items")
                .child(mAuth.getCurrentUser().getUid());
        addTableHeader();

        addItemButton.setOnClickListener(v -> handleAddOrUpdateItem());
        addTableButton.setOnClickListener(v -> handleAddTableClick());
        DeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedRow != null) {
                    int headerOffset = 1; // header row is at index 0
                    int rowIndex = itemTable.indexOfChild(selectedRow) - headerOffset;
                    // Get the row's total price from cell index 5
                    double rowTotal = Double.parseDouble(((TextView) selectedRow.getChildAt(5)).getText().toString());
                    // Remove the row from the TableLayout
                    itemTable.removeView(selectedRow);
                    // Remove the corresponding item from the list (if index is valid)
                    if (rowIndex >= 0 && rowIndex < itemList.size()) {
                        itemList.remove(rowIndex);
                    }
                    // Update totals and payable amount
                    totalAmount -= rowTotal;
                    double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
                    payableAmount = basePayable - amountPaid;
                    updateTotalTextViews();
                    // Reindex the remaining rows
                    reindexRows();
                    // Clear input fields
                    clearInputFields();
                    // Reset selectedRow and hide DeleteButton
                    selectedRow = null;
                    DeleteButton.setVisibility(View.GONE);
                }
            }
        });
// When the user presses Enter in itemId, move focus to itemName.
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


        // Recalculate total discount when Enter is pressed on totalDiscountEditText
        totalDiscountEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                applyTotalDiscount();
                return true;
            }
            return false;
        });
        // NEW: Recalculate amount paid when Enter is pressed on amountPaidEditText
        amountPaidEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                applyAmountPaid();
                return true;
            }
            return false;
        });
    }

    // Initialize UI components
    private void initializeViews() {
        itemid = findViewById(R.id.itemid);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        pricePerItemEditText = findViewById(R.id.pricePerItemEditText);
        discountEditText = findViewById(R.id.discountPerItem);
        totalDiscountEditText = findViewById(R.id.TotalDiscount);
        Title = findViewById(R.id.Title);
        // Make sure your layout contains an EditText with id "AmountPaid"
        amountPaidEditText = findViewById(R.id.AmountPaid);
        addItemButton = findViewById(R.id.addRowButton);
        DeleteButton = findViewById(R.id.DeleteButton);
        addTableButton = findViewById(R.id.addTableButton);
        itemTable = findViewById(R.id.itemTable);
        totalAmountTextView = findViewById(R.id.totalAmount);
        payableAmountTextView = findViewById(R.id.PayableAmount);
        itemList = new ArrayList<>();
    }

    // Handle "Add Item" button click
    private void handleAddOrUpdateItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String quantityStr = quantityEditText.getText().toString().trim();
        String priceStr = pricePerItemEditText.getText().toString().trim();
        String discountStr = discountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(AddItemActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        double pricePerItem = Double.parseDouble(priceStr);
        double discount = TextUtils.isEmpty(discountStr) ? 0 : Double.parseDouble(discountStr);

        if (discount > 100) {
            Toast.makeText(AddItemActivity.this, "Discount cannot be more than 100%", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalPrice = pricePerItem * quantity * (1 - discount / 100);

        if (selectedRow == null) {
            // Add new item
            addItemToTable(itemName, quantity, pricePerItem, discount, totalPrice);
        } else {
            // Update existing row
            updateSelectedRow(itemName, quantity, pricePerItem, discount, totalPrice);
        }

        totalAmount += totalPrice;
        // Recalculate payable amount: deduct discount and amount paid
        payableAmount = totalAmount - (totalDiscount * totalAmount) / 100 - amountPaid;
        updateTotalTextViews();
        clearInputFields();
    }

    // Handle "Add Table" button click
    private void handleAddTableClick() {
        if (itemList.isEmpty()) {
            Toast.makeText(AddItemActivity.this, "No items to add to the table", Toast.LENGTH_SHORT).show();
            return;
        }

        String tableId = itemsRef.push().getKey();
        if (tableId == null) {
            Toast.makeText(AddItemActivity.this, "Error generating table ID", Toast.LENGTH_SHORT).show();
            return;
        }
        String titleStr = Title.getText().toString().trim();
        if (TextUtils.isEmpty(titleStr)) {
            Toast.makeText(AddItemActivity.this, "Please add title to table", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());


        // Create a map to store table data (including amountPaid)
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("items", itemList);
        tableData.put("totalAmount", totalAmount);
        tableData.put("totalDiscount", totalDiscount);
        tableData.put("payableAmount", payableAmount);
        tableData.put("title", titleStr);
        tableData.put("amountPaid", amountPaid);
        tableData.put("modifiedTime", currentTime);


        itemsRef.child(tableId).setValue(tableData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddItemActivity.this, "Table added successfully", Toast.LENGTH_SHORT).show();
                    // Reset the table and totals
                    itemTable.removeAllViews();
                    itemList.clear();
                    totalAmount = 0.0;
                    totalDiscount = 0.0;
                    payableAmount = 0.0;
                    amountPaid = 0.0;
                    Title.setText("");
                    totalDiscountEditText.setText("");
                    amountPaidEditText.setText("");
                    addTableHeader();
                    updateTotalTextViews();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to add table", Toast.LENGTH_SHORT).show();
                });
    }

    // Add table header row
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
                Intent intent = new Intent(AddItemActivity.this, FullScreenTableActivity.class);
                // Pass the current itemList (make sure your ArrayList and HashMap are Serializable)
                intent.putExtra("tableContent", itemList);
                startActivity(intent);
                return true;
            }
        });

        headerRow.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        itemTable.addView(headerRow);
    }


    // Add a new item row to the table and update itemList
    private void addItemToTable(String itemName, int quantity, double pricePerItem, double discount, double totalPrice) {
        TableRow newRow = new TableRow(this);
        int rowIndex = itemTable.getChildCount() - 1; // Exclude header row

        newRow.setBackgroundColor(rowIndex % 2 == 0 ? Color.parseColor("#FFF3E0") : Color.WHITE);

        // Create TextViews for row cells
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

        // Allow row selection for editing
        newRow.setOnClickListener(v -> selectRowForEditing(newRow));

        itemTable.addView(newRow);

        // Add the item data to the list
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("itemId", rowIndex);
        itemData.put("itemName", itemName);
        itemData.put("quantity", quantity);
        itemData.put("pricePerItem", pricePerItem);
        itemData.put("discount", discount);
        itemData.put("totalPrice", totalPrice);
        itemList.add(itemData);
    }

    // When a row is clicked, highlight it and populate input fields for editing
    private void selectRowForEditing(TableRow row) {
        if (selectedRow != null) {
            selectedRow.setBackgroundColor(Color.WHITE); // Reset previous selection
        }
        selectedRow = row;
        selectedRow.setBackgroundColor(Color.LTGRAY); // Highlight selected row
        DeleteButton.setVisibility(View.VISIBLE);

        // Retrieve values from the row and fill in the input fields
        String itemName = ((TextView) row.getChildAt(1)).getText().toString();
        String quantity = ((TextView) row.getChildAt(2)).getText().toString();
        String pricePerItem = ((TextView) row.getChildAt(3)).getText().toString();
        String discount = ((TextView) row.getChildAt(4)).getText().toString().replace("%", "");

        itemNameEditText.setText(itemName);
        quantityEditText.setText(quantity);
        pricePerItemEditText.setText(pricePerItem);
        discountEditText.setText(discount);
    }

    // Update an existing row with new data
    private void updateSelectedRow(String itemName, int quantity, double pricePerItem, double discount, double newTotalPrice) {
        if (selectedRow != null) {
            // Subtract the old total price from totalAmount
            double oldTotalPrice = Double.parseDouble(
                    ((TextView) selectedRow.getChildAt(5)).getText().toString());
            totalAmount -= oldTotalPrice;

            // Update row values
            ((TextView) selectedRow.getChildAt(1)).setText(itemName);
            ((TextView) selectedRow.getChildAt(2)).setText(String.valueOf(quantity));
            ((TextView) selectedRow.getChildAt(3)).setText(String.format("%.2f", pricePerItem));
            ((TextView) selectedRow.getChildAt(4)).setText(discount + "%");
            ((TextView) selectedRow.getChildAt(5)).setText(String.format("%.2f", newTotalPrice));

            selectedRow.setBackgroundColor(Color.WHITE);
            selectedRow = null; // Reset selection
        }
    }

    // Create a table cell (TextView) with standardized styling
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


    // Apply total discount and update payable amount (taking into account amount paid)
    private void applyTotalDiscount() {
        String discountStr = totalDiscountEditText.getText().toString().trim();
        totalDiscount = TextUtils.isEmpty(discountStr) ? 0.0 : Double.parseDouble(discountStr);
        payableAmount = totalAmount - (totalDiscount * totalAmount) / 100 - amountPaid;
        updateTotalTextViews();
    }

    // NEW: Apply the amount paid from the AmountPaid field and update payable amount
    private void applyAmountPaid() {
        String amountPaidStr = amountPaidEditText.getText().toString().trim();
        amountPaid = TextUtils.isEmpty(amountPaidStr) ? 0.0 : Double.parseDouble(amountPaidStr);
        double enteredAmountPaid = TextUtils.isEmpty(amountPaidStr) ? 0.0 : Double.parseDouble(amountPaidStr);
        double basePayable = totalAmount - (totalDiscount * totalAmount) / 100;
        if (enteredAmountPaid > basePayable) {
            Toast.makeText(AddItemActivity.this,
                    "Amount paid cannot be greater than the payable amount (" + String.format("%.2f", basePayable) + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        payableAmount = totalAmount - (totalDiscount * totalAmount) / 100 - amountPaid;
        updateTotalTextViews();
    }

    // Update the UI display for totals and amount paid
    private void updateTotalTextViews() {
        totalAmountTextView.setText("" + totalAmount);
        totalDiscountEditText.setText("" + totalDiscount);
        payableAmountTextView.setText("" + payableAmount);
        // Also update the AmountPaid field so it displays the formatted value
        amountPaidEditText.setText("" + amountPaid);
    }

    // Clear input fields after an item is added or updated
    private void clearInputFields() {
        itemNameEditText.setText("");
        quantityEditText.setText("");
        pricePerItemEditText.setText("");
        discountEditText.setText("");
    }
    private void reindexRows() {
        int childCount = itemTable.getChildCount();
        for (int i = 1; i < childCount; i++) { // index 0 is header.
            TableRow row = (TableRow) itemTable.getChildAt(i);
            TextView serialText = (TextView) row.getChildAt(0);
            serialText.setText(String.valueOf(i));
        }
    }
}
