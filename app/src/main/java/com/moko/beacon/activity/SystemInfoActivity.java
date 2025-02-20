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
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.moko.beacon.BeaconConstants;
import com.moko.beacon.R;
import com.moko.beacon.entity.BeaconDeviceInfo;
import com.moko.beacon.service.MokoService;
import com.moko.beacon.utils.ToastUtils;
import com.moko.support.MokoConstants;
import com.moko.support.MokoSupport;
import com.moko.support.entity.OrderType;
import com.moko.support.task.OrderTask;
import com.moko.support.utils.MokoUtils;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @Date 2017/12/15 0015
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.beacon.activity.SystemInfoActivity
 */
public class SystemInfoActivity extends BaseActivity {
    @Bind(R.id.tv_ibeacon_soft_version)
    TextView tvIbeaconSoftVersion;
    @Bind(R.id.tv_ibeacon_firmname)
    TextView tvIbeaconFirmname;
    @Bind(R.id.tv_ibeacon_device_name)
    TextView tvIbeaconDeviceName;
    @Bind(R.id.tv_ibeacon_date)
    TextView tvIbeaconDate;
    @Bind(R.id.tv_ibeacon_mac)
    TextView tvIbeaconMac;
    @Bind(R.id.tv_ibeacon_chip_mode)
    TextView tvIbeaconChipMode;
    @Bind(R.id.tv_ibeacon_hardware_version)
    TextView tvIbeaconHardwareVersion;
    @Bind(R.id.tv_ibeacon_firmware_version)
    TextView tvIbeaconFirmwareVersion;
    @Bind(R.id.tv_ibeacon_runtime)
    TextView tvIbeaconRuntime;
    private MokoService mMokoService;
    private BeaconDeviceInfo mBeaconDeviceInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_info);
        ButterKnife.bind(this);
        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
        mBeaconDeviceInfo = (BeaconDeviceInfo) getIntent().getSerializableExtra(BeaconConstants.EXTRA_KEY_DEVICE_INFO);
        if (mBeaconDeviceInfo == null) {
            finish();
            return;
        }
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        if (TextUtils.isEmpty(mBeaconDeviceInfo.softVersion)) {
            orderTasks.add(mMokoService.getSoftVersion());
        } else {
            tvIbeaconSoftVersion.setText(mBeaconDeviceInfo.softVersion);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.firmname)) {
            orderTasks.add(mMokoService.getFirmname());
        } else {
            tvIbeaconFirmname.setText(mBeaconDeviceInfo.firmname);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.deviceName)) {
            orderTasks.add(mMokoService.getDevicename());
        } else {
            tvIbeaconDeviceName.setText(mBeaconDeviceInfo.deviceName);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.iBeaconDate)) {
            orderTasks.add(mMokoService.getiBeaconDate());
        } else {
            tvIbeaconDate.setText(mBeaconDeviceInfo.iBeaconDate);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.iBeaconMac)) {
            orderTasks.add(mMokoService.getIBeaconMac());
        } else {
            tvIbeaconMac.setText(mBeaconDeviceInfo.iBeaconMac);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.chipModel)) {
            orderTasks.add(mMokoService.getChipModel());
        } else {
            tvIbeaconChipMode.setText(mBeaconDeviceInfo.chipModel);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.hardwareVersion)) {
            orderTasks.add(mMokoService.getChipModel());
        } else {
            tvIbeaconHardwareVersion.setText(mBeaconDeviceInfo.hardwareVersion);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.firmwareVersion)) {
            orderTasks.add(mMokoService.getChipModel());
        } else {
            tvIbeaconFirmwareVersion.setText(mBeaconDeviceInfo.firmwareVersion);
        }
        if (TextUtils.isEmpty(mBeaconDeviceInfo.runtime)) {
            orderTasks.add(mMokoService.getChipModel());
        } else {
            tvIbeaconRuntime.setText(mBeaconDeviceInfo.runtime);
        }
        if (!orderTasks.isEmpty()) {
            if (!MokoSupport.getInstance().isBluetoothOpen()) {
                ToastUtils.showToast(this, "bluetooth is closed,please open");
                return;
            }
            showLoadingProgressDialog();
            for (OrderTask ordertask : orderTasks) {
                mMokoService.sendOrder(ordertask);
            }
        }
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
                String action = intent.getAction();
                if (MokoConstants.ACTION_CONNECT_DISCONNECTED.equals(action)) {
                    ToastUtils.showToast(SystemInfoActivity.this, getString(R.string.alert_diconnected));
                    finish();
                }
                if (MokoConstants.ACTION_RESPONSE_FINISH.equals(action)) {
                    mMokoService.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dismissLoadingProgressDialog();
                        }
                    }, 1000);

                }
                if (MokoConstants.ACTION_RESPONSE_SUCCESS.equals(action)) {
                    OrderType orderType = (OrderType) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TYPE);
                    byte[] value = intent.getByteArrayExtra(MokoConstants.EXTRA_KEY_RESPONSE_VALUE);
                    switch (orderType) {
                        case iBeaconMac:
                            String hexMac = MokoUtils.bytesToHexString(value);
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
                                mBeaconDeviceInfo.iBeaconMac = mac;
                                tvIbeaconMac.setText(mBeaconDeviceInfo.iBeaconMac);
                            }
                            break;
                        case firmname:
                            mBeaconDeviceInfo.firmname = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconFirmname.setText(mBeaconDeviceInfo.firmname);
                            break;
                        case softVersion:
                            mBeaconDeviceInfo.softVersion = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconSoftVersion.setText(mBeaconDeviceInfo.softVersion);
                            break;
                        case devicename:
                            mBeaconDeviceInfo.deviceName = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconDeviceName.setText(mBeaconDeviceInfo.deviceName);
                            break;
                        case iBeaconDate:
                            mBeaconDeviceInfo.iBeaconDate = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconDate.setText(mBeaconDeviceInfo.iBeaconDate);
                            break;
                        case hardwareVersion:
                            mBeaconDeviceInfo.hardwareVersion = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconHardwareVersion.setText(mBeaconDeviceInfo.hardwareVersion);
                            break;
                        case firmwareVersion:
                            mBeaconDeviceInfo.firmwareVersion = MokoUtils.hex2String(MokoUtils.bytesToHexString(value));
                            tvIbeaconFirmwareVersion.setText(mBeaconDeviceInfo.firmwareVersion);
                            break;
                        case writeAndNotify:
                            if ("eb59".equals(MokoUtils.bytesToHexString(Arrays.copyOfRange(value, 0, 2)).toLowerCase())) {
                                byte[] runtimeBytes = Arrays.copyOfRange(value, 4, value.length);
                                int seconds = Integer.parseInt(MokoUtils.bytesToHexString(runtimeBytes), 16);
                                int day = 0, hours = 0, minutes = 0;
                                day = seconds / (60 * 60 * 24);
                                seconds -= day * 60 * 60 * 24;
                                hours = seconds / (60 * 60);
                                seconds -= hours * 60 * 60;
                                minutes = seconds / 60;
                                seconds -= minutes * 60;
                                mBeaconDeviceInfo.runtime = String.format("%dD%dh%dm%ds", day, hours, minutes, seconds);
                                tvIbeaconRuntime.setText(mBeaconDeviceInfo.runtime);
                            }
                            if ("eb5b".equals(MokoUtils.bytesToHexString(Arrays.copyOfRange(value, 0, 2)).toLowerCase())) {
                                byte[] chipModelBytes = Arrays.copyOfRange(value, 4, value.length);
                                mBeaconDeviceInfo.chipModel = MokoUtils.hex2String(MokoUtils.bytesToHexString(chipModelBytes));
                                tvIbeaconChipMode.setText(mBeaconDeviceInfo.chipModel);
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
            mMokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONNECT_DISCONNECTED);
            filter.setPriority(300);
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

    @OnClick({R.id.tv_back})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_back:
                back();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void back() {
        Intent intent = new Intent();
        intent.putExtra(BeaconConstants.EXTRA_KEY_DEVICE_INFO, mBeaconDeviceInfo);
        setResult(RESULT_OK);
        finish();
    }

    private ProgressDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new ProgressDialog(SystemInfoActivity.this);
        mLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mLoadingDialog.setMessage("Syncing...");
        if (!isFinishing() && mLoadingDialog != null && !mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    private void dismissLoadingProgressDialog() {
        if (!isFinishing() && mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }
}
