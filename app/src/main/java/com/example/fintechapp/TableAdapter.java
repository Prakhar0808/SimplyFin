// TableAdapter.java
package com.example.fintechapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // for delete icon if needed per row
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.TableViewHolder> {

    private List<TableItem> tableItems;
    // A set to hold positions of selected items
    private Set<Integer> selectedPositions = new HashSet<>();
    // Flag to indicate if multi-select mode is active.
    private boolean multiSelectMode = false;
    private OnTableItemSelectedListener selectionListener;
    private Context context;

    public interface OnTableItemSelectedListener {
        void onSelectionChanged(int count);
    }

    public TableAdapter(List<TableItem> tableItems, Context context, OnTableItemSelectedListener listener) {
        this.tableItems = tableItems;
        this.context = context;
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public TableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table, parent, false);
        return new TableViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TableViewHolder holder, int position) {
        TableItem item = tableItems.get(position);
        holder.textViewTitle.setText(item.getTitle());
        holder.textViewModifiedTime.setText("Modified: " + item.getModifiedTime());

        if (selectedPositions.contains(position)) {
            // When selected, show light blue.
            holder.itemView.setBackgroundColor(Color.parseColor("#ADD8E6"));
        } else {
            // Not selected: color-code based on payable amount.
            if (item.getPayableAmount() == 0) {
                holder.itemView.setBackgroundColor(Color.parseColor("#c9f18a")); // Green if payable is zero.
            } else if (item.getTotalAmount() > 0 && item.getPayableAmount() > 0.7 * item.getTotalAmount()) {
                holder.itemView.setBackgroundColor(Color.parseColor("#f7997f")); // Red if payable > 70% of total.
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectMode) {
                multiSelectMode = true;
                toggleSelection(position);
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(selectedPositions.size());
                }
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectMode) {
                toggleSelection(position);
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(selectedPositions.size());
                }
            } else {
                // Normal click: open the edit activity.
                Intent intent = new Intent(context, EditItemActivity.class);
                intent.putExtra("tableId", item.getKey());
                intent.putExtra("title", item.getTitle());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tableItems.size();
    }

    // Toggle the selection state of the item at the given position.
    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    // Clear all selections.
    public void clearSelections() {
        selectedPositions.clear();
        multiSelectMode = false;
        notifyDataSetChanged();
        if(selectionListener != null){
            selectionListener.onSelectionChanged(0);
        }
    }

    // Return a list of selected TableItems.
    public List<TableItem> getSelectedItems() {
        // Build a list of items from the selected positions.
        List<TableItem> selectedItems = new java.util.ArrayList<>();
        for (Integer pos : selectedPositions) {
            selectedItems.add(tableItems.get(pos));
        }
        return selectedItems;
    }

    // Check if any items are selected.
    public boolean hasSelections() {
        return !selectedPositions.isEmpty();
    }

    public class TableViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewModifiedTime;

        public TableViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewModifiedTime = itemView.findViewById(R.id.textViewModifiedTime);
        }
    }
}
