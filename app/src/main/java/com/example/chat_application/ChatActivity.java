package com.example.chat_application;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {


    EditText messageEditText;
    ListView chatListView;
    ChatMessageAdapter adapter;

    ArrayList<ChatMessage> chatHistory;
    ImageButton send_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatListView = findViewById(R.id.chatListView);
        send_btn = findViewById(R.id.send_btn);
        messageEditText = findViewById(R.id.message_text);

        String username = getIntent().getStringExtra("username");

        chatHistory = new ArrayList<>();
        adapter = new ChatMessageAdapter(this, chatHistory);
        chatListView.setAdapter(adapter);

        addMessage("Hey " + username + "!, Welcome", false );
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = messageEditText.getText().toString();
                if(!userMessage.isEmpty()){
                    addMessage(userMessage, true);
                    sendChatMessage(userMessage);
                    messageEditText.setText(" ");

                }
            }
        });

    }

    private void addMessage(String message, boolean isUser){
        chatHistory.add(new ChatMessage(message,isUser));
        adapter.notifyDataSetChanged();
        chatListView.setSelection(chatHistory.size() - 1);
    }

    private void sendChatMessage(String userMessage){

        String baseurl = "http://10.0.2.2:5000/chat";

        JSONObject chatobject = new JSONObject();
        try{

            chatobject.put("userMessage",userMessage);

            JSONArray chatHistoryJson = new JSONArray();

            for(ChatMessage message: chatHistory){
                JSONObject messageJson = new JSONObject();
                if(message.isUser()){
                    messageJson.put("User", message.getMessage());
                    messageJson.put("Llama", "");
                }else{
                    messageJson.put("Llama", message.getMessage());
                    messageJson.put("User", "");
                }
                chatHistoryJson.put(messageJson);
            }

            chatobject.put("chatHistory", chatHistoryJson);
            Log.d("ChatActivity", "Sending JSON: " + chatobject.toString());


        }catch (JSONException e){
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, baseurl, chatobject,
                response -> {
                    try {
                        String btmsg = response.getString("message");
                        addMessage(btmsg,false);
                    }catch (JSONException e){
                        e.printStackTrace();
                        addMessage("Failed to get responcse", false);
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("ChatActivity", "Error: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("ChatActivity", "Status Code: " + error.networkResponse.statusCode);
                        Log.e("ChatActivity", "Response Data: " + new String(error.networkResponse.data));
                        addMessage("Error: " + new String(error.networkResponse.data), false);
                    }else {
                        addMessage("Error receiving repsonse from server.", false);
                    }
                }
        ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError{
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                2000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                Volley.newRequestQueue(this).add(jsonObjectRequest);

    }
}