package co.suns.heartbpm;

import android.os.AsyncTask;
import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.TimeZone;

public class HealthRateReporter {

    private final HealthDataStore mStore;       // HealthDataStore 선언
    private HealthRateObserver mHealthRateObserver;     // HealthRateObserver 선언
    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;     // 하루를 나타낸다.


    // 소켓?
    private Socket s; //
    private static PrintWriter printWriter;
    String message = "";
    private static String host = "192.168.0.61";
    private static int port = 8888;

    public HealthRateReporter(HealthDataStore store){mStore = store;}

    public void start(HealthRateObserver listener){
        mHealthRateObserver = listener;
        // HeartRate의 값을 관찰하면서 변화되는값들을 확인하고 오늘 데이터를 가져온다
        HealthDataObserver.addObserver(mStore, HealthConstants.HeartRate.HEALTH_DATA_TYPE, mObserver);
        readTodayHeartBPM();
    }

    private void readTodayHeartBPM() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // 오늘의 시작 시간에서 현재 시간까지의 시간 범위 설정
        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.HeartRate.START_TIME, startTime),
                HealthDataResolver.Filter.lessThanEquals(HealthConstants.HeartRate.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)//.setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[] {HealthConstants.HeartRate.HEART_RATE})
                //.setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                //        startTime, endTime)
                .setFilter(filter)
                .build();
        // Error 선언문
        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(MainActivity.APP_TAG, "심박수를 가져오는데 실패했다.", e);
        }
    }

    //  시작시간 메소드
    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT-9"));       // TimeZone 설정(한국 KST, UTC+9, GMT+9)

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener = result -> {
        int count = 0;
        //int count2 = 0;
        //int count3 = 0;

        try {
            for (HealthData data : result) {
                count = data.getInt(HealthConstants.HeartRate.HEART_RATE); //HEART_BEAT_COUNT
                //count2 = data.getInt(HealthConstants.HeartRate.HEART_RATE);
                //count = data.getInt(HealthConstants.StepCount.COUNT);
            }
        } finally {
            sendData(count);
            result.close();
        }

        if (mHealthRateObserver != null) {
            mHealthRateObserver.onChanged(count);
        }
    };

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {

        // 변경 이벤트가 수신될때마다 심박수를 업데이트
        @Override
        public void onChange(String dataTypeName) {
            Log.d(MainActivity.APP_TAG, "옵져버가 데이터 변화를 감지 이벤트를 수신함");
            readTodayHeartBPM();
        }
    };

    // HealthRateObserver 인터페이스
    public interface HealthRateObserver {
        void onChanged(int count);      // onChanged
    }

    // 데이터 보내기 함수
    public void sendData(int count) {
        //message = editText.getText().toString();
        message = String.valueOf(count);        // message에 심박수 값을 저장

        myTask mt = new myTask();
        mt.execute();

        //Toast.makeText(getApplicationContext(), "Data sent", Toast.LENGTH_LONG).show();
    }

    //
    public class myTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params) {
            try {
                s = new Socket(host, port); //
                printWriter = new PrintWriter(s.getOutputStream());
                printWriter.write(message);
                printWriter.flush();
                printWriter.close();
                //s.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
