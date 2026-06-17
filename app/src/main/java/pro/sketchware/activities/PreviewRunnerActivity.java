package pro.sketchware.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class PreviewRunnerActivity extends Activity {

    private static final String EXTRA_DEX_PATH = "dex_path";
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private FrameLayout previewContainer;

    public static void startPreview(Context context, String dexPath, String packageName) {
        Intent intent = new Intent(context, PreviewRunnerActivity.class);
        intent.putExtra(EXTRA_DEX_PATH, dexPath);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Preview ပြသမည့် ပင်မ Container အား ဖန်တီးခြင်း
        previewContainer = new FrameLayout(this);
        previewContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(previewContainer);

        String dexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);

        if (dexPath == null || !new File(dexPath).exists()) {
            showError("DEX ဖိုင် ရှာမတွေ့ပါ။ ပရောဂျက်ကို အရင်ဆုံး Compile လုပ်ပေးပါ။");
            return;
        }

        executeTargetDex(dexPath, packageName);
    }

    private void executeTargetDex(String dexPath, String packageName) {
        try {
            File optimizedDexOutputDir = getDir("dex_cache", Context.MODE_PRIVATE);
            DexClassLoader classLoader = new DexClassLoader(
                    dexPath,
                    optimizedDexOutputDir.getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            // Target App ရဲ့ MainActivity ကို ခေါ်ယူခြင်း
            String mainActivityClass = packageName + ".MainActivity";
            Class<?> loadedClass = classLoader.loadClass(mainActivityClass);
            Constructor<?> constructor = loadedClass.getConstructor();
            Object activityInstance = constructor.newInstance();

            // အခြေခံ Context လွှဲပြောင်းပေးခြင်း
            try {
                Method attachBaseContext = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(activityInstance, this);
            } catch (Exception ignored) {}

            // Target App ရဲ့ onCreate ကို အလုပ်လုပ်စေခြင်း
            Method onCreateMethod = loadedClass.getDeclaredMethod("onCreate", Bundle.class);
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(activityInstance, new Bundle());

            // ⚠️ အဓိကအဆင့်- Target Activity ရဲ့ Window ထဲက Content View အစစ်ကို ဆွဲထုတ်ခြင်း
            Method getWindowMethod = Activity.class.getMethod("getWindow");
            Object window = getWindowMethod.invoke(activityInstance);
            if (window != null) {
                Method getDecorViewMethod = window.getClass().getMethod("getDecorView");
                View decorView = (View) getDecorViewMethod.invoke(window);
                
                if (decorView != null) {
                    View contentView = decorView.findViewById(android.R.id.content);
                    if (contentView instanceof ViewGroup && ((ViewGroup) contentView).getChildCount() > 0) {
                        View targetUi = ((ViewGroup) contentView).getChildAt(0);
                        ((ViewGroup) contentView).removeView(targetUi); // မူလနေရာမှ ဖြုတ်ချခြင်း
                        previewContainer.addView(targetUi); // Preview ထဲသို့ ထည့်သွင်းခြင်း
                        Toast.makeText(this, "Preview ပွင့်လာပါပြီ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            showError("DEX ကို Load လုပ်နိုင်သော်လည်း UI View အား ဆွဲတင်၍မရနိုင်ပါ။");

        } catch (Exception e) {
            showError("Run Preview စနစ် အလုပ်လုပ်ရာတွင် အမှားအယွင်းရှိနေပါသည်:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(0xFFFF0000);
        textView.setTextSize(16);
        
        layout.addView(textView);
        scrollView.addView(layout);
        previewContainer.removeAllViews();
        previewContainer.addView(scrollView);
    }
}
