package pro.sketchware.activities.preview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.besome.sketch.editor.view.ViewPane;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
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
        super.osOnPostCreate(savedInstanceState);
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
     * အဆင့်မြှင့်တင်ထားသော စမ်းသပ်မောင်းနှင်မှုစနစ် (Android 11 မှ 14 အထိ အလုပ်လုပ်နိုင်သည်)
     */
    private void startInAppLivePreview() {
        if (sc_id == null) {
            Toast.makeText(this, "Project ID မရှိပါ။", Toast.LENGTH_SHORT).show();
            return;
        }

        String dexPath = "";
        boolean found = false;

        String[] potentialPaths = new String[]{
            android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex",
            getExternalFilesDir(null) + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex",
            getFilesDir().getParent() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex",
            android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + getPackageName() + "/files/.sketchware/mysc/" + sc_id + "/bin/classes.dex",
            android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/sketchware/compiled/" + sc_id + "/classes.dex"
        };

        for (String path : potentialPaths) {
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    dexPath = path;
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            Toast.makeText(this, "DEX ဖိုင် ရှာမတွေ့သေးပါ။ ပရောဂျက်ကို အပြင် Editor တွင် သေချာစွာ တစ်ကြိမ် Run ခဲ့ပေးပါ။", Toast.LENGTH_LONG).show();
            return;
        }

        // Package Name အား ယူခြင်း
        String packageName = "com.my.newproject"; 
        try {
            if (getIntent().hasExtra("package_name") && getIntent().getStringExtra("package_name") != null) {
                packageName = getIntent().getStringExtra("package_name");
            } else if (jC.a(sc_id) != null) {
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
            packageName = "com.my.project" + sc_id;
        }

        if (packageName == null || packageName.isEmpty() || packageName.equals("com.my.newproject")) {
            packageName = "com.my.project" + sc_id;
        }

        // Android 11+ Scoped Storage ကန့်သတ်ချက်များကျော်လွှားရန် Private Cache ထဲသို့ DEX အား ကူးယူခြင်း
        try {
            File internalDexDir = getDir("preview_dex", Context.MODE_PRIVATE);
            File internalDexFile = new File(internalDexDir, "classes_" + sc_id + ".dex");
            
            File sourceFile = new File(dexPath);
            try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                 FileChannel destination = new FileOutputStream(internalDexFile).getChannel()) {
                destination.transferFrom(source, 0, source.size());
            }
            
            dexPath = internalDexFile.getAbsolutePath();
        } catch (Exception e) {
            // ကူးယူ၍မရပါက မူရင်း လမ်းကြောင်းအတိုင်း ဆက်သွားမည်
        }

        Toast.makeText(this, "Live Preview ကို စတင်နေပါပြီ...", Toast.LENGTH_SHORT).show();
        
        // 💡 [အဆင့်မြှင့်တင်ချက်] PreviewRunnerActivity သို့ ဒေတာများ သယ်ဆောင်သွားရန် Intent စနစ်အား တိုက်ရိုက်ခေါ်ယူခြင်း
        Intent intent = new Intent(this, PreviewRunnerActivity.class);
        intent.putExtra("dex_path", dexPath);
        intent.putExtra("package_name", packageName);
        intent.putExtra("xml_content", content); // UI XML အား သယ်ဆောင်သွားခြင်း
        intent.putExtra("title", title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
