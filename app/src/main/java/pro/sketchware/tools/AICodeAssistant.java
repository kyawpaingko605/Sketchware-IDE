package pro.sketchware.tools;

import android.util.Log;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class AICodeAssistant {

    // 💡 မှတ်ချက် - YOUR_GEMINI_API_KEY နေရာတွင် လူကြီးမင်း၏ Google AI Studio မှ API Key အစစ်ကို ထည့်သွင်းပေးပါ
    private static final String API_URL = "https://googleapis.com";

    public interface AICallback {
        void onSuccess(String aiCode);
        void onFailure(String error);
    }

    public static void askAIForCode(String userPrompt, final AICallback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONObject textObject = new JSONObject();

            // AI အား ကုဒ်သီးသန့်သာ စနစ်တကျ ထုတ်ပေးရန် စည်းကမ်းချက် (System Prompt) သတ်မှတ်ခြင်း
            String systemInstruction = "You are an expert Android Developer helper inside Sketchware Pro IDE. "
                    + "Generate ONLY pure Android Java or XML source code based on this request: " + userPrompt + ". "
                    + "DO NOT include any Markdown tags like ```java or ```xml. DO NOT include any conversational text or explanations. "
                    + "Provide raw executable code string only.";

            textObject.put("text", systemInstruction);
            partsObject.put("parts", new JSONArray().put(textObject));
            contentsArray.put(partsObject);
            jsonBody.put("contents", contentsArray);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            
                            String aiText = jsonResponse.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                                    
                            // အကယ်၍ AI မှ ``` စာတန်းများ မတော်တဆ ထည့်ပေးခဲ့ပါက ဖယ်ထုတ်ရှင်းလင်းခြင်း
                            aiText = aiText.replaceAll("```java", "")
                                           .replaceAll("```xml", "")
                                           .replaceAll("```", "")
                                           .trim();

                            callback.onSuccess(aiText);
                        } catch (Exception e) {
                            callback.onFailure("JSON Parsing Error: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Server Response Error Code: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }
}
