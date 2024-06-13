package com.example.restaurantlogging.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements View.OnClickListener {
   private Button p ;
   private Button npc ;
   private TextView h;
   private TextView np;
   private EditText posew;

private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        p=root.findViewById(R.id.post);
        np=root.findViewById(R.id.new_post);
        h=root.findViewById(R.id.Home);
        npc=root.findViewById(R.id.npcheck);
        posew=root.findViewById(R.id.post_word);
        p.setOnClickListener(this);

        final TextView textView = binding.newPost;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;



    }



    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.post)
        {
            np.setVisibility(View.VISIBLE);
            h.setVisibility(View.GONE);
            npc.setVisibility(View.VISIBLE);
            posew.setVisibility(View.VISIBLE);

            np.setText("發布貼文" );
        }

    }
}