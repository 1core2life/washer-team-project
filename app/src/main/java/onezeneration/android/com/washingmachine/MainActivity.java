package onezeneration.android.com.washingmachine;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.os.Handler;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

     String myJSON;

     EditText name[] = new EditText[24];                                                             //입력한 이름
     String inputText[] = new String[24];                                                           //변화 텍스트 임시 저장
     Button button[] = new Button[24];
     Button confirmButton;                                                                          //10분 이내 확인 버튼

    static Boolean confirmBool = false;                                                             //컨펌 버튼 플래그

    static final String TAG_RESULTS="result";
    static final String TAG_NUMBER = "number";
    static final String TAG_NAME = "name";

    JSONArray jsonArray = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for(int i=0; i<24; i++) {
            int id = getResources().getIdentifier("editText"+i, "id", getPackageName());
            name[i] = (EditText) findViewById(id);
            name[i].addTextChangedListener(watcher);
            id = getResources().getIdentifier("button"+i, "id", getPackageName());
            button[i] = (Button) findViewById(id) ;
            button[i].setOnClickListener(listener);
        }

        confirmButton = (Button) findViewById(R.id.CompleteID);
        confirmButton.setOnClickListener(confirm);

        GetTimeThread subThread = new GetTimeThread(mainHandler);                                   // 멀티 쓰레드 시작 /오늘 날짜와 어제 날짜 비교
        subThread.setDaemon(true);
        subThread.start();

        confirmButton confirmThread = new confirmButton(confirmHandler);                        // 컨펌 버튼 눌렸는지 확인
        confirmThread.setDaemon(true);
        confirmThread.start();
        getData("http://--/outputDb.php");                                               //서버 데이터 읽기

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        // Custom Actionbar를 사용하기 위해 CustomEnabled을 true 시키고 필요 없는 것은 false 시킨다
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);            //액션바 아이콘을 업 네비게이션 형태로 표시합니다.
        actionBar.setDisplayShowTitleEnabled(false);        //액션바에 표시되는 제목의 표시유무를 설정합니다.
        actionBar.setDisplayShowHomeEnabled(false);            //홈 아이콘을 숨김처리합니다.


        //layout을 가지고 와서 actionbar에 포팅을 시킵니다.
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View actionbar = inflater.inflate(R.layout.custom_actionbar, null);

        actionBar.setCustomView(actionbar);

        //액션바 양쪽 공백 없애기
        Toolbar parent = (Toolbar)actionbar.getParent();
        parent.setContentInsetsAbsolute(0,0);

        return true;
    }

    protected void showList(){                                                                      //Json을 변수로 가져옴
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            jsonArray = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i = 0; i< jsonArray.length(); i++){
                JSONObject c = jsonArray.getJSONObject(i);
                String id = c.getString(TAG_NUMBER);
                String names = c.getString(TAG_NAME);
                if(!names.equals("빔")) {                                                           //Json으로 1~24 모두 불러옴
                    name[i].setText(names);                                                         //내용이 있으면 고정
                    name[i].setClickable(false);
                    name[i].setFocusableInTouchMode(false);
                    button[i].setEnabled(false);

                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public void getData(String url){                                                                //url 접속 후 Json 반환
        class GetDataJSON extends AsyncTask<String, Void, String>{

            @Override
            protected String doInBackground(String... params) {

                String uri = params[0];

                BufferedReader bufferedReader = null;
                try {
                    URL url = new URL(uri);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    String json;
                    while((json = bufferedReader.readLine())!= null){
                        sb.append(json+"\n");
                    }

                    return sb.toString().trim();

                }catch(Exception e){
                    return null;
                }



            }

            @Override
            protected void onPostExecute(String result){
                myJSON=result;
                showList();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    private void insertToDatabase(String number, String name){                                      //서버 접속 후 쿼리문

        class InsertData extends AsyncTask<String, Void, String> {
            ProgressDialog loading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Please Wait", null, true, true);  //Dialog 띄움
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();                 //완료 메시지 Toast
            }

            @Override
            protected String doInBackground(String... params) {

                try{
                    String number = (String)params[0];
                    String name = (String)params[1];
                    String link="http://--/insertDb.php?";
                    String data  =  "number="+number;
                    data += "&" + URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8");          //url connection

                    URL url = new URL(link);
                    URLConnection conn = url.openConnection();

                    conn.setDoOutput(true);
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                    wr.write( data );                                                                   //post 값 전달
                    wr.flush();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    StringBuilder sb = new StringBuilder();
                    String line = null;

                    while((line = reader.readLine()) != null)                                       //응답 확인
                    {
                        sb.append(line);
                        break;
                    }
                    return sb.toString();
                }
                catch(Exception e){
                    return new String("Exception: " + e.getMessage());
                }

            }
        }

        InsertData task = new InsertData();
        task.execute(number,name);
    }


    Handler mainHandler = new Handler() {                                                           // 00:00시에 초기화 핸들러
        @Override
        public void handleMessage(Message msg){
            for(int i=0; i<24 ; i++)
            {
                name[i].setText("");
                name[i].setClickable(true);
                name[i].setFocusableInTouchMode(true);
                button[i].setEnabled(true);
                insertToDatabase(String.valueOf(i+1),"빔");
            }
        }
    };

    Handler confirmHandler = new Handler() {                                                        // 10분 넘어서 컨펌 없어 초기화
        @Override
        public void handleMessage(Message msg){
            name[msg.arg1].setText("");
            name[msg.arg1].setClickable(true);
            button[msg.arg1].setEnabled(true);
            insertToDatabase(String.valueOf(msg.arg1+1),"빔");
        }
    };




    TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {                                                   //edittext 내 글자 저장
            for(int i=0; i<24; i++)
             inputText[i] = name[i].getText().toString();
          }
    };


    View.OnClickListener confirm = new View.OnClickListener() {                                      //confirm 버튼 클릭
        @Override
        public void onClick(View v) {
            confirmBool = true;

        }
    };

   View.OnClickListener listener = new View.OnClickListener() {
       @Override
       public void onClick(View v) {

           int gettingId= v.getId();
           for(int i=0; i<24; i++)
           {
               int id = getResources().getIdentifier("button"+i, "id", getPackageName());

               if(gettingId == id)
               {
                   insertToDatabase(String.valueOf(i+1),inputText[i]);
                   name[i].setText(inputText[i]) ;                                                   // 클릭시 저장 후 읽기 전용으로 변경
                   name[i].setFocusable(false);
                   name[i].setClickable(false);
                   button[i].setEnabled(false);
                }
           }

       }
   };
}


class GetTimeThread extends Thread {                                                                 //현재 시각 비교

    private Handler mainHandler;

    GetTimeThread(Handler h){
        this.mainHandler = h;
    }

    @Override
    public void run(){
        Message msg = Message.obtain();
        msg.arg1 = 0;
        Date date = new Date();
        SimpleDateFormat sdfBefore = new SimpleDateFormat("dd");                                //현재 시각 시:분 형식
        String formatBeforeDate = sdfBefore.format(date);

        while(true)
        {
            SimpleDateFormat sdfNow = new SimpleDateFormat("dd");                                //현재 시각 시:분 형식
            String formatDate = sdfNow.format(date);

            try{
                if(!formatDate.equals(formatBeforeDate)) {
                    mainHandler.sendMessage(msg);                                                   //현재 시각이 00시 -> 메시지 호출
                    formatBeforeDate =formatDate;
                }
                Thread.sleep(30000);
            }
            catch(InterruptedException e){
                System.out.println(e.getMessage());
            }
        }
    }

}


class confirmButton extends Thread{                                                               //Confirm 했는지 확인

    private Handler completedHandler;

    confirmButton(Handler h){
        this.completedHandler = h;
    }

    @Override
    public void run(){
        Message msg = Message.obtain();

        Date date = new Date();
        String formatDate = new SimpleDateFormat("HH").format(date);
        msg.arg1 = Integer.parseInt(formatDate);                                                     //현재 시각 전달  01시 02시..

        formatDate = new SimpleDateFormat("mm").format(date);                                        //분 단위 현재 시각
        int IntformatDate = Integer.parseInt(formatDate);

        Boolean AlreadyHappened = false;

        while(true)
        {
            try{
                if( IntformatDate > 10 && MainActivity.confirmBool != true && AlreadyHappened != true) {
                    completedHandler.sendMessage(msg);                                                        //예약취소
                    AlreadyHappened = true;
                }
                else if(IntformatDate == 0) {
                    if(msg.arg1 == 23)
                        msg.arg1 =0;
                    else
                        msg.arg1++;
                    MainActivity.confirmBool = false;                                                 // 00시에 Bool 초기화
                    AlreadyHappened = false;
                }
                Thread.sleep(40000);
            }
            catch(InterruptedException e){
                System.out.println(e.getMessage());
            }
        }

    }
}