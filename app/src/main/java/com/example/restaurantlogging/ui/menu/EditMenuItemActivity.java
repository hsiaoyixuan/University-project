package com.example.restaurantlogging.ui.menu;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.restaurantlogging.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EditMenuItemActivity extends AppCompatActivity {
    // UI组件声明
    private TextView nameTextView;
    private EditText chineseEditText, numberEditText, calEditText, proteinEditText, sugarEditText, totalCrabEditText, fatEditText;
    private Button addButton, updateButton, deleteButton;
    private ImageView imageView;
    private Switch closeSwitch;
    private CheckBox p_rice, p_noodles, p_cheese, p_shacha;

    // Firebase引用声明
    private DatabaseReference menuRef;
    private StorageReference storageReference;

    // 变量声明
    private String itemName, itemDescription, itemDetail;
    private String pendingAction; // 待执行的操作
    private String imageUrl; // 图片URL

    // 图片选择请求码
    private static final int PICK_IMAGE_REQUEST = 1;

    // FoodItem类，包含菜单项的属性
    public static class FoodItem {
        // 默认构造函数
        public FoodItem() {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_menu_item);

        // 初始化Firebase Storage引用
        storageReference = FirebaseStorage.getInstance().getReference();
        // 從Intent傳遞過來的數據中獲取菜單路徑
        String menuPath = getIntent().getStringExtra("menuPath");
        if (menuPath != null) {
            menuRef = FirebaseDatabase.getInstance().getReference(menuPath);
        }
        // 顯示提示用戶的 AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("請注意!!修改內容不可修改名稱")
                .setPositiveButton("確認", (dialog, which) -> {
                    // 點擊「確認」後的操作，這裡不需要執行任何操作，只是關閉對話框
                })
                .setCancelable(false) // 防止用戶按返回鍵關閉對話框
                .show();

        // 初始化UI组件
        nameTextView = findViewById(R.id.textViewName);
        chineseEditText = findViewById(R.id.editTextChinese);
        numberEditText = findViewById(R.id.editText_price);
        calEditText = findViewById(R.id.editTextcal);
        proteinEditText = findViewById(R.id.editText_protein);
        sugarEditText = findViewById(R.id.editText_sugar);
        totalCrabEditText = findViewById(R.id.editText_total_carb);
        fatEditText = findViewById(R.id.editText_fat);
        addButton = findViewById(R.id.addButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        imageView = findViewById(R.id.firebaseimage);
        Button changeImageButton = findViewById(R.id.change_btn);
        closeSwitch = findViewById(R.id.switch1);

        // 从SharedPreferences读取Switch状态，默认值为false
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isClosed = sharedPreferences.getBoolean("switch_state", false);
        closeSwitch.setChecked(isClosed);

        // 设置Switch监听器以保存状态到SharedPreferences
        closeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("switch_state", isChecked);
            editor.apply();

            String chinese = chineseEditText.getText().toString();
            String name = chinese;
            String on_off = "Closed";
            if (isChecked) {
                menuRef.child(itemName).child(name).child(on_off).setValue(true);
            } else {
                menuRef.child(itemName).child(name).child(on_off).setValue(false);
            }
        });

        // 获取 Intent 传递的标志
        boolean isNew = getIntent().getBooleanExtra("isNew", false);
        if (isNew) {
            addButton.setVisibility(View.VISIBLE);
            updateButton.setVisibility(View.GONE);
        } else {
            addButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
        }

        // 获取从Intent传递过来的数据
        itemName = getIntent().getStringExtra("itemName");
        itemDescription = getIntent().getStringExtra("itemDescription");
        itemDetail = getIntent().getStringExtra("itemDetail");
        if (itemName != null) {
            chineseEditText.setText(itemName);
        }

        if (itemName != null) nameTextView.setText(itemName);
        if (itemDescription != null) {
            String chineseText = itemDescription.contains(":") ? itemDescription.split(":")[0] : itemDescription;
            chineseEditText.setText(chineseText);
            String numberText = itemDetail.split("價格=")[1].split(",")[0].replaceAll("[^0-9.]", "");
            numberEditText.setText(numberText);
            calEditText.setText(extractDetail(itemDetail, "熱量"));
            proteinEditText.setText(extractDetail(itemDetail, "蛋白質"));
            sugarEditText.setText(extractDetail(itemDetail, "糖"));
            totalCrabEditText.setText(extractDetail(itemDetail, "碳水化合物"));
            fatEditText.setText(extractDetail(itemDetail, "脂肪"));
        }

        if (itemName != null) {
            loadPhotoFromFirebase();
        }

        addButton.setOnClickListener(v -> {
            pendingAction = "add";
            showConfirmDialog();
        });

        updateButton.setOnClickListener(v -> {
            pendingAction = "update";
            showConfirmDialog();
        });

        deleteButton.setOnClickListener(v -> {
            pendingAction = "delete";
            showConfirmDialog();
        });

        changeImageButton.setOnClickListener(v -> openFileChooser());
        loadStoredImageUrl();

        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    try {
                        FoodItem item = childSnapshot.getValue(FoodItem.class);
                    } catch (DatabaseException e) {
                        Log.e("Firebase", "Error reading FoodItem", e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditMenuItemActivity.this, "Failed to load food items.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        menuRef.child(itemName).child("closed").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isClosed = snapshot.getValue(Boolean.class);
                if (isClosed != null) {
                    closeSwitch.setChecked(isClosed);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditMenuItemActivity.this, "Failed to load switch state.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractDetail(String itemDetail, String key) {
        return itemDetail.replaceAll(".*" + key + "=", "").replaceAll(",.*", "");
    }

    private void loadStoredImageUrl() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String imageUrl = sharedPreferences.getString("imageUrl", null);
        if (imageUrl != null) {
            Picasso.get().load(imageUrl).into(imageView);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            Picasso.get().load(imageUri).into(imageView);
            imageView.setTag(imageUri);
            storeImageUri(imageUri);
        }
    }

    private void storeImageUri(Uri imageUri) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("imageUri", imageUri.toString());
        editor.apply();
    }

    private void storeImageUrl(String imageUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("imageUrl", imageUrl);
        editor.apply();
    }

    private void uploadImageToFirebase() {
        Uri imageUri = (Uri) imageView.getTag();
        if (imageUri == null) {
            saveMenuItem();
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            File jpegFile = new File(getCacheDir(), "image.jpg");
            try (OutputStream os = new FileOutputStream(jpegFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            }

            Uri jpegUri = Uri.fromFile(jpegFile);
            StorageReference fileReference = storageReference.child("uploads/" + System.currentTimeMillis() + ".jpg");

            fileReference.putFile(jpegUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageUrl = uri.toString();
                        Toast.makeText(EditMenuItemActivity.this, "新增成功!!", Toast.LENGTH_LONG).show();
                        Picasso.get().load(uri).into(imageView);
                        storeImageUrl(imageUrl);
                        saveMenuItem();
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditMenuItemActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showConfirmDialog() {
        String message;
        switch (pendingAction) {
            case "add":
                message = "你確定要新增此項目嗎？";
                break;
            case "update":
                message = "你確定要修改此項目嗎？";
                break;
            case "delete":
                message = "你確定要刪除此項目嗎？";
                break;
            default:
                message = "你確定要進行此操作嗎？";
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("確認操作")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if ("add".equals(pendingAction) || "update".equals(pendingAction)) {
                        uploadImageToFirebase();
                    } else if ("delete".equals(pendingAction)) {
                        deleteMenuItem();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void saveMenuItem() {
        String chinese = chineseEditText.getText().toString();
        float price = Float.parseFloat(numberEditText.getText().toString());
        float cal = Float.parseFloat(calEditText.getText().toString());
        float protein = Float.parseFloat(proteinEditText.getText().toString());
        float sugar = Float.parseFloat(sugarEditText.getText().toString());
        float total_crab = Float.parseFloat(totalCrabEditText.getText().toString());
        float fat = Float.parseFloat(fatEditText.getText().toString());

        DatabaseReference itemRef = menuRef.child(itemName).child(chinese);
        itemRef.child("價格").setValue(price);
        itemRef.child("熱量").setValue(cal);
        itemRef.child("蛋白質").setValue(protein);
        itemRef.child("糖").setValue(sugar);
        itemRef.child("碳水化合物").setValue(total_crab);
        itemRef.child("脂肪").setValue(fat);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            itemRef.child("照片").setValue(imageUrl);
        }

        Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void copyItemName(String oldName, String newName) {
        DatabaseReference oldRef = menuRef.child(oldName);
        DatabaseReference newRef = menuRef.child(newName);

        oldRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    newRef.setValue(snapshot.getValue()).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditMenuItemActivity.this, "品項名稱已複製", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to copy data to new node");
                        }
                    });
                } else {
                    Log.e(TAG, "未找到原始項目");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void deleteMenuItem() {
        String chinese = chineseEditText.getText().toString();
        menuRef.child(itemName).child(chinese).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show());
    }

    private void loadPhotoFromFirebase() {
        String chinese = chineseEditText.getText().toString();
        menuRef.child(itemName).child(chinese).child("照片").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrl = snapshot.getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // 如果有圖片URL，顯示圖片
                    Picasso.get().load(imageUrl).into(imageView);
                } else {
                    // 如果沒有圖片URL，顯示預設圖片
                    imageView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditMenuItemActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                // 在錯誤情況下，也顯示預設圖片
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }
        });
    }

}
