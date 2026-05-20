package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeekJournalAdapter extends RecyclerView.Adapter<WeekJournalAdapter.JournalViewHolder> {
    private final List<LocalDate> dates = new ArrayList<>();

    public void submit(List<LocalDate> newDates) {
        dates.clear();
        dates.addAll(newDates);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_journal_slot, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        holder.card.setBackground(UiUtils.roundedStroke(
                holder.itemView.getContext().getColor(R.color.surface_soft),
                holder.itemView.getContext().getColor(R.color.line),
                11,
                holder.itemView.getContext()
        ));
        holder.card.setAlpha(0.9f);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout card;

        JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.week_journal_slot_card);
        }
    }
}
