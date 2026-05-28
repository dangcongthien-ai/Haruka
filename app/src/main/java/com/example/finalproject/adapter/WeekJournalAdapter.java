package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.ui.common.UiUtils;
import com.example.finalproject.util.JournalMoodUtils;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeekJournalAdapter extends RecyclerView.Adapter<WeekJournalAdapter.JournalViewHolder> {
    private final List<LocalDate> dates = new ArrayList<>();
    private final Map<LocalDate, String> journalDayMoods = new HashMap<>();

    public void submit(List<LocalDate> newDates, Map<LocalDate, String> moodNamesByDate) {
        dates.clear();
        dates.addAll(newDates);
        journalDayMoods.clear();
        if (moodNamesByDate != null) {
            journalDayMoods.putAll(moodNamesByDate);
        }
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
        int iconRes = JournalMoodUtils.resolveMoodResource(
                holder.itemView.getContext(),
                journalDayMoods.get(dates.get(position))
        );
        if (iconRes != 0) {
            holder.icon.setImageResource(iconRes);
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setImageDrawable(null);
            holder.icon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout card;
        final ImageView icon;

        JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.week_journal_slot_card);
            icon = itemView.findViewById(R.id.img_week_journal_icon);
        }
    }
}
