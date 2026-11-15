
package com.example.test;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.test.models.User;

import java.util.Random;


import android.net.Uri;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class MainActivity extends AppCompatActivity {

    private User user;


    private ImageView selectedImage;
    private EditText shareText;

    private Uri imageUri;

    private ActivityResultLauncher<String> imagePicker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        selectedImage = findViewById(R.id.selectedImage);
        shareText = findViewById(R.id.shareText);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;

                        selectedImage.setImageURI(uri);
                        selectedImage.setVisibility(View.VISIBLE);
                    }
                }
        );

        Button selectImageButton = findViewById(R.id.selectImageButton);

        selectImageButton.setOnClickListener(v -> {
            imagePicker.launch("image/*");
        });


        Button shareButton = findViewById(R.id.shareButton);

        shareButton.setOnClickListener(v -> {

            String text = shareText.getText().toString();

            Intent intent = new Intent(Intent.ACTION_SEND);

            if (imageUri != null) {
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                intent.putExtra(Intent.EXTRA_TEXT, text);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, text);
            }

            Intent chooser = Intent.createChooser(intent, "Поширити через");
            startActivity(chooser);
        });


        if (savedInstanceState == null) {
            user = new User("Alex", 20, "Kyiv");
        } else {
            user = (User) savedInstanceState.getSerializable("user");
        }

        Button openEditorButton = findViewById(R.id.openEditorButton);

        openEditorButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PhotoEditorActivity.class);
            startActivity(intent);
        });

        GridLayout grid = findViewById(R.id.grid);

        Random random = new Random();

        for (int i = 0; i < 50; i++) {

            Button button = new Button(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 100;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(3, 3, 3, 3);

            button.setLayoutParams(params);
            button.setText("");

            button.setOnClickListener(v -> {
                int color = Color.rgb(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256)
                );

                button.setBackgroundColor(color);
            });

            grid.addView(button);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("user", user);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        user = (User) savedInstanceState.getSerializable("user");
    }

    private int counter = 0;

    public void fun(View view) {
        counter++;

        Button b = (Button) view;
        b.setText(String.valueOf(counter));

        Toast.makeText(this, "Clicks: " + counter, Toast.LENGTH_SHORT).show();
    }
}
