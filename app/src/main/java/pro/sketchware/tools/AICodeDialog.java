package pro.sketchware.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AICodeDialog {

    private final Context context;
    private BottomSheetDialog bottomSheetDialog;
    
    private EditText inputPrompt;
    private Button btnAskAI;
    private Button btnCopyCode;
    private TextView txtResultCode;
    private ProgressBar progressBar;
    private LinearLayout resultContainer;

    public AICodeDialog(Context context) {
        this.context = context;
        setupDialog();
    }

    private void setupDialog() {
        bottomSheetDialog = new BottomSheetDialog(context);
        
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(45, 55, 45, 55);
        mainLayout.setBackgroundColor(Color.parseColor("#121212")); // Ultra Dark Background

        // ခေါင်းစဉ် (Title)
        TextView title = new TextView(context);
        title.setText("✨ Sketchware AI Assistant");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 35);
        mainLayout.addView(title);

        // စာရိုက်ရန် အကွက် (Fancy Input)
        inputPrompt = new EditText(context);
        inputPrompt.setHint("ဥပမာ - ဘလူးတုသ်ဖွင့်မည့် ကုဒ်ရေးပေးပါ...");
        inputPrompt.setHintTextColor(Color.parseColor("#757575"));
        inputPrompt.setTextColor(Color.WHITE);
        inputPrompt.setTextSize(15);
        inputPrompt.setPadding(30, 30, 30, 30);
        
        GradientDrawable editBg = new GradientDrawable();
        editBg.setColor(Color.parseColor("#1E1E1E"));
        editBg.setCornerRadius(20);
        editBg.setStroke(2, Color.parseColor("#333333"));
        inputPrompt.setBackground(editBg);
        mainLayout.addView(inputPrompt);

        // အလျားလိုက် တိုးတက်မှုပြဘား (Loading Bar)
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        progressBar.setPadding(0, 20, 0, 20);
        mainLayout.addView(progressBar);

        // AI ခေါ်ရန် ခလုတ် (Material Accent Button)
        btnAskAI = new Button(context);
        btnAskAI.setText("AI ထံမှ ကုဒ်တောင်းမည်");
        btnAskAI.setTextColor(Color.WHITE);
        btnAskAI.setAllCaps(false);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#2196F3")); // Indigo Accent Blue
        btnBg.setCornerRadius(25);
        btnAskAI.setBackground(btnBg);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 30, 0, 30);
        btnAskAI.setLayoutParams(btnParams);
        mainLayout.addView(btnAskAI);

        // ရလဒ်ပြသရန် နေရာ (Result Box)
        resultContainer = new LinearLayout(context);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        resultContainer.setVisibility(View.GONE);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 500); // UI စာမျက်နှာ အမြင့်အား 500dp သတ်မှတ်
        scrollView.setLayoutParams(scrollParams);

        txtResultCode = new TextView(context);
        txtResultCode.setTextColor(Color.parseColor("#A9B7C6")); // Android Studio Log Color
        txtResultCode.setTextSize(14);
        txtResultCode.setPadding(25, 25, 25, 25);
        
        GradientDrawable codeBg = new GradientDrawable();
        codeBg.setColor(Color.parseColor("#2B2B2B")); // Code Block Background
        codeBg.setCornerRadius(15);
        txtResultCode.setBackground(codeBg);
        scrollView.addView(txtResultCode);
        resultContainer.addView(scrollView);

        // ကုဒ် ကူးယူရန်ခလုတ် (Copy Button)
        btnCopyCode = new Button(context);
        btnCopyCode.setText("ကုဒ်အား ကူးယူမည် (Copy)");
        btnCopyCode.setTextColor(Color.WHITE);
        btnCopyCode.setAllCaps(false);
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setColor(Color.parseColor("#4CAF50")); // Premium Green
        copyBg.setCornerRadius(25);
        btnCopyCode.setBackground(copyBg);
        
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copyParams.setMargins(0, 20, 0, 0);
        btnCopyCode.setLayoutParams(copyParams);
        resultContainer.addView(btnCopyCode);

        mainLayout.addView(resultContainer);
        bottomSheetDialog.setContentView(mainLayout);

        btnAskAI.setOnClickListener(v -> startAiProcess());
    }

    private void startAiProcess() {
        String prompt = inputPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(context, "မေးခွန်း အရင်ရိုက်ပါ...", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAskAI.setEnabled(false);
        btnAskAI.setText("AI စဉ်းစားနေပါသည်...");

        AICodeAssistant.askAIForCode(prompt, new AICodeAssistant.AICallback() {
            @Override
            public void onSuccess(final String aiCode) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnAskAI.setEnabled(true);
                        btnAskAI.setText("ထပ်မံ မေးမြန်းမည်");
                        
                        txtResultCode.setText(aiCode);
                        resultContainer.setVisibility(View.VISIBLE);

                        btnCopyCode.setOnClickListener(v -> {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("AI_CODE", aiCode);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, "ကုဒ်ကို Clipboard ထဲသို့ ကူးယူပြီးပါပြီ။", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }

            @Override
            public void onFailure(final String error) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnAskAI.setEnabled(true);
                        btnAskAI.setText("ထပ်မံ ကြိုးစားမည်");
                        Toast.makeText(context, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    public void show() {
        if (bottomSheetDialog != null) {
            bottomSheetDialog.show();
        }
    }
}
