package pro.sketchware.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import pro.sketchware.utility.SketchwareUtil;

public class PreviewRunnerActivity extends Activity {

    private static final String EXTRA_DEX_PATH = "dex_path";
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    private LinearLayout rootContainer;

    /**
     * ProjectBuilder ကနေ Preview Activity ကို လှမ်းပွင့်ခိုင်းရန်သုံးသည့် Method
     */
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

        // အခြေခံ Main Container အား တည်ဆောက်ခြင်း
        rootContainer = new LinearLayout(this);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(rootContainer);

        String dexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);

        if (dexPath == null || !new File(dexPath).exists()) {
            showError("DEX file not found! Please compile the project first.");
            return;
        }

        loadAndExecuteDex(dexPath, packageName, savedInstanceState);
    }

    /**
     * DexClassLoader အား အပြည့်အဝသုံး၍ Target MainActivity ရဲ့ UI Lifecycle တစ်ခုလုံးအား ဆွဲတင်ပတ်ခြင်း
     */
    private void loadAndExecuteDex(String dexPath, String packageName, Bundle savedInstanceState) {
        try {
            // Optimized DEX သိမ်းဆည်းမည့် Cache Directory တည်ဆောက်ခြင်း
            File optimizedDexOutputDir = getDir("dex_cache", Context.MODE_PRIVATE);

            // DexClassLoader စနစ်ဖြင့် classes.dex အား လှမ်းဖတ်ခြင်း
            DexClassLoader classLoader = new DexClassLoader(
                    dexPath,
                    optimizedDexOutputDir.getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            // စမ်းသပ်မည့် App ၏ MainActivity Class ကို ခေါ်ယူခြင်း
            String mainActivityClass = packageName + ".MainActivity";
            Class<?> loadedClass = classLoader.loadClass(mainActivityClass);

            // Target Activity ၏ Instance (အရာဝတ္ထု) ကို Reflection ဖြင့် တည်ဆောက်ခြင်း
            Constructor<?> constructor = loadedClass.getConstructor();
            final Object targetActivityInstance = constructor.newInstance();

            // ၁။ Target Activity ထဲသို့ လက်ရှိ Context / Base Context အား လွှဲပြောင်းပေးခြင်း (မဖြစ်မနေလိုအပ်)
            try {
                Method attachBaseContextMethod = Activity.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContextMethod.setAccessible(true);
                attachBaseContextMethod.invoke(targetActivityInstance, this);
            } catch (Exception ignored) {
                // အကယ်၍ အဆင်မပြေပါက အခြား Method ဖြင့် Context ထည့်သွင်းခြင်း
                try {
                    Method setParentMethod = Activity.class.getDeclaredMethod("setParent", Activity.class);
                    setParentMethod.setAccessible(true);
                    setParentMethod.invoke(targetActivityInstance, this);
                } catch (Exception e2) {
                    Log.e("PreviewRunner", "Context injection failed: " + e2.getMessage());
                }
            }

            // ၂။ Target Activity ရဲ့ onCreate(Bundle) Lifecycle အား တိုက်ရိုက် ခေါ်ယူအလုပ်လုပ်စေခြင်း
            Method onCreateMethod = loadedClass.getDeclaredMethod("onCreate", Bundle.class);
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(targetActivityInstance, savedInstanceState);

            // ၃။ Target App က ဖန်တီးလိုက်တဲ့ အဓိက UI View (Window Content) ကို လှမ်းယူခြင်း
            try {
                Method getWindowMethod = Activity.class.getMethod("getWindow");
                Object window = getWindowMethod.invoke(targetActivityInstance);
                if (window != null) {
                    Method getDecorViewMethod = window.getClass().getMethod("getDecorView");
                    View decorView = (View) getDecorViewMethod.invoke(window);
                    
                    if (decorView != null) {
                        View contentView = decorView.findViewById(android.R.id.content);
                        if (contentView instanceof ViewGroup && ((ViewGroup) contentView).getChildCount() > 0) {
                            View realAppUi = ((ViewGroup) contentView).getChildAt(0);
                            ((ViewGroup) contentView).removeView(realAppUi);
                            rootContainer.addView(realAppUi);
                        } else {
                            rootContainer.addView(decorView);
                        }
                    }
                }
            } catch (Exception e) {
                TextView successText = new TextView(this);
                successText.setText("Successfully initialized " + mainActivityClass + " lifecycle.\n(UI Rendering falls back to basic mode)");
                successText.setTextSize(16);
                rootContainer.addView(successText);
            }

            Toast.makeText(this, "In-App Run Preview Active!", Toast.LENGTH_SHORT).show();

        } catch (ClassNotFoundException e) {
            showError("MainActivity not found in compiled DEX. Make sure the package name is correct.");
        } catch (Exception e) {
            showError("Runtime Sandbox Error: " + e.getMessage() + "\n\nTip: If your app uses custom resources/themes, it's recommended to build a full APK.");
        }
    }

    private void showError(String message) {
        ScrollView errorScroll = new ScrollView(this);
        LinearLayout errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setPadding(32, 32, 32, 32);

        TextView errorTitle = new TextView(this);
        errorTitle.setText("Preview Compilation Failed");
        errorTitle.setTextColor(0xFFFF0000);
        errorTitle.setTextSize(20);
        errorTitle.setPadding(0, 0, 0, 16);
        errorLayout.addView(errorTitle);

        TextView errorText = new TextView(this);
        errorText.setText(message);
        errorText.setTextColor(0xFF333333);
        errorText.setTextSize(14);
        errorLayout.addView(errorText);

        errorScroll.addView(errorLayout);
        rootContainer.addView(errorScroll);
    }
}
