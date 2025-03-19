package com.example.fintechapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class FullScreenTableActivity extends AppCompatActivity {

    private TableLayout fullScreenTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_table);

        // Optional: Hide system UI for immersive experience.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        fullScreenTable = findViewById(R.id.fullScreenTable);

        // Set up the back button (ensure your layout has an ImageButton with id "backButton")
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Retrieve unsaved table content passed from the previous activity.
        Serializable extra = getIntent().getSerializableExtra("tableContent");
        if (extra != null && extra instanceof ArrayList) {
            ArrayList<Map<String, Object>> tableContent = (ArrayList<Map<String, Object>>) extra;
            populateTable(tableContent);
        } else {
            Toast.makeText(this, "No table content available", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateTable(ArrayList<Map<String, Object>> tableContent) {
        fullScreenTable.removeAllViews();

        // Add header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        headerRow.addView(createCell("S.No"));
        headerRow.addView(createCell("Item Name"));
        headerRow.addView(createCell("Quantity"));
        headerRow.addView(createCell("Price"));
        headerRow.addView(createCell("Discount"));
        headerRow.addView(createCell("Total"));
        fullScreenTable.addView(headerRow);

        int index = 1;
        for (Map<String, Object> data : tableContent) {
            TableRow row = new TableRow(this);
            row.setBackgroundColor(index % 2 == 0 ? 0xFFD3D3D3 : 0xFFFFFFFF); // light gray vs white
            row.addView(createCell(String.valueOf(index)));
            row.addView(createCell(String.valueOf(data.get("itemName"))));
            row.addView(createCell(String.valueOf(data.get("quantity"))));

            // For numeric fields, convert the value to a double if possible.
            double price = getDoubleFromObject(data.get("pricePerItem"));
            double discount = getDoubleFromObject(data.get("discount"));
            double total = getDoubleFromObject(data.get("totalPrice"));

            row.addView(createCell(String.format(Locale.getDefault(), "%.2f", price)));
            row.addView(createCell(String.format(Locale.getDefault(), "%.0f", discount) + "%"));
            row.addView(createCell(String.format(Locale.getDefault(), "%.2f", total)));
            fullScreenTable.addView(row);
            index++;
        }
    }

    private double getDoubleFromObject(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private TextView createCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setPadding(8, 8, 8, 8);
        cell.setGravity(Gravity.CENTER);
        cell.setMaxLines(1);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return cell;
    }
}
