package com.example.finalproject;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.finalproject.ui.calendar.CalendarFragment;
import com.example.finalproject.ui.calendar.EventEditFragment;
import com.example.finalproject.ui.common.PlaceholderFragment;
import com.example.finalproject.ui.todo.TodoDetailFragment;
import com.example.finalproject.ui.todo.TodoEditFragment;
import com.example.finalproject.ui.todo.TodoFragment;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private View bottomNavigationView;
    private View mainRoot;
    private View fab;
    private FrameLayout navCalendar;
    private FrameLayout navTodo;
    private FrameLayout navHabits;
    private FrameLayout navJournal;
    private ImageView iconCalendar;
    private ImageView iconTodo;
    private ImageView iconHabits;
    private ImageView iconJournal;
    private View indicatorCalendar;
    private View indicatorTodo;
    private View indicatorHabits;
    private View indicatorJournal;
    private int currentTabId = R.id.nav_calendar;
    private LocalDate selectedDate = LocalDate.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mainRoot = findViewById(R.id.main);
        bottomNavigationView = findViewById(R.id.custom_bottom_navigation);
        fab = findViewById(R.id.main_fab);
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNavigationView.getLayoutParams();
            if (params.bottomMargin != systemBars.bottom) {
                params.bottomMargin = systemBars.bottom;
                bottomNavigationView.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(mainRoot);
        navCalendar = findViewById(R.id.nav_calendar_button);
        navTodo = findViewById(R.id.nav_todo_button);
        navHabits = findViewById(R.id.nav_habits_button);
        navJournal = findViewById(R.id.nav_journal_button);
        iconCalendar = findViewById(R.id.nav_calendar_icon);
        iconTodo = findViewById(R.id.nav_todo_icon);
        iconHabits = findViewById(R.id.nav_habits_icon);
        iconJournal = findViewById(R.id.nav_journal_icon);
        indicatorCalendar = findViewById(R.id.nav_calendar_indicator);
        indicatorTodo = findViewById(R.id.nav_todo_indicator);
        indicatorHabits = findViewById(R.id.nav_habits_indicator);
        indicatorJournal = findViewById(R.id.nav_journal_indicator);
        setupNavigation();
        setupBackStackVisibility();
        fab.setOnClickListener(v -> onFabClicked());

        if (savedInstanceState == null) {
            showCalendar();
        }
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void openEventEditor(long eventId, LocalDate date) {
        openFullScreen(EventEditFragment.newInstance(eventId, date == null ? selectedDate : date));
    }

    public void openTodoEditor(long todoId, LocalDate date) {
        openFullScreen(TodoEditFragment.newInstance(todoId, date == null ? selectedDate : date));
    }

    public void openTodoDetail(long todoId) {
        openFullScreen(TodoDetailFragment.newInstance(todoId));
    }

    public void finishFullScreen() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStack();
        }
    }

    public void finishToHome() {
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (currentTabId == R.id.nav_todo) {
            showTodo();
        } else if (currentTabId == R.id.nav_habits) {
            showPlaceholder(getString(R.string.placeholder_habit));
        } else if (currentTabId == R.id.nav_journal) {
            showPlaceholder(getString(R.string.placeholder_journal));
        } else {
            showCalendar();
        }
    }

    public void switchFullScreen(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setupNavigation() {
        navCalendar.setOnClickListener(v -> selectTab(R.id.nav_calendar));
        navTodo.setOnClickListener(v -> selectTab(R.id.nav_todo));
        navHabits.setOnClickListener(v -> selectTab(R.id.nav_habits));
        navJournal.setOnClickListener(v -> selectTab(R.id.nav_journal));
    }

    private void selectTab(int id) {
        currentTabId = id;
        if (id == R.id.nav_calendar) {
            showCalendar();
        } else if (id == R.id.nav_todo) {
            showTodo();
        } else if (id == R.id.nav_habits) {
            showPlaceholder(getString(R.string.placeholder_habit));
        } else if (id == R.id.nav_journal) {
            showPlaceholder(getString(R.string.placeholder_journal));
        }
    }

    private void setupBackStackVisibility() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean fullScreen = getSupportFragmentManager().getBackStackEntryCount() > 0;
            bottomNavigationView.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
            fab.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
        });
    }

    private void onFabClicked() {
        if (currentTabId == R.id.nav_todo) {
            openTodoEditor(0, selectedDate);
        } else if (currentTabId == R.id.nav_calendar) {
            openEventEditor(0, selectedDate);
        }
    }

    private void showCalendar() {
        replaceHome(new CalendarFragment());
    }

    private void showTodo() {
        replaceHome(TodoFragment.newInstance(selectedDate));
    }

    private void showPlaceholder(String message) {
        replaceHome(PlaceholderFragment.newInstance(message));
    }

    private void replaceHome(Fragment fragment) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        bottomNavigationView.setVisibility(View.VISIBLE);
        fab.setVisibility(currentTabId == R.id.nav_habits || currentTabId == R.id.nav_journal ? View.GONE : View.VISIBLE);
        updateNavSelection();
    }

    private void openFullScreen(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(fragment.getClass().getSimpleName())
                .commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void updateNavSelection() {
        int selectedFill = getColor(R.color.brand_orange);
        int dark = getColor(R.color.text_primary);
        iconCalendar.setColorFilter(currentTabId == R.id.nav_calendar ? selectedFill : dark);
        iconTodo.setColorFilter(currentTabId == R.id.nav_todo ? selectedFill : dark);
        iconHabits.setColorFilter(currentTabId == R.id.nav_habits ? selectedFill : dark);
        iconJournal.setColorFilter(currentTabId == R.id.nav_journal ? selectedFill : dark);
        indicatorCalendar.setVisibility(currentTabId == R.id.nav_calendar ? View.VISIBLE : View.GONE);
        indicatorTodo.setVisibility(currentTabId == R.id.nav_todo ? View.VISIBLE : View.GONE);
        indicatorHabits.setVisibility(currentTabId == R.id.nav_habits ? View.VISIBLE : View.GONE);
        indicatorJournal.setVisibility(currentTabId == R.id.nav_journal ? View.VISIBLE : View.GONE);
    }
}
