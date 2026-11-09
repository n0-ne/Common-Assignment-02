package com.example.photoviewer;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {

    TextView textView;
    RecyclerView recyclerView;
    EditText searchInput;
    Button viewModeButton;
    Button themeButton;

    final List<PostItem> allPosts = new ArrayList<>();
    List<PostItem> visiblePosts = new ArrayList<>();
    boolean gridMode;
    boolean darkMode;
    SharedPreferences viewerPreferences;

    // Android 에뮬레이터에서 10.0.2.2는 PC의 localhost를 의미한다.
    String site_url = "https://dbrwogud46.pythonanywhere.com";

    JSONObject post_json;
    String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;

    // ID 11에서 업로드 기능을 구현
    PutPost taskUpload;
    private static final String AUTHOR_ID = "1";

    Uri uploadImageUri;
    ImageView uploadPreview;

    final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && uploadPreview != null) {
                            uploadImageUri = uri;
                            uploadPreview.setImageURI(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewerPreferences = getSharedPreferences(
                "photo_viewer_preferences",
                MODE_PRIVATE
        );
        darkMode = viewerPreferences.getBoolean(
                "dark_mode",
                false
        );
        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 상태표시줄 겹침 수정
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        searchInput = findViewById(R.id.edit_search);
        viewModeButton = findViewById(R.id.btn_view_mode);
        themeButton = findViewById(R.id.btn_theme);

        gridMode = viewerPreferences.getBoolean(
                "grid_mode",
                false
        );
        updateFeatureButtons();
        updateRecyclerView();
    }

    public void onClickDownload(View v) {
        if (taskDownload != null
                && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");

        Toast.makeText(
                getApplicationContext(),
                "Download",
                Toast.LENGTH_LONG
        ).show();
    }

    public void onClickUpload(View v) {
        if (taskUpload != null
                && taskUpload.getStatus()
                == AsyncTask.Status.RUNNING) {

            Toast.makeText(
                    this,
                    "현재 게시물을 업로드하고 있습니다.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        View form = getLayoutInflater().inflate(
                R.layout.dialog_upload,
                null
        );

        EditText editTitle =
                form.findViewById(R.id.edit_title);

        EditText editText =
                form.findViewById(R.id.edit_text);

        Button selectImage =
                form.findViewById(R.id.btn_select_image);

        uploadPreview =
                form.findViewById(R.id.image_preview);

        uploadImageUri = null;

        selectImage.setOnClickListener(
                view -> imagePicker.launch("image/*")
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("새로운 이미지 게시")
                        .setView(form)
                        .setPositiveButton("업로드", null)
                        .setNegativeButton("취소", null)
                        .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(button -> {

                    String title =
                            editTitle.getText()
                                    .toString()
                                    .trim();

                    String text =
                            editText.getText()
                                    .toString()
                                    .trim();

                    if (title.isEmpty()) {
                        editTitle.setError("제목을 입력하세요.");
                        return;
                    }

                    if (text.isEmpty()) {
                        editText.setError("내용을 입력하세요.");
                        return;
                    }

                    if (uploadImageUri == null) {
                        Toast.makeText(
                                this,
                                "이미지를 선택하세요.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    taskUpload = new PutPost();
                    taskUpload.execute(title, text);

                    dialog.dismiss();
                })
        );

        dialog.show();
    }

    public void onClickSearch(View v) {
        String query = searchInput.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.getDefault());

        visiblePosts = new ArrayList<>();

        for (PostItem item : allPosts) {
            if (query.isEmpty()
                    || item.getTitle()
                    .toLowerCase(Locale.getDefault())
                    .contains(query)) {
                visiblePosts.add(item);
            }
        }

        updateRecyclerView();

        if (query.isEmpty()) {
            textView.setText(
                    "전체 게시글: " + visiblePosts.size() + "개"
            );
        } else {
            textView.setText(
                    "제목 검색 결과: " + visiblePosts.size() + "개"
            );
        }
    }

    public void onClickViewMode(View v) {
        gridMode = !gridMode;
        viewerPreferences.edit()
                .putBoolean("grid_mode", gridMode)
                .apply();
        updateFeatureButtons();
        updateRecyclerView();
    }

    public void onClickTheme(View v) {
        darkMode = !darkMode;
        viewerPreferences.edit()
                .putBoolean("dark_mode", darkMode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void updateFeatureButtons() {
        viewModeButton.setText(
                gridMode ? "목록 보기" : "격자 보기"
        );
        themeButton.setText(
                darkMode ? "라이트 모드" : "다크 모드"
        );
    }

    private void updateRecyclerView() {
        recyclerView.setLayoutManager(
                gridMode
                        ? new GridLayoutManager(this, 2)
                        : new LinearLayoutManager(this)
        );
        recyclerView.setAdapter(
                new ImageAdapter(
                        visiblePosts,
                        gridMode,
                        this::showFullScreenImage
                )
        );
    }

    private void showFullScreenImage(PostItem item) {
        Dialog dialog = new Dialog(
                this,
                android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen
        );

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText(item.getTitle());
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(16, 24, 16, 16);

        ImageView image = new ImageView(this);
        image.setImageBitmap(item.getBitmap());
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("전체 화면 이미지");

        container.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        container.addView(
                image,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        image.setOnClickListener(view -> dialog.dismiss());
        dialog.setContentView(container);
        dialog.show();
    }

    private class CloadImage
            extends AsyncTask<String, Integer, List<PostItem>> {

        @Override
        protected List<PostItem> doInBackground(String... urls) {
            List<PostItem> postItems = new ArrayList<>();

            try {
                String apiUrl = urls[0];

                // 강의자료의 Socket 요청 메시지 형식을 재현하기 위한 헤더이다.
                String token =
                        "bf46b8f9337d1d27b4ef2511514c798be1a954b8";

                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn =
                        (HttpURLConnection) urlAPI.openConnection();

                conn.setRequestProperty(
                        "Authorization",
                        "Token " + token
                );
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(is)
                            );

                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    reader.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = aryJson.getJSONObject(i);
                        imageUrl = post_json.getString("image");
                        String title = post_json.optString(
                                "title",
                                "제목 없음"
                        );

                        if (!imageUrl.equals("")
                                && !imageUrl.equals("null")) {
                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imageConn =
                                    (HttpURLConnection)
                                            myImageUrl.openConnection();

                            imageConn.setConnectTimeout(3000);
                            imageConn.setReadTimeout(3000);

                            InputStream imgStream =
                                    imageConn.getInputStream();

                            Bitmap imageBitmap =
                                    BitmapFactory.decodeStream(imgStream);

                            if (imageBitmap != null) {
                                postItems.add(
                                        new PostItem(title, imageBitmap)
                                );
                            }

                            imgStream.close();
                            imageConn.disconnect();
                        }
                    }
                }

                conn.disconnect();

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return postItems;
        }

        @Override
        protected void onPostExecute(List<PostItem> posts) {
            allPosts.clear();
            allPosts.addAll(posts);
            visiblePosts = new ArrayList<>(allPosts);

            if (posts.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText(
                        "이미지 로드 성공! 전체 게시글: "
                                + posts.size()
                                + "개"
                );
            }

            updateRecyclerView();
        }
    }

    private class PutPost
            extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... values) {
            String boundary =
                    "----PhotoViewer"
                            + System.currentTimeMillis();

            HttpURLConnection conn = null;

            try {
                URL url = new URL(
                        site_url + "/api_root/Post/"
                );

                conn = (HttpURLConnection)
                        url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                conn.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );

                try (
                        DataOutputStream output =
                                new DataOutputStream(
                                        conn.getOutputStream()
                                );

                        InputStream imageInput =
                                getContentResolver()
                                        .openInputStream(
                                                uploadImageUri
                                        )
                ) {
                    writeTextPart(
                            output,
                            boundary,
                            "author",
                            AUTHOR_ID
                    );

                    writeTextPart(
                            output,
                            boundary,
                            "title",
                            values[0]
                    );

                    writeTextPart(
                            output,
                            boundary,
                            "text",
                            values[1]
                    );

                    String publishedDate =
                            new SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                                    Locale.getDefault()
                            ).format(new Date());

                    writeTextPart(
                            output,
                            boundary,
                            "published_date",
                            publishedDate
                    );

                    String mimeType =
                            getContentResolver()
                                    .getType(uploadImageUri);

                    if (mimeType == null) {
                        mimeType = "image/jpeg";
                    }

                    String extension =
                            MimeTypeMap.getSingleton()
                                    .getExtensionFromMimeType(
                                            mimeType
                                    );

                    if (extension == null) {
                        extension = "jpg";
                    }

                    output.writeBytes(
                            "--" + boundary + "\r\n"
                    );

                    output.writeBytes(
                            "Content-Disposition: form-data; "
                                    + "name=\"image\"; "
                                    + "filename=\"upload."
                                    + extension
                                    + "\"\r\n"
                    );

                    output.writeBytes(
                            "Content-Type: "
                                    + mimeType
                                    + "\r\n\r\n"
                    );

                    byte[] buffer = new byte[4096];
                    int length;

                    while ((length =
                            imageInput.read(buffer)) != -1) {

                        output.write(
                                buffer,
                                0,
                                length
                        );
                    }

                    output.writeBytes(
                            "\r\n--"
                                    + boundary
                                    + "--\r\n"
                    );

                    output.flush();
                }

                return conn.getResponseCode();

            } catch (Exception e) {
                e.printStackTrace();
                return -1;

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(
                Integer responseCode
        ) {
            if (responseCode
                    == HttpURLConnection.HTTP_CREATED) {

                Toast.makeText(
                        MainActivity.this,
                        "게시물 업로드 성공",
                        Toast.LENGTH_LONG
                ).show();

            } else {
                Toast.makeText(
                        MainActivity.this,
                        "업로드 실패: " + responseCode,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void writeTextPart(
            DataOutputStream output,
            String boundary,
            String name,
            String value
    ) throws IOException {

        output.writeBytes(
                "--" + boundary + "\r\n"
        );

        output.writeBytes(
                "Content-Disposition: form-data; "
                        + "name=\""
                        + name
                        + "\"\r\n"
        );

        output.writeBytes(
                "Content-Type: "
                        + "text/plain; charset=UTF-8"
                        + "\r\n\r\n"
        );

        output.write(
                value.getBytes(StandardCharsets.UTF_8)
        );

        output.writeBytes("\r\n");
    }
}
