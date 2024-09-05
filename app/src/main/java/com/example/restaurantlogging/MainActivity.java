package com.example.restaurantlogging;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private EditText editTextEmail, editTextPassword;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    private CheckBox keepAccount_checkBox;
    // 新增 SharedPreferences 物件
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // 用户已经登录，跳转到 home 活动
            Intent intent = new Intent(getApplicationContext(), home.class);
            intent.putExtra("uid", currentUser.getUid());
            startActivity(intent);
            finish();
            return;
        }
        // 檢查是否有保存的帳號和密碼
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (isRemembered) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            editTextEmail.setText(savedEmail);
            editTextPassword.setText(savedPassword);
            keepAccount_checkBox.setChecked(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.login);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        keepAccount_checkBox = findViewById(R.id.checkBox);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "請輸入帳號", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "請輸入密碼", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "登入成功", Toast.LENGTH_SHORT).show();
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        String uid = user.getUid();
                                        Intent intent = new Intent(getApplicationContext(), home.class);
                                        intent.putExtra("uid", uid);
                                        // 保存帳號和密碼
                                        if (keepAccount_checkBox.isChecked()) {
                                            editor = sharedPreferences.edit();
                                            editor.putString(KEY_EMAIL, email);
                                            editor.putString(KEY_PASSWORD, password);
                                            editor.putBoolean(KEY_REMEMBER, true);
                                            editor.apply();
                                        } else {
                                            editor = sharedPreferences.edit();
                                            editor.clear();
                                            editor.apply();
                                        }
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, "身分驗證失敗" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "身分驗證失敗" + task.getException().getMessage());
                                }
                            }
                        });
            }
        });
    }
}
