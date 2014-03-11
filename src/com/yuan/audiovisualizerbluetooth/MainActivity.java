package com.yuan.audiovisualizerbluetooth;

import java.util.Arrays;

import com.customview.graph.*;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.media.audiofx.Visualizer;

public class MainActivity extends Activity {

	private static final String TAG = "AudioVB";

	private Visualizer mVisualizer;
	private BarGraphView mBarGraphView;
	private LineGraphView mLineGraphView;

	private int param = 1;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Layout Views
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	private WakeLock mWakeLock;

	// Member object for the chat services
	// private BluetoothChatService mChatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBarGraphView = (BarGraphView) findViewById(R.id.barGraphMain);
		mLineGraphView = (LineGraphView) findViewById(R.id.lineGraphViewMain);

		setupVisualizer();
		// Make sure the visualizer is enabled only when you actually want to
		// receive data, and
		// when it makes sense to receive data.

		Log.i(TAG, "onCreate called.");
	}

	// Array.Copy(Mod, RawFFTData_New, 256);
	//
	//
	// for (i = 0; i < 256; i++)
	// {
	// AdjustFFTData_New[i] = AdjustFFTData_Old[i] * 0.5F + RawFFTData_New[i] *
	// (1.0F - 0.5F);
	// }
	//
	//
	// //Array.Copy(RawFFTData_New, RawFFTData_Old, 256);
	// Array.Copy(AdjustFFTData_New, AdjustFFTData_Old, 256);
	//
	//
	//
	// spectrumShow1.Data = AdjustFFTData_New;

	float[] fftDataOld;
	float[] fftDataAdj;

	private void fftDataProcess(byte[] data) {

		// mSpecturmNum<=data.length / 2;
		int mSpecturmNum = data.length / 2;

		float[] model = new float[mSpecturmNum];

		fftDataAdj = new float[mSpecturmNum];

		if (fftDataOld == null || fftDataOld.length < mSpecturmNum)
			fftDataOld = new float[mSpecturmNum];

		model[0] = Math.abs(data[0]) / 128f;
		fftDataAdj[0] = fftDataOld[0] * 0.3F + model[0] * (1.0F - 0.3F);

		for (int i = 2, j = 1; j < mSpecturmNum;) {
			model[j] = (float) Math.hypot(data[i], data[i + 1]);
			model[j] = model[j] / 128f;

			fftDataAdj[j] = fftDataOld[j] * 0.5F + model[j] * (1.0F - 0.5F);

			i += 2;
			j++;

		}

		fftDataOld = Arrays.copyOf(fftDataAdj, fftDataAdj.length);

		mBarGraphView.setValueArray(fftDataAdj);
	}

	private void waveDataProcess(byte[] data) {

		float[] normalizedData = new float[data.length];
		for (int i = 0; i < data.length; ++i) {
			normalizedData[i] = ((float) ((byte) (data[i] + 128))) / 128f;
		}
		mLineGraphView.setValueArray(normalizedData);
	}

	private void setupVisualizer() {
		// Create the Visualizer object and attach it to our media player.
		mVisualizer = new Visualizer(0);
		mVisualizer.setCaptureSize(256);// Visualizer.getCaptureSizeRange()[0]);
		mVisualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
		mVisualizer.setDataCaptureListener(
				new Visualizer.OnDataCaptureListener() {
					public void onWaveFormDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
						waveDataProcess(bytes);

					}

					public void onFftDataCapture(Visualizer visualizer,
							byte[] bytes, int samplingRate) {
						fftDataProcess(bytes);

					}
				}, Visualizer.getMaxCaptureRate() / 2, true, true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Activity从后台重新回到前台时被调用
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart called.");
	}

	// Activity创建或者从被覆盖、后台重新回到前台时被调用
	@Override
	protected void onResume() {
		super.onResume();
		mBarGraphView.setBackColor(SettingsActivity
				.getSpecturmBackColor(getApplicationContext()));
		mLineGraphView.setBackColor(SettingsActivity
				.getWaveBackColor(getApplicationContext()));

		mBarGraphView.setExtraText("频域视图");
		mLineGraphView.setExtraText("时域视图");

		mVisualizer.setEnabled(true);

		if (SettingsActivity.getIsKeepOn(getApplicationContext())) {
			PowerManager pManager = ((PowerManager) getSystemService(POWER_SERVICE));
			mWakeLock = pManager.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK
							| PowerManager.ON_AFTER_RELEASE, TAG);
			mWakeLock.acquire();
		}

		Log.i(TAG, "onResume called.");
	}

	// Activity窗口获得或失去焦点时被调用,在onResume之后或onPause之后
	/*
	 * @Override public void onWindowFocusChanged(boolean hasFocus) {
	 * super.onWindowFocusChanged(hasFocus); Log.i(TAG,
	 * "onWindowFocusChanged called."); }
	 */

	// Activity被覆盖到下面或者锁屏时被调用
	@Override
	protected void onPause() {
		super.onPause();
		mVisualizer.setEnabled(false);
		if (isFinishing()) {
			mVisualizer.release();
		}
		if (null != mWakeLock) {
			mWakeLock.release();
		}
		Log.i(TAG, "onPause called.");
		// 有可能在执行完onPause或onStop后,系统资源紧张将Activity杀死,所以有必要在此保存持久数据
	}

	// 退出当前Activity或者跳转到新Activity时被调用
	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop called.");
	}

	// 退出当前Activity时被调用,调用之后Activity就结束了
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestory called.");

	}

	/**
	 * Activity被系统杀死时被调用. 例如:屏幕方向改变时,Activity被销毁再重建;当前Activity处于后台,系统资源紧张将其杀死.
	 * 另外,当跳转到其他Activity或者按Home键回到主屏时该方法也会被调用,系统是为了保存当前View组件的状态. 在onPause之前被调用.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("param", param);
		Log.i(TAG, "onSaveInstanceState called. put param: " + param);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Activity被系统杀死后再重建时被调用.
	 * 例如:屏幕方向改变时,Activity被销毁再重建;当前Activity处于后台,系统资源紧张将其杀死,用户又启动该Activity.
	 * 这两种情况下onRestoreInstanceState都会被调用,在onStart之后.
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		param = savedInstanceState.getInt("param");
		Log.i(TAG, "onRestoreInstanceState called. get param: " + param);
		super.onRestoreInstanceState(savedInstanceState);
	}

}
