package com.example.restaurantlogging.ui.menu;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
        menuRef = FirebaseDatabase.getInstance().getReference("校區/屏商校區/美琪晨餐館/食物");

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
        p_rice = findViewById(R.id.p_rice);
        p_noodles = findViewById(R.id.p_noodles);
        p_cheese = findViewById(R.id.p_cheese);
        p_shacha = findViewById(R.id.p_shacha);

        // 从SharedPreferences读取Switch状态，默认值为false
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isClosed = sharedPreferences.getBoolean("switch_state", false);
        closeSwitch.setChecked(isClosed);

        // 设置Switch监听器以保存状态到SharedPreferences
        closeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 保存Switch状态到SharedPreferences
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

        // 加载CheckBox状态
        loadCheckBoxStates();

        // 获取从Intent传递过来的数据
        itemName = getIntent().getStringExtra("itemName");
        itemDescription = getIntent().getStringExtra("itemDescription");
        itemDetail = getIntent().getStringExtra("itemDetail");

        // 将数据设置到UI组件中
        if (itemName != null) nameTextView.setText(itemName);

        if (itemDescription != null) {
            // 提取描述中的中文和其他信息
            String chineseText = itemDescription.replaceAll("[^\\u4e00-\\u9fa5]", "");
            chineseEditText.setText(chineseText);
            String numberText = itemDetail.split("價格=")[1].split(",")[0].replaceAll("[^0-9.]", ""); // 提取数字部分
            numberText = numberText.replaceAll(",.*", "");
            numberEditText.setText(numberText); // 设置数字到第二个 EditText
            calEditText.setText(extractDetail(itemDetail, "熱量"));
            proteinEditText.setText(extractDetail(itemDetail, "蛋白質"));
            sugarEditText.setText(extractDetail(itemDetail, "糖"));
            totalCrabEditText.setText(extractDetail(itemDetail, "碳水化合物"));
            fatEditText.setText(extractDetail(itemDetail, "脂肪"));
        }

        // 从Firebase加载照片并显示
        if (itemName != null) {
            loadPhotoFromFirebase();
        }

        // 设置按钮点击事件
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

        // 添加Firebase Database监听器
        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    try {
                        FoodItem item = childSnapshot.getValue(FoodItem.class);
                        // 在这里可以处理FoodItem对象
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
        // 重新讀取 Switch 狀態
        menuRef.child(itemName).child("closed").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isClosed = snapshot.getValue(Boolean.class); // 從Firebase獲取closed狀態
                if (isClosed != null) {
                    closeSwitch.setChecked(isClosed); // 設置Switch狀態
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditMenuItemActivity.this, "Failed to load switch state.", Toast.LENGTH_SHORT).show();
            }
        });

        // 加载CheckBox状态
        loadCheckBoxStates();
    }

    // 从详细信息中提取特定键的值
    private String extractDetail(String itemDetail, String key) {
        return itemDetail.replaceAll(".*" + key + "=", "").replaceAll(",.*", "");
    }

    private void loadStoredImageUrl() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String imageUrl = sharedPreferences.getString("imageUrl", null);

        if (imageUrl != null) {
            // 使用 Picasso 将图片加载到 ImageView 中
            Picasso.get().load(imageUrl).into(imageView);
        }
    }

    // 打开文件选择器以选择图片
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
            // 使用 Picasso 将选择的图片加载到 ImageView 中
            Picasso.get().load(imageUri).into(imageView);
            imageView.setTag(imageUri);
            // 存储图片 URI 到 SharedPreferences
            storeImageUri(imageUri);
        }
    }

    private void storeImageUri(Uri imageUri) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("imageUri", imageUri.toString());
        editor.apply();
    }

    // 将图片 URL 存储到 SharedPreferences
    private void storeImageUrl(String imageUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("imageUrl", imageUrl);
        editor.apply();
    }

    // 上传图片到Firebase Storage
    private void uploadImageToFirebase() {
        Uri imageUri = (Uri) imageView.getTag(); // 获取图片Uri
        if (imageUri == null) {
            // 如果没有选择图片，直接保存数据
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
                        Picasso.get().load(uri).into(imageView); // 修改：显示新上传的图片
                        storeImageUrl(imageUrl);
                        saveMenuItem(); // 上传图片完成后保存数据
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditMenuItemActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示确认对话框
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
                        uploadImageToFirebase(); // 尝试上传图片，如果没有选择图片也能新增或更新
                    } else if ("delete".equals(pendingAction)) {
                        deleteMenuItem();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // 保存或更新菜单项到Firebase
    private void saveMenuItem() {
        String chinese = chineseEditText.getText().toString();
        float price = Float.parseFloat(numberEditText.getText().toString());
        float cal = Float.parseFloat(calEditText.getText().toString());
        float protein = Float.parseFloat(proteinEditText.getText().toString());
        float sugar = Float.parseFloat(sugarEditText.getText().toString());
        float total_crab = Float.parseFloat(totalCrabEditText.getText().toString());
        float fat = Float.parseFloat(fatEditText.getText().toString());
        String name = chinese;
        String desprice = "價格";
        String descal = "熱量";
        String desprotein = "蛋白質";
        String dessugar = "糖";
        String destoatl_crab = "碳水化合物";
        String desfat = "脂肪";
        String photo = "照片";
        String plus = "加料";

        DatabaseReference itemRef = menuRef.child(itemName).child(name);
        itemRef.child(desprice).setValue(price);
        itemRef.child(descal).setValue(cal);
        itemRef.child(desprotein).setValue(protein);
        itemRef.child(dessugar).setValue(sugar);
        itemRef.child(destoatl_crab).setValue(total_crab);
        itemRef.child(desfat).setValue(fat);

        // 确保图片URL被保存
        if (imageUrl != null && !imageUrl.isEmpty()) {
            itemRef.child(photo).setValue(imageUrl);
        }

        // 檢查並儲存勾選的 CheckBox 的文字到 Firebase
        if (p_rice.isChecked()) {
            itemRef.child(plus).child("飯").setValue(10); // 儲存"米飯"選項
        }
        if (p_noodles.isChecked()) {
            itemRef.child(plus).child("麵").setValue(10); // 儲存"麵條"選項
        }
        if (p_cheese.isChecked()) {
            itemRef.child(plus).child("起司").setValue(5); // 儲存"起司"選項
        }
        if (p_shacha.isChecked()) {
            itemRef.child(plus).child("沙茶").setValue(0);
        }

        // 保存CheckBox状态到SharedPreferences
        saveCheckBoxStates();

        Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 删除菜单项
    private void deleteMenuItem() {
        String chinese = chineseEditText.getText().toString();
        String name = chinese;
        menuRef.child(itemName).child(name).removeValue();
        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    // 从Firebase加载照片并显示在ImageView中
    private void loadPhotoFromFirebase() {
        String chinese = chineseEditText.getText().toString();
        String name = chinese;
        menuRef.child(itemName).child(name).child("照片").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrl = snapshot.getValue(String.class); // 获取图片URL
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get().load(imageUrl).into(imageView); // 显示图片
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditMenuItemActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 保存CheckBox状态到SharedPreferences
    private void saveCheckBoxStates() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("p_rice", p_rice.isChecked());
        editor.putBoolean("p_noodles", p_noodles.isChecked());
        editor.putBoolean("p_cheese", p_cheese.isChecked());
        editor.putBoolean("p_shacha", p_shacha.isChecked());
        editor.apply();
    }

    // 加载CheckBox状态
    private void loadCheckBoxStates() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        p_rice.setChecked(sharedPreferences.getBoolean("p_rice", false));
        p_noodles.setChecked(sharedPreferences.getBoolean("p_noodles", false));
        p_cheese.setChecked(sharedPreferences.getBoolean("p_cheese", false));
        p_shacha.setChecked(sharedPreferences.getBoolean("p_shacha", false));
    }
}
