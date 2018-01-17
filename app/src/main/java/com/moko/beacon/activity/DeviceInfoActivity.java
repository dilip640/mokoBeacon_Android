package com.moko.beacon.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.moko.beacon.BeaconConstants;
import com.moko.beacon.R;
import com.moko.beacon.entity.BeaconParam;
import com.moko.beacon.service.BeaconService;
import com.moko.beacon.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderType;
import com.moko.support.task.OrderTask;
import com.moko.support.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @Date 2017/12/13 0013
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.beacon.activity.DeviceInfoActivity
 */
public class DeviceInfoActivity extends BaseActivity {
    @Bind(R.id.tv_conn_state)
    TextView tvConnState;
    @Bind(R.id.tv_ibeacon_battery)
    TextView tvIbeaconBattery;
    @Bind(R.id.tv_ibeacon_uuid)
    TextView tvIbeaconUuid;
    @Bind(R.id.tv_ibeacon_major)
    TextView tvIbeaconMajor;
    @Bind(R.id.tv_ibeacon_minor)
    TextView tvIbeaconMinor;
    @Bind(R.id.tv_ibeacon_measure_power)
    TextView tvIbeaconMeasurePower;
    @Bind(R.id.tv_ibeacon_transmission)
    TextView tvIbeaconTransmission;
    @Bind(R.id.tv_ibeacon_broadcasting_interval)
    TextView tvIbeaconBroadcastingInterval;
    @Bind(R.id.tv_ibeacon_serialID)
    TextView tvIbeaconSerialID;
    @Bind(R.id.tv_ibeacon_mac)
    TextView tvIbeaconMac;
    @Bind(R.id.tv_ibeacon_device_name)
    TextView tvIbeaconDeviceName;
    @Bind(R.id.tv_ibeacon_device_conn_mode)
    TextView tvIbeaconDeviceConnMode;
    @Bind(R.id.rl_ibeacon_three_axis)
    RelativeLayout rlIbeaconThreeAxis;
    private BeaconService mBeaconService;
    private BeaconParam mBeaconParam;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        ButterKnife.bind(this);
        bindService(new Intent(this, BeaconService.class), mServiceConnection, BIND_AUTO_CREATE);
        mBeaconParam = (BeaconParam) getIntent().getSerializableExtra(BeaconConstants.EXTRA_KEY_DEVICE_PARAM);
        if (mBeaconParam == null) {
            finish();
            return;
        }
        rlIbeaconThreeAxis.setVisibility(!TextUtils.isEmpty(mBeaconParam.threeAxis) ? View.VISIBLE : View.GONE);
        if (MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
            tvConnState.setText(getString(R.string.device_info_conn_status_connected));
        } else {
            tvConnState.setText(getString(R.string.device_info_conn_status_disconnect));
        }
        changeValue();
    }

    private void changeValue() {
        tvIbeaconBattery.setText(mBeaconParam.battery);
        tvIbeaconUuid.setText(mBeaconParam.uuid);
        tvIbeaconMajor.setText(mBeaconParam.major);
        tvIbeaconMinor.setText(mBeaconParam.minor);
        tvIbeaconMeasurePower.setText(String.format("-%sdBm", mBeaconParam.measurePower));
        tvIbeaconTransmission.setText(mBeaconParam.transmission);
        tvIbeaconBroadcastingInterval.setText(mBeaconParam.broadcastingInterval);
        tvIbeaconSerialID.setText(mBeaconParam.serialID);
        tvIbeaconMac.setText(mBeaconParam.iBeaconMAC);
        tvIbeaconDeviceName.setText(mBeaconParam.iBeaconName);
        tvIbeaconDeviceConnMode.setText("00".equals(mBeaconParam.connectionMode) ? "YES" : "NO");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                abortBroadcast();
                String action = intent.getAction();
                if (MokoConstants.ACTION_CONNECT_SUCCESS.equals(action)) {
                    tvConnState.setText(getString(R.string.device_info_conn_status_connected));
                    // 读取全部可读数据
                    mBeaconService.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBeaconService.getReadableData(mBeaconParam.password);
                        }
                    }, 1000);
                }
                if (MokoConstants.ACTION_CONNECT_DISCONNECTED.equals(action)) {
                    tvConnState.setText(getString(R.string.device_info_conn_status_disconnect));
                    ToastUtils.showToast(DeviceInfoActivity.this, "Connect Failed");
                    dismissLoadingProgressDialog();
                    dismissSyncProgressDialog();
                }
                if (MokoConstants.ACTION_RESPONSE_TIMEOUT.equals(action)) {
                }
                if (MokoConstants.ACTION_RESPONSE_FINISH.equals(action)) {
                    mBeaconService.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dismissLoadingProgressDialog();
                            dismissSyncProgressDialog();
                        }
                    }, 1000);

                }
                if (MokoConstants.ACTION_RESPONSE_SUCCESS.equals(action)) {
                    OrderType orderType = (OrderType) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TYPE);
                    byte[] value = intent.getByteArrayExtra(MokoConstants.EXTRA_KEY_RESPONSE_VALUE);
                    switch (orderType) {
                        case battery:
                            mBeaconParam.battery = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconBattery.setText(mBeaconParam.battery);
                            break;
                        case iBeaconUuid:
                            // 读取UUID成功
                            // ToastUtils.showToast(DeviceInfoActivity.this, "读取UUID成功");
                            String hexString = Utils.bytesToHexString(value).toUpperCase();
                            if (hexString.length() > 31) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(hexString.substring(0, 8));
                                sb.append("-");
                                sb.append(hexString.substring(8, 12));
                                sb.append("-");
                                sb.append(hexString.substring(12, 16));
                                sb.append("-");
                                sb.append(hexString.substring(16, 20));
                                sb.append("-");
                                sb.append(hexString.substring(20, 32));
                                String uuid = sb.toString();
                                mBeaconParam.uuid = uuid;
                                tvIbeaconUuid.setText(uuid);
                                tvIbeaconUuid.setText(mBeaconParam.uuid);
                            }
                            break;
                        case major:
                            mBeaconParam.major = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconMajor.setText(mBeaconParam.major);
                            break;
                        case minor:
                            mBeaconParam.minor = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconMinor.setText(mBeaconParam.minor);
                            break;
                        case measurePower:
                            mBeaconParam.measurePower = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconMeasurePower.setText(String.format("-%sdBm", mBeaconParam.measurePower));
                            break;
                        case transmission:
                            mBeaconParam.transmission = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconTransmission.setText(mBeaconParam.transmission);
                            break;
                        case broadcastingInterval:
                            mBeaconParam.broadcastingInterval = Integer.parseInt(Utils.bytesToHexString(value), 16) + "";
                            tvIbeaconBroadcastingInterval.setText(mBeaconParam.broadcastingInterval);
                            break;
                        case serialID:
                            mBeaconParam.serialID = Utils.hex2String(Utils.bytesToHexString(value));
                            tvIbeaconSerialID.setText(mBeaconParam.serialID);
                            break;
                        case iBeaconMac:
                            String hexMac = Utils.bytesToHexString(value);
                            if (hexMac.length() > 11) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(hexMac.substring(0, 2));
                                sb.append(":");
                                sb.append(hexMac.substring(2, 4));
                                sb.append(":");
                                sb.append(hexMac.substring(4, 6));
                                sb.append(":");
                                sb.append(hexMac.substring(6, 8));
                                sb.append(":");
                                sb.append(hexMac.substring(8, 10));
                                sb.append(":");
                                sb.append(hexMac.substring(10, 12));
                                String mac = sb.toString().toUpperCase();
                                mBeaconParam.iBeaconMAC = mac;
                                mBeaconParam.beaconInfo.iBeaconMac = mac;
                                tvIbeaconMac.setText(mBeaconParam.iBeaconMAC);
                            }
                            break;
                        case iBeaconName:
                            mBeaconParam.iBeaconName = Utils.hex2String(Utils.bytesToHexString(value));
                            tvIbeaconDeviceName.setText(mBeaconParam.iBeaconName);
                            break;
                        case connectionMode:
                            mBeaconParam.connectionMode = Utils.bytesToHexString(value);
                            tvIbeaconDeviceConnMode.setText("00".equals(mBeaconParam.connectionMode) ? "YES" : "NO");
                            break;
                        case firmname:
                            mBeaconParam.beaconInfo.firmname = Utils.hex2String(Utils.bytesToHexString(value));
                            break;
                        case devicename:
                            mBeaconParam.beaconInfo.deviceName = Utils.hex2String(Utils.bytesToHexString(value));
                            break;
                        case iBeaconDate:
                            mBeaconParam.beaconInfo.iBeaconDate = Utils.hex2String(Utils.bytesToHexString(value));
                            break;
                        case hardwareVersion:
                            mBeaconParam.beaconInfo.hardwareVersion = Utils.hex2String(Utils.bytesToHexString(value));
                            break;
                        case firmwareVersion:
                            mBeaconParam.beaconInfo.firmwareVersion = Utils.hex2String(Utils.bytesToHexString(value));
                            break;
                        case writeAndNotify:
                            if ("eb59".equals(Utils.bytesToHexString(Arrays.copyOfRange(value, 0, 2)).toLowerCase())) {
                                byte[] runtimeBytes = Arrays.copyOfRange(value, 4, value.length);
                                int runtime = Integer.parseInt(Utils.bytesToHexString(runtimeBytes), 16);
                                int runtimeDays = runtime / (60 * 60 * 24);
                                int runtimeHours = (runtime % (60 * 60 * 24)) / (60 * 60);
                                int runtimeMinutes = (runtime % (60 * 60)) / (60);
                                int runtimeSeconds = (runtime % (60)) / 1000;
                                mBeaconParam.beaconInfo.runtime = String.format("%dD%dh%dm%ds", runtimeDays, runtimeHours, runtimeMinutes, runtimeSeconds);
                            }
                            if ("eb5b".equals(Utils.bytesToHexString(Arrays.copyOfRange(value, 0, 2)).toLowerCase())) {
                                byte[] chipModelBytes = Arrays.copyOfRange(value, 4, value.length);
                                mBeaconParam.beaconInfo.chipModel = Utils.hex2String(Utils.bytesToHexString(chipModelBytes));
                            }
                            break;
                        case systemMark:
                            mBeaconParam.beaconInfo.systemMark = Utils.bytesToHexString(value);
                        case IEEEInfo:
                            mBeaconParam.beaconInfo.IEEEInfo = Utils.bytesToHexString(value);
                            break;
                        case changePassword:
                            if ("00".equals(Utils.bytesToHexString(value))) {
                                changeValue();
                            }
                            break;
                    }
                }
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBeaconService = ((BeaconService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONNECT_SUCCESS);
            filter.addAction(MokoConstants.ACTION_CONNECT_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_RESPONSE_SUCCESS);
            filter.addAction(MokoConstants.ACTION_RESPONSE_TIMEOUT);
            filter.addAction(MokoConstants.ACTION_RESPONSE_FINISH);
            filter.setPriority(200);
            registerReceiver(mReceiver, filter);
            if (!MokoSupport.getInstance().isBluetoothOpen()) {
                // 蓝牙未打开，开启蓝牙
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, MokoConstants.REQUEST_CODE_ENABLE_BT);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void back() {
        if (MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
            MokoSupport.getInstance().disConnectBle();
        }
        finish();
    }


    @OnClick({R.id.tv_back, R.id.tv_conn_state, R.id.rl_ibeacon_battery, R.id.rl_ibeacon_uuid,
            R.id.rl_ibeacon_major, R.id.rl_ibeacon_minor, R.id.rl_ibeacon_measure_power,
            R.id.rl_ibeacon_transmission, R.id.rl_ibeacon_broadcasting_interval, R.id.rl_ibeacon_serialID,
            R.id.rl_ibeacon_mac, R.id.rl_ibeacon_device_name, R.id.rl_ibeacon_device_conn_mode,
            R.id.rl_ibeacon_change_password, R.id.rl_ibeacon_device_info, R.id.rl_ibeacon_three_axis})
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.tv_back:
                back();
                break;
            case R.id.tv_conn_state:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    mBeaconService.connDevice(mBeaconParam.iBeaconMAC);
                    showLoadingProgressDialog(getString(R.string.dialog_connecting));
                }
                break;
            case R.id.rl_ibeacon_uuid:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetUUIDActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_UUID, mBeaconParam.uuid);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_UUID);
                break;
            case R.id.rl_ibeacon_major:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetMajorActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_MAJOR, Integer.parseInt(mBeaconParam.major));
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_MAJOR);
                break;
            case R.id.rl_ibeacon_minor:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetMinorActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_MINOR, Integer.parseInt(mBeaconParam.minor));
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_MINOR);
                break;
            case R.id.rl_ibeacon_measure_power:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetMeasurePowerActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_MEASURE_POWER, Integer.parseInt(mBeaconParam.measurePower));
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_MEASURE_POWER);
                break;
            case R.id.rl_ibeacon_transmission:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetTransmissionActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_TRANSMISSION, Integer.parseInt(mBeaconParam.transmission));
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_TRANSMISSION);
                break;
            case R.id.rl_ibeacon_broadcasting_interval:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetBroadcastIntervalActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_BROADCASTINTERVAL, Integer.parseInt(mBeaconParam.broadcastingInterval));
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_BROADCASTINTERVAL);
                break;
            case R.id.rl_ibeacon_serialID:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetDeviceIdActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_DEVICE_ID, mBeaconParam.serialID);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_DEVICE_ID);
                break;
            case R.id.rl_ibeacon_device_name:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetIBeaconNameActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_IBEACON_NAME, mBeaconParam.iBeaconName);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_IBEACON_NAME);
                break;
            case R.id.rl_ibeacon_device_conn_mode:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetConnectionModeActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_CONNECTION_MODE, mBeaconParam.connectionMode);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_CONNECTION_MODE);
                break;
            case R.id.rl_ibeacon_change_password:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SetPasswordActivity.class);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_PASSWORD);
                break;
            case R.id.rl_ibeacon_device_info:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, SystemInfoActivity.class);
                intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_INFO, mBeaconParam.beaconInfo);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_SYSTEM_INFO);
                break;
            case R.id.rl_ibeacon_three_axis:
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    ToastUtils.showToast(this, "bluetooth is closed,please open");
                    return;
                }
                if (!MokoSupport.getInstance().isConnDevice(this, mBeaconParam.iBeaconMAC)) {
                    ToastUtils.showToast(this, getString(R.string.alert_click_reconnect));
                    return;
                }
                intent = new Intent(this, ThreeAxesActivity.class);
                startActivityForResult(intent, BeaconConstants.REQUEST_CODE_SET_THREE_AXIS);
                break;
            case R.id.rl_ibeacon_mac:
                ToastUtils.showToast(this, getString(R.string.device_info_cannot_modify));
                break;
            case R.id.rl_ibeacon_battery:
                ToastUtils.showToast(this, getString(R.string.device_info_cannot_modify));
                break;
        }
    }

    private ProgressDialog mLoadingDialog;

    private void showLoadingProgressDialog(String tips) {
        mLoadingDialog = new ProgressDialog(DeviceInfoActivity.this);
        mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage(tips);
        if (!isFinishing() && mLoadingDialog != null && !mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    private void dismissLoadingProgressDialog() {
        if (!isFinishing() && mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    private ProgressDialog mSyncingDialog;

    private void showSyncProgressDialog(String tips) {
        mSyncingDialog = new ProgressDialog(DeviceInfoActivity.this);
        mSyncingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSyncingDialog.setCanceledOnTouchOutside(false);
        mSyncingDialog.setCancelable(false);
        mSyncingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mSyncingDialog.setMessage(tips);
        if (!isFinishing() && mSyncingDialog != null && !mSyncingDialog.isShowing()) {
            mSyncingDialog.show();
        }
    }

    private void dismissSyncProgressDialog() {
        if (!isFinishing() && mSyncingDialog != null && mSyncingDialog.isShowing()) {
            mSyncingDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            switch (requestCode) {
//                case BeaconConstants.REQUEST_CODE_SET_UUID:
//                    if (data != null && data.getExtras() != null) {
//                        String uuid = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_UUID);
//                        tvIbeaconUuid.setText(uuid);
//                        mBeaconParam.uuid = uuid;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_MAJOR:
//                    if (data != null && data.getExtras() != null) {
//                        int major = data.getExtras().getInt(BeaconConstants.EXTRA_KEY_DEVICE_MAJOR, 0);
//                        tvIbeaconMajor.setText(major + "");
//                        mBeaconParam.major = major;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_MINOR:
//                    if (data != null && data.getExtras() != null) {
//                        int minor = data.getExtras().getInt(BeaconConstants.EXTRA_KEY_DEVICE_MINOR, 0);
//                        tvIbeaconMinor.setText(minor + "");
//                        mBeaconParam.minor = minor;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_MEASURE_POWER:
//                    if (data != null && data.getExtras() != null) {
//                        int measurePower = data.getExtras().getInt(BeaconConstants.EXTRA_KEY_DEVICE_MEASURE_POWER, 0);
//                        tvIbeaconMeasurePower.setText(String.format("-%ddBm", measurePower));
//                        mBeaconParam.measurePower = measurePower;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_TRANSMISSION:
//                    if (data != null && data.getExtras() != null) {
//                        int transmission = data.getExtras().getInt(BeaconConstants.EXTRA_KEY_DEVICE_TRANSMISSION, 0);
//                        tvIbeaconMeasurePower.setText(String.valueOf(transmission));
//                        mBeaconParam.transmission = transmission;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_BROADCASTINTERVAL:
//                    if (data != null && data.getExtras() != null) {
//                        int broadcastInterval = data.getExtras().getInt(BeaconConstants.EXTRA_KEY_DEVICE_BROADCASTINTERVAL, 0);
//                        tvIbeaconBroadcastingInterval.setText(String.valueOf(broadcastInterval));
//                        mBeaconParam.broadcastingInterval = broadcastInterval;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_DEVICE_ID:
//                    if (data != null && data.getExtras() != null) {
//                        String deviceId = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_DEVICE_ID);
//                        tvIbeaconSerialID.setText(String.valueOf(deviceId));
//                        mBeaconParam.serialID = deviceId;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_IBEACON_NAME:
//                    if (data != null && data.getExtras() != null) {
//                        String deviceName = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_IBEACON_NAME);
//                        tvIbeaconDeviceName.setText(String.valueOf(deviceName));
//                        mBeaconParam.iBeaconName = deviceName;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_CONNECTION_MODE:
//                    if (data != null && data.getExtras() != null) {
//                        String connectionMode = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_CONNECTION_MODE);
//                        tvIbeaconDeviceConnMode.setText("00".equals(connectionMode) ? "YES" : "NO");
//                        mBeaconParam.connectionMode = connectionMode;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_PASSWORD:
//                    if (data != null && data.getExtras() != null) {
//                        String password = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_PASSWORD);
//                        mBeaconParam.password = password;
//                    }
//                    break;
//                case BeaconConstants.REQUEST_CODE_SET_SYSTEM_INFO:
//                    if (data != null && data.getExtras() != null) {
//                        BeaconDeviceInfo beaconDeviceInfo = (BeaconDeviceInfo) data.getExtras().getSerializable(BeaconConstants.EXTRA_KEY_DEVICE_INFO);
//                        mBeaconParam.beaconInfo = beaconDeviceInfo;
//                    }
//                    break;
//            }
//        }
        if (resultCode == BeaconConstants.RESULT_CONN_DISCONNECTED) {
            tvConnState.setText(getString(R.string.device_info_conn_status_disconnect));
        } else {
            switch (requestCode) {
                case BeaconConstants.REQUEST_CODE_SET_UUID:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.uuid = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_MAJOR:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.major = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_MINOR:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.minor = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_MEASURE_POWER:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.measurePower = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_TRANSMISSION:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.transmission = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_BROADCASTINTERVAL:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.broadcastingInterval = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_DEVICE_ID:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.serialID = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_IBEACON_NAME:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.iBeaconName = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_CONNECTION_MODE:
                    if (resultCode == RESULT_OK) {
                        mBeaconParam.connectionMode = null;
                    }
                    break;
                case BeaconConstants.REQUEST_CODE_SET_PASSWORD:
                    if (resultCode == RESULT_OK) {
                        if (data != null && data.getExtras() != null) {
                            String password = data.getExtras().getString(BeaconConstants.EXTRA_KEY_DEVICE_PASSWORD);
                            mBeaconParam.password = password;
                            back();
                        }
                        return;
                    }
            }
            getEmptyInfo();
        }
    }

    private void getEmptyInfo() {
        final ArrayList<OrderTask> orderTasks = new ArrayList<>();

        if (TextUtils.isEmpty(mBeaconParam.battery)) {
            orderTasks.add(mBeaconService.getBattery());
        }
        if (TextUtils.isEmpty(mBeaconParam.uuid)) {
            orderTasks.add(mBeaconService.getIBeaconUuid());
        }
        if (TextUtils.isEmpty(mBeaconParam.major)) {
            orderTasks.add(mBeaconService.getMajor());
        }
        if (TextUtils.isEmpty(mBeaconParam.minor)) {
            orderTasks.add(mBeaconService.getMinor());
        }
        if (TextUtils.isEmpty(mBeaconParam.measurePower)) {
            orderTasks.add(mBeaconService.getMeasurePower());
        }
        if (TextUtils.isEmpty(mBeaconParam.transmission)) {
            orderTasks.add(mBeaconService.getTransmission());
        }
        if (TextUtils.isEmpty(mBeaconParam.broadcastingInterval)) {
            orderTasks.add(mBeaconService.getBroadcastingInterval());
        }
        if (TextUtils.isEmpty(mBeaconParam.serialID)) {
            orderTasks.add(mBeaconService.getSerialID());
        }
        if (TextUtils.isEmpty(mBeaconParam.iBeaconName)) {
            orderTasks.add(mBeaconService.getIBeaconName());
        }
        if (TextUtils.isEmpty(mBeaconParam.iBeaconMAC)) {
            orderTasks.add(mBeaconService.getIBeaconMac());
        }
        if (TextUtils.isEmpty(mBeaconParam.connectionMode)) {
            orderTasks.add(mBeaconService.getConnectionMode());
        }
        if (!orderTasks.isEmpty()) {
            if (!MokoSupport.getInstance().isBluetoothOpen()) {
                ToastUtils.showToast(this, "bluetooth is closed,please open");
                return;
            }
            showSyncProgressDialog("Syncing...");
            mBeaconService.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (OrderTask ordertask : orderTasks) {
                        mBeaconService.sendOrder(mBeaconService.setOvertime(), ordertask);
                    }
                }
            }, 1000);
        }
    }
}
