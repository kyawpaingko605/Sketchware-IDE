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
        // Toolbar ပေါ်တွင် Live Run ခလုတ်အား Icon နှင့်တကွ သေချာစွာထည့်သွင်းခြင်း
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
     * Compiled classes.dex အားဖတ်ပြီး PreviewRunnerActivity ထဲတွင် အလုပ်လုပ်စေမည့်စနစ်
     */
    private void startInAppLivePreview() {
        if (sc_id == null) {
            Toast.makeText(this, "Project ID မရှိပါ။", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sketchware Pro ရဲ့ Internal storage bin folder အောက်က classes.dex ကို လှမ်းရှာခြင်း
        String dexPath = getFilesDir().getParent() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex";
        File dexFile = new File(dexPath);
        
        // Internal Storage ထဲမှာမတွေ့ပါက External Storage တွင် ထပ်မံရှာဖွေခြင်း
        if (!dexFile.exists()) {
            dexPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/.sketchware/mysc/" + sc_id + "/bin/classes.dex";
            dexFile = new File(dexPath);
        }

        if (!dexFile.exists()) {
            Toast.makeText(this, "DEX ဖိုင် ရှာမတွေ့ပါ။ ပရောဂျက်ကို အပြင်မျက်နှာပြင်မှာ အရင်ဆုံး 'Run/Compile' ပတ်ခဲ့ပေးပါရန် လိုအပ်ပါတယ်။", Toast.LENGTH_LONG).show();
            return;
        }

        // Package Name ကို ရှာဖွေခြင်းစနစ် (v7 metadata အမှန်ကို သုံးထားပါသည်)
        String packageName = "com.my.newproject"; 
        try {
            // Sketchware Pro v7 ရဲ့ jC.c(sc_id).c() က ပရောဂျက်ရဲ့ အခြေခံအချက်အလက် (ProjectInfoBean) ကိုပေးပါတယ်
            if (jC.c(sc_id) != null && jC.c(sc_id).c() != null) {
                packageName = jC.c(sc_id).c().myPkgName;
            }
        } catch (Exception e) {
            // ဒုတိယ အရန်စနစ်ဖြင့် ထပ်မံကြိုးစားခြင်း
            try {
                if (jC.b(sc_id) != null) {
                    packageName = jC.b(sc_id).myPkgName;
                }
            } catch (Exception ignored) {}
        }

        Toast.makeText(this, "Live Preview ကို စတင်နေပါပြီ...", Toast.LENGTH_SHORT).show();
        
        // Preview Runner အား အချက်အလက်များ လွှဲပြောင်းပေးပြီး ပွင့်စေခြင်း
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
