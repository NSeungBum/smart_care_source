package co.suns.heartbpm;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.Collections;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static final String APP_TAG = "HeartBPM";    //APP_TAG 선언

    @BindView(R.id.editHealthDateValue1) TextView mHealthRateTv;        //BindView view를 조금 더 쉽게 선언해주는 툴?

    private HealthDataStore mStore;     // HealthDataStore 변수 선언
    private HealthRateReporter mReporter;       //HealthRateReporter class 변수 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);     //BindView


        // 없어도 됨
        /*HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(this);
        }catch (Exception e){
            e.printStackTrace();
            //Toast.makeText(getApplicationContext(), "Can't connect", Toast.LENGTH_LONG).show(); //
        }*/

        // HealthDataStore의 인스턴스 생성 및 수신자 설정
        mStore = new HealthDataStore(this, mConnectionListener);
        // HealthDataStore에 대한 연결 요청
        mStore.connectService();

    }

    // 활동이 종료되면 HealthDataStore 연결 종료
    @Override
    public void onDestroy() {
        mStore.disconnectService();     // HealthDataStore 연결 끊기
        super.onDestroy();      // 종료
    }


    private final HealthDataStore.ConnectionListener mConnectionListener =
            new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {     // HealthDataStore에 연결이 성공 했을때
            Log.d(APP_TAG, "Health data service is connected.");
            mReporter = new HealthRateReporter(mStore);
            if (isPermissionAcquired()) {       // 만약 권한이 있으면
                mReporter.start(mHealthRateObserver);       // mReporter (옵저버시작)
            } else {        // 권한이 없으면
                requestPermission();    // 권한 받기 시작
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {     // HealthDataStore에 연결이 실패 했을때
            Log.d(APP_TAG, "Health data service를 사용할 수 없다.");
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {      // HealthDataStore에 연결이 끊겼을때
            Log.d(APP_TAG, "Health data service와 연결이 끊어짐.");
            if (!isFinishing()) {
                mStore.connectService();        // 다시 HealthDataStore에 연결
            }
        }
    };

    private void showPermissionAlarmDialog() {      // 권한 화면 보여주기
        if (isFinishing()) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(R.string.notice)     // Alert 제목
                .setMessage(R.string.msg_perm_acquired)     // Alert 메세지
                .setPositiveButton(R.string.ok, null)       // Alert ok 버튼
                .show();
    }

    private void showConnectionFailureDialog(final HealthConnectionErrorResult error) {  // 연결 에러가 났을때
        if (isFinishing()) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // 에러내용 (String 값들은 values - String안에 선언)
        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:        // 연결에러값이 PLATFORM_NOT_INSTALLED이면
                    alert.setMessage(R.string.msg_req_install);     // Alert창으로 삼성헬스를 설치해주세요. 라고 나타냄
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:       // 연결에러값이 OLD_VERSION_PLATFORM이면
                    alert.setMessage(R.string.msg_req_upgrade);      // Alert창으로 삼성헬스를 업그레이드 해주세요. 라고 나타냄
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:      // 연결에러값이 PLATFORM_DISABLED 이면
                    alert.setMessage(R.string.msg_req_enable);       // Alert창으로 삼성헬스를 활성화 해주세요. 라고 나타냄
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:      // 연결에러값이 USER_AGREEMENT_NEEDED 이면
                    alert.setMessage(R.string.msg_req_agree);        // Alert창으로 삼성헬스 정책에 동의 해주세요. 라고 나타냄
                    break;
                default:         // 이 외에 에러는
                    alert.setMessage(R.string.msg_req_available);        // Alert창으로 삼성헬스를 사용할 수 있게 해주세요. 라고 나타냄
                    break;
            }
        } else {        // 위에 내용이 아니면
            alert.setMessage(R.string.msg_conn_not_available);      // Alert창으로 삼성헬스와 연결할 수 없습니다. 라고 나타냄
        }

        alert.setPositiveButton(R.string.ok, (dialog, id) -> {
            if (error.hasResolution()) {
                error.resolve(MainActivity.this);
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton(R.string.cancel, null);
        }

        alert.show();
    }

    // 권한 받기
    private boolean isPermissionAcquired() {
        HealthPermissionManager.PermissionKey permKey =     // HeartRate 권한 받기
                new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        // Error 처리문
        try {
            // 이 응용프로그램을 사용하기 위한 권한을 획득했는지 확인
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(Collections.singleton(permKey));
            return resultMap.get(permKey);
        } catch (Exception e) {     // 획득 못했을시 아래 로그메시지 띄움
            Log.e(APP_TAG, "권한 요청이 실패함.", e);
        }
        return false;
    }

    // 권한 응답
    private void requestPermission() {
        HealthPermissionManager.PermissionKey permKey2 = new HealthPermissionManager.PermissionKey(HealthConstants.SleepStage.SLEEP_ID, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager.PermissionKey permKey =
                new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        // Error 처리문
        try {
            // 사용자에게 옵션 변경을 허용하기 위한 사용자 권한 UI 표시
            pmsManager.requestPermissions(Collections.singleton(permKey), MainActivity.this)
                    .setResultListener(result -> {
                        Log.d(APP_TAG, "권한 콜백이 수신.");
                        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();

                        if (resultMap.containsValue(Boolean.FALSE)) {
                            updateHealthRateView("");
                            showPermissionAlarmDialog();
                        } else {
                            // 현재 심박수를 가져와서 표시!
                            mReporter.start(mHealthRateObserver);
                        }
                    });
        } catch (Exception e) {
            Log.e(APP_TAG, "권한 세팅이 실패함.", e);
        }
    }

    // 심박수 옵져버
    private HealthRateReporter.HealthRateObserver mHealthRateObserver = count -> {
        Log.d(APP_TAG, "Heart Beat: " + count);
        updateHealthRateView(String.valueOf(count));        // HealthRateView에 심박수 값을 업데이트 시킴
    };

    private void updateHealthRateView(final String count) {     // updateHealthRateView 함수
        runOnUiThread(() -> mHealthRateTv.setText(count));      // count값을 mHealthRateTv Textview에 표시, runOnUiThread 는 좀 더 확인필요
    }


    // 앱화면 옵션
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {     // 옵션 메뉴 설정
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.memu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {      // 옵션 아이템 선택했을때

        if (item.getItemId() == R.id.connect) {     // 아이템 선택시 requestPermission() 호출
            requestPermission();        // 권한응답함수
        }

        return true;
    }
}