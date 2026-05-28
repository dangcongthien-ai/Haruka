package com.example.finalproject.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.data.DateTimeUtils;
import com.example.finalproject.model.JournalEntry;
import com.example.finalproject.util.JournalMoodUtils;
import com.example.finalproject.util.JournalTextUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JournalEntryAdapter extends RecyclerView.Adapter<JournalEntryAdapter.JournalViewHolder> {
    public interface Listener {
        void onClick(JournalEntry entry);

        void onEdit(JournalEntry entry);

        void onDelete(JournalEntry entry);
    }

    private static final long LAYOUT_DENIM = 2L;
    private static final long LAYOUT_PLAID = 3L;

    private final List<JournalEntry> entries = new ArrayList<>();
    private final Listener listener;

    public JournalEntryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<JournalEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal_entry, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = entries.get(position);
        Context context = holder.itemView.getContext();
        LocalDate date = entry.getJournalDate();
        holder.date.setText(formatFullDate(context, date));
        holder.pattern.setImageResource(patternResource(entry.getLayoutId()));
        bindMoods(context, holder, entry.getMoodResourceNames());
        holder.title.setText(displayTitle(context, entry), TextView.BufferType.SPANNABLE);
        holder.title.setGravity(titleGravity(entry));
        holder.preview.setText(displayPreview(context, entry), TextView.BufferType.SPANNABLE);
        holder.preview.setGravity(previewGravity(entry));
        bindImages(holder, entry.getImageUris());
        holder.card.setOnClickListener(v -> listener.onClick(entry));
        holder.edit.setOnClickListener(v -> listener.onEdit(entry));
        holder.delete.setOnClickListener(v -> listener.onDelete(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private CharSequence displayTitle(Context context, JournalEntry entry) {
        if (!isBlank(JournalTextUtils.plainText(entry.getCaption()))) {
            return JournalTextUtils.fromStoredContent(entry.getCaption());
        }
        String plainTitle = JournalTextUtils.plainText(entry.getTitle());
        return isBlank(plainTitle) ? context.getString(R.string.journal_untitled) : plainTitle;
    }

    private int titleGravity(JournalEntry entry) {
        if (isBlank(JournalTextUtils.plainText(entry.getCaption()))) {
            return Gravity.CENTER;
        }
        Layout.Alignment alignment = JournalTextUtils.alignmentFromStoredContent(entry.getCaption());
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            return Gravity.CENTER;
        }
        if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
            return Gravity.END | Gravity.CENTER_VERTICAL;
        }
        return Gravity.START | Gravity.CENTER_VERTICAL;
    }

    private CharSequence displayPreview(Context context, JournalEntry entry) {
        if (isBlank(JournalTextUtils.plainText(entry.getContent()))) {
            return context.getString(R.string.journal_empty_preview);
        }
        return JournalTextUtils.fromStoredContent(entry.getContent());
    }

    private int previewGravity(JournalEntry entry) {
        Layout.Alignment alignment = firstAlignment(JournalTextUtils.fromStoredContent(entry.getContent()));
        if (alignment == Layout.Alignment.ALIGN_CENTER) {
            return Gravity.CENTER;
        }
        if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
            return Gravity.END | Gravity.CENTER_VERTICAL;
        }
        return Gravity.START | Gravity.CENTER_VERTICAL;
    }

    private Layout.Alignment firstAlignment(CharSequence value) {
        if (!(value instanceof Spanned)) {
            return Layout.Alignment.ALIGN_NORMAL;
        }
        Spanned spanned = (Spanned) value;
        AlignmentSpan[] spans = spanned.getSpans(0, spanned.length(), AlignmentSpan.class);
        if (spans.length == 0) {
            return Layout.Alignment.ALIGN_NORMAL;
        }
        return spans[0].getAlignment();
    }

    private void bindImages(JournalViewHolder holder, List<String> imageUris) {
        for (int index = 0; index < holder.photoSlots.length; index++) {
            String imageUri = imageUris != null && index < imageUris.size() ? imageUris.get(index) : "";
            boolean hasImage = !isBlank(imageUri);
            holder.photoSlots[index].setVisibility(hasImage ? View.VISIBLE : View.GONE);
            holder.photoViews[index].setImageDrawable(null);
            if (hasImage) {
                holder.photoViews[index].setImageURI(Uri.parse(imageUri));
            }
        }
    }

    private void bindMoods(Context context, JournalViewHolder holder, List<String> moodResourceNames) {
        int[] fallback = {
                R.drawable.journal_emo_nang,
                R.drawable.journal_emo_vui,
                R.drawable.journal_emo_sang_khoai
        };
        ImageView[] moodViews = {
                holder.weatherMood,
                holder.dayMood,
                holder.bodyMood
        };
        for (int index = 0; index < moodViews.length; index++) {
            String resourceName = moodResourceNames != null && index < moodResourceNames.size()
                    ? moodResourceNames.get(index)
                    : "";
            int resourceId = JournalMoodUtils.resolveMoodResource(context, resourceName);
            moodViews[index].setImageResource(resourceId != 0 ? resourceId : fallback[index]);
        }
    }

    private int patternResource(Long layoutId) {
        if (layoutId != null && layoutId == LAYOUT_DENIM) {
            return R.drawable.journal_layout_write_2_pattern;
        }
        if (layoutId != null && layoutId == LAYOUT_PLAID) {
            return R.drawable.journal_layout_2_pattern;
        }
        return R.drawable.journal_layout_1_pattern;
    }

    private String formatFullDate(Context context, LocalDate date) {
        if (date == null) {
            return "";
        }
        return String.format(
                Locale.US,
                "%s, %s",
                weekdayName(context, date),
                DateTimeUtils.formatVietnameseDate(date)
        );
    }

    private String weekdayName(Context context, LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
                return context.getString(R.string.weekday_full_monday);
            case TUESDAY:
                return context.getString(R.string.weekday_full_tuesday);
            case WEDNESDAY:
                return context.getString(R.string.weekday_full_wednesday);
            case THURSDAY:
                return context.getString(R.string.weekday_full_thursday);
            case FRIDAY:
                return context.getString(R.string.weekday_full_friday);
            case SATURDAY:
                return context.getString(R.string.weekday_full_saturday);
            case SUNDAY:
            default:
                return context.getString(R.string.weekday_full_sunday);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final TextView date;
        final TextView title;
        final TextView preview;
        final ImageView pattern;
        final ImageView weatherMood;
        final ImageView dayMood;
        final ImageView bodyMood;
        final FrameLayout[] photoSlots;
        final ImageView[] photoViews;
        final ImageButton edit;
        final ImageButton delete;

        JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.journal_card);
            date = itemView.findViewById(R.id.tv_journal_item_date);
            title = itemView.findViewById(R.id.tv_journal_title);
            preview = itemView.findViewById(R.id.tv_journal_preview);
            pattern = itemView.findViewById(R.id.img_journal_card_pattern);
            weatherMood = itemView.findViewById(R.id.img_journal_mood_weather);
            dayMood = itemView.findViewById(R.id.img_journal_mood_day);
            bodyMood = itemView.findViewById(R.id.img_journal_mood_body);
            photoSlots = new FrameLayout[]{
                    itemView.findViewById(R.id.journal_entry_photo_slot_1),
                    itemView.findViewById(R.id.journal_entry_photo_slot_2),
                    itemView.findViewById(R.id.journal_entry_photo_slot_3)
            };
            photoViews = new ImageView[]{
                    itemView.findViewById(R.id.img_journal_photo_1),
                    itemView.findViewById(R.id.img_journal_photo_2),
                    itemView.findViewById(R.id.img_journal_photo_3)
            };
            edit = itemView.findViewById(R.id.btn_journal_edit);
            delete = itemView.findViewById(R.id.btn_journal_delete);
        }
    }
}
