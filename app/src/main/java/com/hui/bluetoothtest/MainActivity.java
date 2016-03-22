package com.hui.bluetoothtest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final int REQUEST_NDVI = 0;
    public static final int REQUEST_LAI = 1;
    public static final int REQUEST_BIO = 2;
    public static final int REQUEST_CHL = 3;
    public static final int REQUEST_N = 4;
    public static final int REQUEST_HEIGHT = 5;
    public static final int REQUEST_TOBJ = 6;
    public static final int REQUEST_TENV = 7;

    private double argR670;
    private double argR690;
    private double argR735;
    private double argR808;
    private double argHeight;
    private double argTobj;
    private double argTonv;
    private String dateText;
    private double resultNdvi;
    private double resultLAI;
    private double resultBio;
    private double resultCHL;
    private double resultN;
    private double resultHeight;
    private double resultTobj;
    private double resultTenv;


    private EditText etDate;
    private EditText etNdvi;
    private EditText etLai;
    private EditText etBiomass;
    private EditText etChl;
    private EditText etN;
    private EditText etHeight;
    private EditText etTobj;
    private EditText etTenv;

    private ListView btList;
    private FrameLayout connectLayout;
    private TableLayout dataLayout;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private List<BluetoothDevice> devices;
    private DeviceAdapter deviceAdapter;
    private BluetoothLeScanner scanner;

    private ProgressBar progressBar;
    private String response = "";

    //private static final UUID DATA_UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    private static final UUID DATA_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");


    private BluetoothDevice selectedDevice;
    private boolean isConnected = false; //用于标记是否连接成功
    private Handler handler;
    private ScanCallback leCallback;
    private boolean isScanning;
    private BluetoothGattCharacteristic characteristic;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        setTitle("搜索到的设备");

        etDate = (EditText) findViewById(R.id.et_date);
        etNdvi = (EditText) findViewById(R.id.et_ndvi);
        etLai = (EditText) findViewById(R.id.et_lai);
        etBiomass = (EditText) findViewById(R.id.et_biomass);
        etChl = (EditText) findViewById(R.id.et_chl);
        etN = (EditText) findViewById(R.id.et_n);
        etHeight = (EditText) findViewById(R.id.et_height);
        etTobj = (EditText) findViewById(R.id.et_tobj);
        etTenv = (EditText) findViewById(R.id.et_tenv);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        handler = new Handler();

        btList = (ListView) findViewById(R.id.bt_list);
        dataLayout = (TableLayout) findViewById(R.id.data_layout);
        connectLayout = (FrameLayout) findViewById(R.id.connect_layout);

        findViewById(R.id.button_connect).setOnClickListener(this);
        findViewById(R.id.button_convey).setOnClickListener(this);
        findViewById(R.id.button_save).setOnClickListener(this);
        findViewById(R.id.button_exit).setOnClickListener(this);

        devices = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this,R.layout.list_device,devices);
        btList.setAdapter(deviceAdapter);

        /*添加蓝牙列表的监听器*/
        btList.setOnItemClickListener(this);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,"该设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
        }
        /*隐式打开蓝牙*/
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            leCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        BluetoothDevice device = result.getDevice();
                        System.out.println("the name of device is :" + device.getName() + "|" + device.getAddress());
                        if (!devices.contains(device)) {
                            devices.add(device);
                        }
                        deviceAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    System.out.println("搜索失败");
                }
            };
        }
        /*搜索蓝牙设备*/
        progressBar.setVisibility(View.VISIBLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanBluetooth();
            }
        },1000);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_connect:
                connect(selectedDevice);
                break;
            case R.id.button_convey:
                convey();
                break;
            case R.id.button_save:
                save();
                break;
            case R.id.button_exit:
                finish();
                break;
            default:
                break;
        }
    }

    /*通过公式计算各个参数，返回结果*/
    private double compute(int requestCode) {
        switch (requestCode) {
            case REQUEST_NDVI:
                return ((argR808 - argR670) / (argR808 + argR670));
            case REQUEST_LAI:
                return 0.202 * Math.exp(4.535 * ((argR808 - argR670) / Math.sqrt(argR808 - argR670)));
            case REQUEST_BIO:
                return 54.81 * resultLAI + 42.39;
            case REQUEST_CHL:
                return -668.319 + 12.652 * argR808 / argR670 - 5675.885 * (argR735 / argR690 - 1) +
                        2316.909 * (argR735 - argR690) / (argR690 - argR670);
            case REQUEST_N:
                return 242.2 * Math.pow(resultCHL, 0.457);
            case REQUEST_HEIGHT:
                double x = argHeight * 2.5 / 4096;
                return -36.64 * Math.pow(x, 3) + 207.71 * Math.pow(x, 2) - 376.1 * x + 272.56;
            case REQUEST_TOBJ:
                return argTobj / 1000;
            case REQUEST_TENV:
                return argTonv / 1000;
            default:
                break;
        }
        return 0;
    }

    /*对蓝牙设备进行扫描*/
    private void scanBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothAdapter.isEnabled()&&isScanning) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            isScanning = false;
                            scanner.stopScan(leCallback);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "搜索完成", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.INVISIBLE);
                                    System.out.println("The size of devices : " + devices.size());
                                }
                            });
                        }
                    }
                }
            }, 15000);
            isScanning = true;
            scanner.startScan(leCallback);

        }
    }


    private void convey() {
        if (!isConnected) {
            Toast.makeText(MainActivity.this, "无连接", Toast.LENGTH_SHORT).show();
            return;
        }
        sendForResult("010400000003B00B");
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yy年MM月dd日 HH:mm:ss");
        dateText = format.format(date);
        etDate.setText(dateText);
        resultNdvi = compute(REQUEST_NDVI);
        resultLAI = compute(REQUEST_LAI);
        resultBio = compute(REQUEST_BIO);
        resultCHL = compute(REQUEST_CHL);
        resultN = compute(REQUEST_N);
        resultHeight = compute(REQUEST_HEIGHT);
        resultTobj = compute(REQUEST_TOBJ);
        resultTenv = compute(REQUEST_TENV);

        /*计算后得到的结果显示到界面上*/
        etDate.setText(dateText);
        etNdvi.setText(resultNdvi + "");
        etLai.setText(resultLAI + "");
        etBiomass.setText(resultBio + "");
        etChl.setText(resultCHL + "");
        etN.setText(resultN + "");
        etHeight.setText(resultHeight + "");
        etTobj.setText(resultTobj + "");
        etTenv.setText(resultTenv + "");
    }

    public void save() {
        if (TextUtils.isEmpty(response)) {
            Toast.makeText(MainActivity.this, "无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory(), "data_collection.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("-----------------------------" + "\n");
            bw.write("日期:" + dateText + "\n");
            bw.write("Ndvi:" + resultNdvi + "\n");
            bw.write("LAI:" + resultLAI + "\n");
            bw.write("Biomass:" + resultBio + "\n");
            bw.write("CHL:" + resultCHL + "\n");
            bw.write("N含量:" + resultN + "\n");
            bw.write("冠层高度:" + resultHeight + "\n");
            bw.write("目标温度:" + resultTobj + "\n");
            bw.write("环境温度:" + resultTenv + "\n");
            bw.flush();
            Toast.makeText(getApplicationContext(), "Save successfully", Toast.LENGTH_SHORT).show();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothAdapter!=null && bluetoothGatt!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                bluetoothGatt.disconnect();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isScanning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scanner.stopScan(leCallback);
            }
            isScanning = false;
        }
        if (connectLayout.getVisibility() == View.VISIBLE) {
            connectLayout.setVisibility(View.GONE);
            dataLayout.setVisibility(View.VISIBLE);
            setTitle("待连接");
        }

        selectedDevice = devices.get(position);
        if (selectedDevice != null) {
            if (!TextUtils.isEmpty(selectedDevice.getName())) {
                Toast.makeText(MainActivity.this, "选择了" + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "选择了 Unknown Device", Toast.LENGTH_SHORT).show();
            }
        }

    }

    /*连接到指定的蓝牙设备*/
    private void connect(final BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(MainActivity.this, "请选择一个设备", Toast.LENGTH_SHORT).show();
            return;
        }
        setTitle("正在请求连接...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothGatt = device.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        isConnected = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isConnected) {
                                    if (!TextUtils.isEmpty(device.getName())) {
                                        setTitle("已连接上" + device.getName());
                                    }else {
                                        setTitle("已经连接上Unknown Device");
                                    }
                                }
                            }
                        });
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            gatt.discoverServices();
                        }
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            System.out.println("The size of service is :" +bluetoothGatt.getServices().size());
                            for (BluetoothGattService service:gatt.getServices()){
                                int type = service.getType();
                                System.out.println("-->service type:"+UtilOnStr.getServiceType(type));
                                System.out.println("-->included service size : " + service.getIncludedServices().size());
                                System.out.println("-->service uuid : " + service.getUuid());

                                List<BluetoothGattCharacteristic> gattCharacteristics =service.getCharacteristics();
                                for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                                    System.out.println("---->char uuid:" + gattCharacteristic.getUuid());

                                    int permission = gattCharacteristic.getPermissions();
                                    System.out.println("---->char permission:" + UtilOnStr.getCharPermission(permission));

                                    int property = gattCharacteristic.getProperties();
                                    System.out.println("---->char property:" + UtilOnStr.getCharPropertie(property));

                                    byte[] data = gattCharacteristic.getValue();
                                    if (data != null && data.length > 0) {
                                        System.out.println("---->char value:" + new String(data));
                                    }

                                    //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
                                    if(gattCharacteristic.getUuid().toString().equals(DATA_UUID)) {
                                        //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                bluetoothGatt.readCharacteristic(gattCharacteristic);
                                            }
                                        }, 500);
                                    }

                                    //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                                    bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                                    //设置数据内容
                                    gattCharacteristic.setValue("send data->\"BlaBlaBla~~~~~~~~~~\"");
                                    //往蓝牙模块写入数据
                                    bluetoothGatt.writeCharacteristic(gattCharacteristic);

                                    //-----Descriptors的字段信息-----//
                                    List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                                    for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                                        System.out.println("-------->desc uuid:" + gattDescriptor.getUuid());
                                        int descPermission = gattDescriptor.getPermissions();
                                        System.out.println("-------->desc permission:" + UtilOnStr.getDescPermission(descPermission));

                                        byte[] desData = gattDescriptor.getValue();
                                        if (desData != null && desData.length > 0) {
                                            System.out.println("-------->desc value:" + new String(desData));
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            response = UtilOnStr.bytesToHexString(characteristic.getValue());
                            System.out.println(gatt.getDevice().getName() + " read" + characteristic.getUuid().toString() +
                                    " -> " + response);
                        }
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        System.out.println(gatt.getDevice().getName() + " write "+
                        " -> " + new String(characteristic.getValue()));
                    }
                }
            });
        }
    }

    /*向设备发送并接收返回数据*/
    private void sendForResult(String request) {
        BluetoothGattService service = null;
        boolean status = false;

        /*写入数据*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            service = bluetoothGatt.getService(UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb"));
            if (service!=null) {
                characteristic = service.getCharacteristic(UUID.fromString("8082caa8-41a6-4021-91c6-56f9b954cc34"));
                if (characteristic !=null) {
                    bluetoothGatt.setCharacteristicNotification(characteristic,true);  //设置characteristic的通知，触发bluetoothGatt.onCharacteristicWrite()事件。
                    characteristic.setValue(request.getBytes());
                    status = bluetoothGatt.writeCharacteristic(characteristic);
                    if (status) {
                        System.out.println("写入成功");
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                bluetoothGatt.readCharacteristic(characteristic);
                            }
                        }
                    },500);
                } else {
                    System.out.println("charateristic is null");
                }
            } else {
                System.out.println("service is null");
            }
        }


        //把字符数组表示的十六进制转换成double格式
           if (!TextUtils.isEmpty(response)) {
               argR735 = UtilOnStr.parseHex(response, 6)/2750;
               argR690 = UtilOnStr.parseHex(response, 10)/2750;
               argR670 = UtilOnStr.parseHex(response, 14)/2600;
               argR808 = UtilOnStr.parseHex(response, 18)/2600;
               argHeight = UtilOnStr.parseHex(response, 26);
               argTobj = UtilOnStr.parseHex(response, 34);
               argTonv = UtilOnStr.parseHex(response, 38);
           }
    }

}
