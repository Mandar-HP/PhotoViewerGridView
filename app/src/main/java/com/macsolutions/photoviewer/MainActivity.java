package com.macsolutions.photoviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    RecyclerView gridRecyView;
    NestedScrollView idNestedSV;
    RecyclerViewAdapter adapter;
    GridLayoutManager layoutManager;
    ArrayList<DataModel> dataModelArrayList;
    private ProgressBar loadingPB;
    int page = 0, limit = 15;
    public static SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gridRecyView = findViewById(R.id.gridRecyView);
        idNestedSV = findViewById(R.id.idNestedSV);
        loadingPB = findViewById(R.id.idPBLoading);

        dataModelArrayList = new ArrayList<>();

        db = openOrCreateDatabase("macsolutions.photoviewer",MODE_PRIVATE,null);
        String sqlStatementCreateTable = "CREATE TABLE IF NOT EXISTS "+"mainTable"+"(id VARCHAR,author VARCHAR ,width VARCHAR," +
                " height VARCHAR, url VARCHAR,download_url VARCHAR);";
        db.execSQL(sqlStatementCreateTable);

        if (isNetworkAvailable())
        {
            apiCall(page,limit);
        }
        else
        {
            loadfromTable();
        }

        idNestedSV.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    // in this method we are incrementing page number,
                    // making progress bar visible and calling get data method.
                    page++;
                    loadingPB.setVisibility(View.VISIBLE);
                    if (isNetworkAvailable())
                    {
                        apiCall(page,limit);
                    }
                    else
                    {
                        loadfromTable();
                    }
                }
            }
        });
    }

    private void loadfromTable() {
        db.beginTransaction();
        Cursor c21 = db.rawQuery("SELECT * FROM " + "mainTable" + " ", null);
        int count = c21.getCount();
        if (count>0)
        {
            dataModelArrayList.clear();
            while (c21.moveToNext())
            {
                String id = c21.getString(0);
                String author = c21.getString(1);
                String width = c21.getString(2);
                String height = c21.getString(3);
                String url = c21.getString(4);
                String download_url = c21.getString(5);

                dataModelArrayList.add(new DataModel(id,author,width,height
                        ,url,download_url));
            }
            db.endTransaction();
            adapter = new RecyclerViewAdapter(dataModelArrayList,MainActivity.this);
            layoutManager = new GridLayoutManager(MainActivity.this,2);
            gridRecyView.setAdapter(adapter);
            gridRecyView.setLayoutManager(layoutManager);
        }
        else
        {
            Toast.makeText(this, "Please enable data connection !", Toast.LENGTH_SHORT).show();
            loadingPB.setVisibility(View.GONE);
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void apiCall(int page,int limit) {
        String pageStr = String.valueOf(page);

        if (page > limit) {
            Toast.makeText(this, "That's all the data..", Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncHttpClient httpClient = new AsyncHttpClient();
        httpClient.get("https://picsum.photos/v2/list?page="+ pageStr/*+"&limit="+limit*/, new JsonHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    //JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray responseArr = response;
                    if (!responseArr.toString().contains("[[]]"))
                    {
                        //SQLOperation.DeleteTable(Tablecreation.MainCategoryMaster);
                        db.beginTransaction();
                        String sql15ws = "INSERT INTO " + "mainTable" + "(id ,author,width ," +
                                "height , url,download_url) VALUES (?,?,?,?,?,?)";
                        SQLiteStatement stmt15ws = db.compileStatement(sql15ws);

                        for (int i = 0; i < responseArr.length(); i++)
                        {
                            JSONObject osbj = responseArr.getJSONObject(i);
                            String id = osbj.getString("id");
                            String author = osbj.getString("author");
                            String width = osbj.getString("width");
                            String height = osbj.getString("height");
                            String url = osbj.getString("url");
                            String download_url = osbj.getString("download_url");
                            stmt15ws.bindString(1, id);
                            stmt15ws.bindString(2, author);
                            stmt15ws.bindString(3, width);
                            stmt15ws.bindString(4, height);
                            stmt15ws.bindString(5, url);
                            stmt15ws.bindString(6, download_url);
                            stmt15ws.execute();
                            stmt15ws.clearBindings();

                            dataModelArrayList.add(new DataModel(id,author,width,height
                                    ,url,download_url));
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();

                        adapter = new RecyclerViewAdapter(dataModelArrayList,MainActivity.this);
                        layoutManager = new GridLayoutManager(MainActivity.this,2);
                        gridRecyView.setAdapter(adapter);
                        gridRecyView.setLayoutManager(layoutManager);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
            }
        });
        //loadingPB.setVisibility(View.GONE);
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>
    {

        private ArrayList<DataModel> courseDataArrayList;
        private Context mcontext;

        public RecyclerViewAdapter(ArrayList<DataModel> recyclerDataArrayList, Context mcontext) {
            this.courseDataArrayList = recyclerDataArrayList;
            this.mcontext = mcontext;
        }

        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate Layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder holder, @SuppressLint("RecyclerView") int position) {
            // Set the data to textview and imageview.
            DataModel recyclerData = courseDataArrayList.get(position);
            holder.courseTV.setText(recyclerData.getAuthor());
            if(mcontext!=null)
            {
                Glide.with(mcontext)
                        .load(recyclerData.getDownload_url())
                        .thumbnail(0.5f)
                        .into(holder.imageView);
            }
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this,ImageViewer.class);
                    intent.putExtra("id",courseDataArrayList.get(position).getDownload_url());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            // this method returns the size of recyclerview
            return courseDataArrayList.size();
        }

        // View Holder Class to handle Recycler View.
        public class RecyclerViewHolder extends RecyclerView.ViewHolder {

            private TextView courseTV;
            private ImageView imageView;

            public RecyclerViewHolder(@NonNull View itemView) {
                super(itemView);
                courseTV = itemView.findViewById(R.id.idTVCourse);
                imageView = itemView.findViewById(R.id.idIVcourseIV);
            }
        }
    }

}