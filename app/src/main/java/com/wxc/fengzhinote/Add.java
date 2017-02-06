package com.wxc.fengzhinote;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wxc on 2017/2/6.
 */

public class Add extends AppCompatActivity {
    private LinearLayout add;            //布局容器
    private EditText noteTxt;            //内容框
    private EditText titleTxt;            //标题框
    private ImageButton backBtn;        //返回
    private ImageButton lockBtn;        //加锁
    private ImageButton goneBtn;        //放飞
    private ImageButton saveBtn;        //保存
    private TextView lengthTxt;            //长度标签
    private Button clearBtn;            //清空标签
    private Dialog goneDialog;            //放飞对话框

    private boolean lock = false;            //是否加锁
    private int n_time = -1;                //限制保存天数
    private int n_count = -1;                //限制浏览次数
    private int color;                    //当前皮肤颜色

    private SharedPreferences sp;        //数据存储
    private SQLiteDatabase wn;            //数据库

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);
        wn = Database(R.raw.windnote);
        sp = getSharedPreferences("setting", 0);        //获取设置数据
        color = sp.getInt("color", getResources().getColor(R.color.blue));        //获取皮肤颜色

        add = (LinearLayout) findViewById(R.id.add);
        add.setBackgroundColor(color);
        noteTxt = (EditText) findViewById(R.id.note_txt);
        titleTxt = (EditText) findViewById(R.id.title_txt);
        lengthTxt = (TextView) findViewById(R.id.length_txt);
        clearBtn = (Button) findViewById(R.id.clear_btn);
        noteTxt.addTextChangedListener(change);
        if (getIntent().hasExtra("title"))        //还原未保存的数据
        {
            Bundle data = getIntent().getExtras();
            if (data.containsKey("title"))
                titleTxt.setText(data.getString("title"));
            if (data.containsKey("content"))
                noteTxt.setText(data.getString("content"));
            if (data.containsKey("lock"))
                lock = data.getBoolean("lock");
        }

        backBtn = (ImageButton) findViewById(R.id.back_btn);
        lockBtn = (ImageButton) findViewById(R.id.lock_btn);
        goneBtn = (ImageButton) findViewById(R.id.gone_btn);
        saveBtn = (ImageButton) findViewById(R.id.save_btn);

        clearBtn.setOnClickListener(new View.OnClickListener() {        //清空内容事件
            @Override
            public void onClick(View view) {
                View deleteView = View.inflate(Add.this, R.layout.deletenote, null);
                final Dialog clearDialog = new Dialog(Add.this, R.style.dialog);
                clearDialog.setContentView(deleteView);
                Button yesBtn = (Button) deleteView.findViewById(R.id.delete_yes);
                yesBtn.setText(R.string.clear_note);
                Button noBtn = (Button) deleteView.findViewById(R.id.delete_no);
                noBtn.setText(R.string.clear_cancel);
                yesBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        titleTxt.setText("");
                        noteTxt.setText("");
                        clearDialog.dismiss();
                    }
                });
                noBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clearDialog.dismiss();
                    }
                });
                clearDialog.show();
            }
        });
        ImageButton[] btns = {backBtn, lockBtn, goneBtn, saveBtn};
        for (ImageButton btn : btns)        //添加按钮事件监听
            btn.setOnClickListener(click);
        setLock(lock);
    }

    public void setLock(Boolean b) {            //加锁（解锁）
        lockBtn.setImageResource(b == true ? R.drawable.unlock : R.drawable.lock);
        EditText[] txts = {titleTxt, noteTxt};
        for (EditText txt : txts) {
            focus(txt, !b);
            txt.setTextColor(b == true ? getResources().getColor(R.color.darkgray) : color);
            txt.setBackgroundResource(b == true ? R.color.gray : R.color.white);
        }
        lengthTxt.setTextColor(b == true ? getResources().getColor(R.color.darkgray) : color);
        lengthTxt.setBackgroundResource(b == true ? R.color.gray : R.color.white);
        clearBtn.setTextColor(b == true ? getResources().getColor(R.color.darkgray) : color);
        clearBtn.setEnabled(!b);
        clearBtn.setBackgroundResource(b == true ? R.color.gray : R.color.white);
    }

    public static void focus(EditText view, Boolean b) {        //失去（得到）焦点
        view.setCursorVisible(b);
        view.setFocusable(b);
        view.setFocusableInTouchMode(b);
        if (b == true)
            view.requestFocus();
        else
            view.clearFocus();
        Spannable text = (Spannable) view.getText();
        Selection.setSelection(text, text.length());
    }

    private void save()            //保存记事
    {
        String n_title = titleTxt.getText().toString().trim();
        if (n_title.length() == 0)
            n_title = "无标题";
        String n_content = noteTxt.getText().toString().trim();
        Boolean n_lock = lock;
        if (n_content.trim().length() > 0) {
            wn.execSQL("insert into notes(n_title,n_content,n_time,n_count,n_lock) values(?,?,?,?,?)", new Object[]{n_title, n_content, n_time, n_count, n_lock});
            Toast.makeText(Add.this, R.string.note_saved, Toast.LENGTH_SHORT).show();
            sp.edit().remove("time").commit();
            sp.edit().remove("count").commit();
            Intent intent = new Intent(Add.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else
            Toast.makeText(Add.this, R.string.note_null, Toast.LENGTH_SHORT).show();
    }

    private void back() {        //返回主界面
        Intent intent = new Intent(Add.this, MainActivity.class);
        String title = titleTxt.getText().toString().trim();
        String content = noteTxt.getText().toString().trim();
        if (title.length() > 0 || content.length() > 0)        //传递未保存的数据
        {
            Bundle data = new Bundle();
            data.putString("title", title);
            data.putString("content", content);
            data.putBoolean("lock", lock);
            intent.putExtras(data);
        }
        startActivity(intent);
        finish();
    }

    private void gone() {        //放飞
        goneDialog = new Dialog(this, R.style.dialog);
        View goneView = View.inflate(this, R.layout.gone, null);
        goneDialog.setContentView(goneView);
        final EditText timeTxt = (EditText) goneView.findViewById(R.id.time_txt);
        final EditText countTxt = (EditText) goneView.findViewById(R.id.count_txt);
        if (!sp.getString("time", "").equals("-1"))
            timeTxt.setText(sp.getString("time", ""));
        if (!sp.getString("count", "").equals("-1"))
            countTxt.setText(sp.getString("count", ""));
        Button confirmBtn = (Button) goneView.findViewById(R.id.gone_confirm);
        Button cancelBtn = (Button) goneView.findViewById(R.id.gone_cancel);
        confirmBtn.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);        //下划线效果
        cancelBtn.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timeTxt.getText().length() > 0)
                    n_time = Integer.parseInt(timeTxt.getText().toString());
                if (countTxt.getText().length() > 0)
                    n_count = Integer.parseInt(countTxt.getText().toString());
                sp.edit().putString("time", String.valueOf(n_time)).commit();
                sp.edit().putString("count", String.valueOf(n_count)).commit();
                goneDialog.dismiss();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goneDialog.dismiss();
            }
        });
        goneDialog.show();
    }

    public SQLiteDatabase Database(int raw_id) {        //数据库连接，从raw中读取文件
        try {
            int BUFFER_SIZE = 100000;
            String DB_NAME = "windnote.db";
            String PACKAGE_NAME = "com.wxc.fengzhinote";
            String DB_PATH = "/data"
                    + Environment.getDataDirectory().getAbsolutePath() + "/"
                    + PACKAGE_NAME + "/databases/";
            File destDir = new File(DB_PATH);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            String file = DB_PATH + DB_NAME;
            if (!(new File(file).exists())) {
                InputStream is = this.getResources().openRawResource(
                        raw_id);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
            return db;
        } catch (FileNotFoundException e) {
            Log.e("Database", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Database", "IO exception");
            e.printStackTrace();
        }
        return null;
    }

    private TextWatcher change = new TextWatcher() {        //文本改变监听
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                lengthTxt.setVisibility(View.VISIBLE);
                clearBtn.setVisibility(View.VISIBLE);
                lengthTxt.setText(String.valueOf(s.length()));
            } else {
                lengthTxt.setVisibility(View.GONE);
                clearBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event)        //返回事件重写
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return true;
        }
        return false;
    }

    private View.OnClickListener click = new View.OnClickListener() {        //点击事件监听

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.back_btn:
                    back();
                    break;
                case R.id.lock_btn:
                    lock = !lock;
                    setLock(lock);
                    break;
                case R.id.gone_btn:
                    gone();
                    break;
                case R.id.save_btn:
                    save();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}
