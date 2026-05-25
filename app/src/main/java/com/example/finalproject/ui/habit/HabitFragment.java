package com.example.finalproject.ui.habit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.MainActivity;
import com.example.finalproject.R;
import com.example.finalproject.adapter.HabitAdapter;
import com.example.finalproject.repository.HabitRepository;
import com.example.finalproject.ui.common.HomeDataRefreshable;
import com.example.finalproject.ui.common.UiUtils;

import java.time.LocalDate;

public class HabitFragment extends Fragment implements HomeDataRefreshable {
    private HabitRepository repository;
    private HabitAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;

    public static HabitFragment newInstance() {
        return new HabitFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit, container, false);
        repository = new HabitRepository(requireContext());
        recyclerView = view.findViewById(R.id.recycler_habits);
        emptyView = view.findViewById(R.id.layout_habit_empty);
        adapter = new HabitAdapter(item -> ((MainActivity) requireActivity()).openHabitEditor(item.getId()));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onHomeDataRefresh(LocalDate selectedDate) {
        refresh();
    }

    private void refresh() {
        if (adapter == null) {
            return;
        }
        adapter.submit(repository.getHabits());
        boolean empty = adapter.getItemCount() == 0;
        UiUtils.visible(emptyView, empty);
        UiUtils.visible(recyclerView, !empty);
    }
}
