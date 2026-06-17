package pro.sketchware.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.besome.sketch.beans.ViewBean;
import pro.sketchware.tools.ViewBeanParser;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

public class PreviewRunnerActivity extends Activity {

    private static final String EXTRA_DEX_PATH = "dex_path";
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_XML_CONTENT = "xml_content";
    
    private FrameLayout previewContainer;
    private DexClassLoader classLoader;
    private Object targetActivityLogicInstance;
    private Class<?> targetActivityClass;

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

        previewContainer = new FrameLayout(this);
        previewContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(previewContainer);

        String dexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        String xmlContent = getIntent().getStringExtra(EXTRA_XML_CONTENT);

        if (dexPath == null || !new File(dexPath).exists()) {
            showError("DEX ဖိုင် ရှာမတွေ့ပါ။ ပရောဂျက်ကို အရင်ဆုံး Compile လုပ်ပေးပါ။");
            return;
        }

        // ၁။ နောက်ကွယ်က ကုဒ် (DEX) ကို အရင် Load လုပ်ခြင်း
        loadTargetDexLogic(dexPath, packageName);

        // ၂။ UI ကို စမ်းသပ်ဆွဲတင်ခြင်း
        if (xmlContent != null && !xmlContent.isEmpty()) {
            renderTargetUi(xmlContent);
        } else {
            showError("UI Layout XML ဒေတာ မပါလာပါ။");
        }
    }

    /**
     * Target App ၏ Logic ကုဒ်များကို ကနဦး သတ်မှတ်သိမ်းဆည်းခြင်း
     */
    private void loadTargetDexLogic(String dexPath, String packageName) {
        try {
            File optimizedDexOutputDir = getDir("dex_cache", Context.MODE_PRIVATE);
            classLoader = new DexClassLoader(
                    dexPath,
                    optimizedDexOutputDir.getAbsolutePath(),
                    null,
                    getClassLoader()
            );

            String mainActivityClassName = packageName + ".MainActivity";
            try {
                targetActivityClass = classLoader.loadClass(mainActivityClassName);
                targetActivityLogicInstance = targetActivityClass.getConstructor().newInstance();
            } catch (Exception e) {
                Log.e("PreviewRunner", "MainActivity Logic class ကို ရှာမတွေ့ပါ သို့မဟုတ် မဆောက်နိုင်ပါ");
            }

        } catch (Exception e) {
            Log.e("PreviewRunner", "DEX Loading Error: " + e.getMessage());
        }
    }

    /**
     * ViewBeanParser ကို သုံးပြီး Install လုပ်စရာမလိုဘဲ UI ကို ချက်ချင်း ဖော်ပြခြင်း
     */
    private void renderTargetUi(String xmlContent) {
        try {
            ViewBeanParser parser = new ViewBeanParser(xmlContent);
            ArrayList<ViewBean> viewBeans = parser.parse();

            if (viewBeans == null || viewBeans.isEmpty()) {
                showError("Layout ထဲတွင် မည်သည့် UI ပစ္စည်းမှ မရှိပါ။");
                return;
            }

            LinearLayout rootLayout = new LinearLayout(this);
            rootLayout.setOrientation(LinearLayout.VERTICAL);
            rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));

            // View များကို ပတ်လမ်းကြောင်း Loop ပတ်ခြင်း
            for (ViewBean bean : viewBeans) {
                View elementView = createRuntimeViewFromBean(bean);
                if (elementView != null) {
                    rootLayout.addView(elementView);
                }
            }

            previewContainer.removeAllViews();
            previewContainer.addView(rootLayout);
            Toast.makeText(this, "Live UI Preview အလုပ်လုပ်နေပါပြီ", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            showError("UI ဆွဲတင်ရာတွင် အမှားအယွင်း ရှိနေပါသည်:\n" + e.getMessage());
        }
    }

    /**
     * ViewBean မှ တစ်ဆင့် တကယ့် Android View (Button, TextView) များကို ဖန်တီးပေးခြင်း
     */
    private View createRuntimeViewFromBean(ViewBean bean) {
        try {
            View view;
            
            // ပြင်ဆင်ချက် - TextBean Error ကျော်လွှားရန် စာသား (String) အဖြစ် စနစ်တကျ ပြောင်းလဲခြင်း
            String elementText = "";
            if (bean.text != null) {
                elementText = bean.text.toString(); 
            } else {
                elementText = bean.id; // စာသားမရှိပါက View ID ကိုပဲ ခေတ္တပြပေးရန်
            }

            // Bean Type အလိုက် ခွဲခြားခြင်း
            if (bean.type == 1) { // Button
                android.widget.Button btn = new android.widget.Button(this);
                btn.setText(elementText); 
                view = btn;
            } else if (bean.type == 2) { // TextView
                TextView tv = new TextView(this);
                tv.setText(elementText);  
                
                // ပြင်ဆင်ချက် - textSize variable အမှားအတွက် စိတ်ချရသော default တန်ဖိုးပေးခြင်း
                tv.setTextSize(16); 
                view = tv;
            } else {
                // အခြား View များအတွက်
                TextView genericView = new TextView(this);
                genericView.setText("[" + bean.id + "]");
                genericView.setPadding(16, 16, 16, 16);
                view = genericView;
            }

            // ခလုတ်နှိပ်သည့် Logic (onClick) ကို တိုက်ရိုက်ချိတ်ပေးခြင်း
            view.setOnClickListener(v -> {
                triggerDexMethod(bean.id + "_onClick", v);
            });

            return view;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * နှိပ်လိုက်သည့် ခလုတ်၏ ကုဒ် (Logic) များကို classes.dex ထဲမှ ရှာဖွေပြီး လှမ်းမောင်းပေးခြင်း
     */
    private void triggerDexMethod(String methodName, View view) {
        if (targetActivityClass == null || targetActivityLogicInstance == null) return;
        try {
            Method targetMethod = targetActivityClass.getDeclaredMethod(methodName, View.class);
            targetMethod.setAccessible(true);
            targetMethod.invoke(targetActivityLogicInstance, view);
        } catch (NoSuchMethodException e) {
            Toast.makeText(this, "Event '" + methodName + "' အတွက် ကုဒ် မရေးရသေးပါ။", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("PreviewRunner", "Method Invoke Error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(Color.RED);
        textView.setTextSize(16);
        
        layout.addView(textView);
        scrollView.addView(layout);
        previewContainer.removeAllViews();
        previewContainer.addView(scrollView);
    }
}
