package pro.sketchware.activities.preview;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.besome.sketch.editor.view.ViewPane;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.io.File;
import java.util.ArrayList;

import a.a.a.jC;
import a.a.a.mB;
import pro.sketchware.activities.PreviewRunnerActivity;
import pro.sketchware.databinding.ActivityLayoutPreviewBinding;
import pro.sketchware.tools.ViewBeanParser;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class LayoutPreviewActivity extends BaseAppCompatActivity {

    private ViewPane pane;
    private String content;
    private String sc_id;
    private String title;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        ActivityLayoutPreviewBinding binding = ActivityLayoutPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        var toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        
        title = getIntent().getStringExtra("title");
        sc_id = getIntent().getStringExtra("sc_id");
        content = getIntent().getStringExtra("xml");
        
        getSupportActionBar().setTitle("Layout Preview");
        getSupportActionBar().setSubtitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        
        toolbar.setNavigationOnClickListener(v -> {
            if (!mB.a()) {
                onBackPressed();
            }
        });
        
        pane = binding.pane;
        pane.initialize(sc_id, true);
        pane.updateRootLayout(sc_id, title);
        pane.setVerticalScrollBarEnabled(true);
        pane.setResourceManager(jC.d(sc_id));
        UI.addSystemWindowInsetToPadding(binding.pane, false, false, false, true);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (content != null) {
            try {
                var parser = new ViewBeanParser(content);
                loadViews(parser.parse());
            } catch (Exception e) {
                SketchwareUtil.toastError(e.toString());
            }
        } else {
            SketchwareUtil.toastError("content is null");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Toolbar ညာဘက်အပေါ်ထောင့်တွင် Live Run စမ်းသပ်ရန် ခလုတ်အသစ်ထည့်သွင်းခြင်း
        MenuItem livePreviewItem = menu.add(Menu.NONE, 888, Menu.NONE, "Live Run");
        livePreviewItem.setIcon(android.R.drawable.ic_media_play);
        livePreviewItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 888) {
            if (!mB.a()) {
                startInAppLivePreview();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Compiled ဖြစ်ပြီးသား dex ဖိုင်ကိုဖတ်ပြီး Sandbox Preview စနစ်သို့ လွှဲပြောင်းပေးခြင်း
     */
    private void startInAppLivePreview() {
        if (sc_id == null) {
            Toast.makeText(this, "Project ID မရှိပါ။", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sketchware ရဲ့ Project Bin လမ်းကြောင်းထဲမှ classes.dex ကို ရှာဖွေခြင်း
        String dexPath = getFilesDir().getParent() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex";
        File dexFile = new File(dexPath);
        
        // အကယ်၍ အပေါ်က လမ်းကြောင်းမတွေ့ပါက ဒုတိယ အရန်လမ်းကြောင်းဖြင့် ထပ်ရှာခြင်း
        if (!dexFile.exists()) {
            dexPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex";
            dexFile = new File(dexPath);
        }

        if (!dexFile.exists()) {
            Toast.makeText(this, "DEX ဖိုင် ရှာမတွေ့ပါ။ ပရောဂျက်ကို အပြင်မှာ အရင်ဆုံး Run/Compile လုပ်ခဲ့ပေးရန် လိုအပ်ပါတယ်။", Toast.LENGTH_LONG).show();
            return;
        }

        // Package Name အား ယူခြင်း (Compilation Error လုံးဝကင်းဝေးစေရန် Dynamic Structure ဖြင့် ပြင်ဆင်ထားပါသည်)
        String packageName = "com.my.newproject"; 
        try {
            if (getIntent().hasExtra("package_name") && getIntent().getStringExtra("package_name") != null) {
                packageName = getIntent().getStringExtra("package_name");
            } else if (jC.a(sc_id) != null) {
                // Obfuscation နှင့် Version ကွဲလွဲမှုများကြောင့် Error မတက်စေရန် Object Type မှတစ်ဆင့် စစ်ဆေးယူခြင်း
                Object projectInfo = jC.a(sc_id);
                java.lang.reflect.Field[] fields = projectInfo.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    if (field.getName().equals("myPkgName") || field.getName().equals("b")) {
                        Object val = field.get(projectInfo);
                        if (val instanceof String && ((String) val).contains(".")) {
                            packageName = (String) val;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // လုံးဝမရပါက Default Safe Package Name ကို အသုံးပြုမည်
            packageName = "com.my.project" + sc_id;
        }

        if (packageName == null || packageName.isEmpty() || packageName.equals("com.my.newproject")) {
            packageName = "com.my.project" + sc_id;
        }

        Toast.makeText(this, "Live Preview ကို စတင်နေပါပြီ...", Toast.LENGTH_SHORT).show();
        PreviewRunnerActivity.startPreview(this, dexPath, packageName);
    }

    private ItemView loadView(ViewBean view) {
        var itemView = pane.createItemView(view);
        pane.addViewAndUpdateIndex(itemView);
        if (itemView instanceof ItemView sy) {
            sy.setFixed(true);
            return sy;
        }
        return null;
    }

    private ItemView loadViews(ArrayList<ViewBean> views) {
        ItemView itemView = null;
        for (ViewBean view : views) {
            if (views.indexOf(view) == 0) {
                view.parent = "root";
                view.parentType = 0;
                view.preParent = null;
                view.preParentType = -1;
                itemView = loadView(view);
            } else {
                loadView(view);
            }
        }
        return itemView;
    }
}
