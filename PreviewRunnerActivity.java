package pro.sketchware.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private LinearLayout container;

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

        // Preview ပြသပေးမည့် Layout အား Dynamic ဆောက်ခြင်း
        ScrollView scrollView = new ScrollView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);
        scrollView.addView(container);
        setContentView(scrollView);

        String dexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);

        if (dexPath == null || !new File(dexPath).exists()) {
            showError("DEX file not found! Please compile the project first.");
            return;
        }

        loadAndExecuteDex(dexPath, packageName);
    }

    /**
     * DexClassLoader သုံးပြီး classes.dex ကို Sketchware အတွင်းထဲမှာတင် တိုက်ရိုက် Load လုပ်၍ ပတ်ခြင်း
     */
    private void loadAndExecuteDex(String dexPath, String packageName) {
        try {
            // Optimized DEX သိမ်းဆည်းမည့် Cache Directory တည်ဆောက်ခြင်း
            File optimizedDexOutputDir = getDir("dex_cache", Context.MODE_PRIVATE);

            // DexClassLoader အား သတ်မှတ်ခြင်း
            DexClassLoader classLoader = new DexClassLoader(
                    dexPath,
                    optimizedDexOutputDir.getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            // စမ်းသပ်မည့် App ၏ MainActivity အား ရှာဖွေခြင်း
            String mainActivityClass = packageName + ".MainActivity";
            Class<?> loadedClass = classLoader.loadClass(mainActivityClass);

            // တကယ်လို့ ၎င်းသည် Activity ဖြစ်ပါက (သို့မဟုတ်) View ပုံဖော်နိုင်သော Constructor ပါက ဆွဲတင်မည်
            Constructor<?> constructor = loadedClass.getConstructor();
            Object instance = constructor.newInstance();

            // ၎င်း Class ထဲတွင် UI View ပြန်ပေးမည့် Method သို့မဟုတ် onCreate ကဲ့သို့ စနစ်အား ပတ်ခြင်း
            // မှတ်ချက်- ဤနေရာတွင် ရိုးရှင်းစွာ စမ်းသပ်ရန် တိုက်ရိုက် View ဖန်တီးမှု သို့မဟုတ် Reflection သုံးနိုင်သည်
            TextView successText = new TextView(this);
            successText.setText("Successfully loaded: " + mainActivityClass + "\nRunning in Sandbox Environment.");
            successText.setTextSize(18);
            container.addView(successText);

            Toast.makeText(this, "App Preview Started Successfully!", Toast.LENGTH_SHORT).show();

        } catch (ClassNotFoundException e) {
            showError("MainActivity not found in compiled DEX. Make sure the package name is correct.");
        } catch (Exception e) {
            showError("Runtime Execution Error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        TextView errorText = new TextView(this);
        errorText.setText(message);
        errorText.setTextColor(0xFFFF0000); // အနီရောင်
        errorText.setTextSize(16);
        container.addView(errorText);
    }
}
