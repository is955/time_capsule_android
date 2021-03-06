package com.example.timecapsule_2;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainPage extends AppCompatActivity {

    String username;
    List<Contents> contentsList = new ArrayList<>();
    MyAdapter mMyAdapter;
    RecyclerView mRecyclerView;
    Handler handler;
    SharedPreferences preferences;
    ProgressDialog progressDialog;
    private SharedPreferences.Editor editor;
    FloatingActionsMenu fam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        Bundle bundle = this.getIntent().getExtras();
        username = bundle.getString("username");
        mRecyclerView = findViewById(R.id.recyclerview);
        //??????handler?????????????????????
        handler = new uiHandler();
        //??????????????????????????????
        mMyAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mMyAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainPage.this);
        mRecyclerView.setLayoutManager(layoutManager);
        fam = findViewById(R.id.multiple_actions_up);
        ActivityData.getInstance().addActivity(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();

//        addPage.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showInfo("123");
//            }
//        });


        //??????????????????
        final FloatingActionButton editAction = findViewById(R.id.edit);
        editAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToAdd();
                fam.collapse();
            }
        });

        //??????????????????
        final FloatingActionButton setAction = findViewById(R.id.set);
        setAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainPage.this, SettingsActivity.class);
                startActivity(intent);
                fam.collapse();
            }
        });

        //??????????????????
        final FloatingActionButton exitAction = findViewById(R.id.exit);
        exitAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityData.getInstance().exit();
            }
        });

    }

    public void closeFBT(View v){
        fam.collapse();
    }


    @Override
    protected void onStart() {
        super.onStart();
        //????????????????????????
        boolean bgmSet = preferences.getBoolean("bgmSet", BgmData.isBgmState());
        if (bgmSet && !BgmData.isBgmState()) {
            BgmData.setBgmState(true);
            startService(BgmData.getBgmS());
        } else if (!bgmSet) {
            BgmData.setBgmState(false);
            stopService(BgmData.getBgmS());
        }



        //????????????????????????
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                Connection conn = null;
                conn = (Connection) DBOpenHelper.getConn();
                String sqlQ = "select emailRemind from user where username='" + username + "'";
                try {
                    Statement st = (Statement) conn.createStatement();
                    ResultSet rs = st.executeQuery(sqlQ);
                    boolean nowState = false;
                    boolean setEmailState = false;
                    if (rs.next()) {
                        System.out.println(rs.getInt(1));
//                        Log.i("???????????????????????????", "" + rs.getInt(1));
                        nowState = rs.getInt(1) == 1;
//                        Log.i("?????????001???", "" + (rs.getInt(1) == 1));
                        setEmailState=preferences.getBoolean("emailRemind", nowState);
                        editor.putBoolean("emailRemind", nowState);
                    }

                    if (setEmailState && !nowState) {
                        String sqlU = "update user set emailRemind=1 where username='" + username + "'";
                        st.executeUpdate(sqlU);
                    } else if (!setEmailState && nowState) {
                        String sqlU = "update user set emailRemind=0 where username='" + username + "'";
                        st.executeUpdate(sqlU);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        //????????????????????????????????????
        showProgressDialog("????????????", "??????????????????,?????????...");
        getData("https://tc.ruut.cn:8100/get?username=" + username);
    }

    /*** ???????????? ***/
    class MyAdapter extends RecyclerView.Adapter<MyViewHoder> {

        @NonNull
        @Override
        public MyViewHoder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(MainPage.this, R.layout.item_list, null);
            MyViewHoder myViewHoder = new MyViewHoder(view);
            return myViewHoder;
        }


        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(@NonNull MyViewHoder holder, @SuppressLint("RecyclerView") int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fam.collapse();
                }
            });
            Contents c = contentsList.get(position);
            Log.i("?????????001:", position+" "+c.state+" "+c.content);
            holder.vOpen.setEnabled(false);

            holder.vContent.setText(c.showContent);
            holder.vEndTime.setText(c.endTime);
            if (c.state) {
                holder.vOpen.setBackground(getResources().getDrawable(R.drawable.open_bt_style));
                holder.vOpen.setEnabled(true);
//                holder.vOpen.setText("??????");
            }


            holder.vOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //???????????????????????????????????????
                    new XPopup.Builder(MainPage.this)
                            .asConfirm("???????????????" + contentsList.get(position).startTime, contentsList.get(position).content,
                                    null, null, null
                                    , null, false, R.layout.content_dialog)//??????????????????
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return contentsList.size();
        }


    }

    class MyViewHoder extends RecyclerView.ViewHolder {
        TextView vContent;
        TextView vEndTime;
        Button vOpen;

        public MyViewHoder(@NonNull View itemView) {
            super(itemView);
            vContent = itemView.findViewById(R.id.content);
            vEndTime = itemView.findViewById(R.id.endTime);
            vOpen = itemView.findViewById(R.id.open);
        }
    }

    //    ????????????????????????????????????
    public void getData(String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(20_000, TimeUnit.MILLISECONDS)
                .connectTimeout(20_000, TimeUnit.MILLISECONDS)
                .readTimeout(20_000, TimeUnit.MILLISECONDS)
                .writeTimeout(20_000, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //????????????
                handler.sendEmptyMessage(3);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    try {
                        contentsList.clear();
                        Log.i("????????????:", "onResponse: " + contentsList.size());
                        if (result.equals("()")) {
                            handler.sendEmptyMessage(2);
                        } else {
                            JSONArray jsonArray = new JSONArray(result);
                            Date date = new Date();//?????????????????????
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");//??????????????????
                            String nowTime = df.format(date);
                            Log.i("???????????????", "onResponse: " + nowTime);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String content = jsonObject.getString("content");
                                String startTime = jsonObject.getString("startTime");
                                String endTime = jsonObject.getString("endTime");

                                Contents item = new Contents();

                                if (nowTime.compareTo(endTime) < 0) {
                                    if (content.length() < 5) {
                                        item.showContent = content + "......\n\n" + "?????????????????????????????????????????????...";
                                    } else {
                                        item.showContent = content.substring(0, 5) + "......\n\n" + "?????????????????????????????????????????????......";
                                    }
                                    item.state = false;
                                } else {
                                    item.showContent = content;
                                    item.state = true;
                                }
                                item.content = content;
                                item.startTime = startTime;
                                item.endTime = endTime;
                                contentsList.add(item);
                            }
                            //??????ui????????????????????????
                            handler.sendEmptyMessage(1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //??????UI???????????????UI????????????
                }
            }
        });
    }

    //    ??????????????????
    private class uiHandler extends Handler {
        @SuppressLint("NotifyDataSetChanged")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //????????????????????????
                    mMyAdapter.notifyItemRangeChanged(0, mMyAdapter.getItemCount() + 1);
                    mMyAdapter.notifyDataSetChanged();
                    hideProgressDialog();
                    break;
                case 2:
                    mMyAdapter.notifyDataSetChanged();
                    hideProgressDialog();
                    showInfo("?????????");
                    break;
                case 3:
                    showInfo("????????????,??????????????????????????????");
                    hideProgressDialog();
                    break;
            }
        }
    }

    /*
     * ????????????
     */
    public void showProgressDialog(String title, String message) {
        if (progressDialog == null) {

            progressDialog = ProgressDialog.show(this,
                    title, message, true, false);
        } else if (progressDialog.isShowing()) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
        }

        progressDialog.show();

    }

    /*
     * ??????????????????
     */
    public void hideProgressDialog() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

    }


    public void showInfo(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    public void jumpToAdd() {
        Intent intent = new Intent(this, AddPage.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }
}