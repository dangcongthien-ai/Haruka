package com.example.finalproject.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;

public class PlaceholderFragment extends Fragment {
    private static final String ARG_MESSAGE = "message";

    public static PlaceholderFragment newInstance(String message) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView message = view.findViewById(R.id.tv_placeholder_message);
        message.setText(requireArguments().getString(ARG_MESSAGE));
        return view;
    }
}
