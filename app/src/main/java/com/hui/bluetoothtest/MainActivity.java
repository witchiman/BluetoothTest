package com.hui.bluetoothtest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.os.Message;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
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
    public static final int OPEN_SUCCESS = 8;
    private static final String TAG = "MainActivity";

    private double argR670;
    private double argR690;
    private double argR735;
    private double argR808;
    private double argHeight;
    private double argTobj;
    private double argTonv;
    private String dateText;
    private String result1610;
    private String result1550;
    private String result1510;
    private String result1450;
    private String result1270;
    private String result1310;
    private String result1350;
    private String result1410;


    private EditText etDate;
    private EditText et1610;
    private EditText et1550;
    private EditText et1510;
    private EditText et1450;
    private EditText et1270;
    private EditText et1310;
    private EditText et1350;
    private EditText et1410;

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

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final String request = "010400000003b00b";


    private BluetoothDevice selectedDevice;
    private boolean isConnected = false; //用于标记是否连接成功
    private Handler delayedHandler;     //用于延时操作
    private ScanCallback leCallback;
    private boolean isScanning;
    private BluetoothGattCharacteristic characteristic;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        setTitle("搜索到的设备");

        etDate = (EditText) findViewById(R.id.et_date);  //layout的xml懒得改了
        et1270 = (EditText) findViewById(R.id.et_ndvi);
        et1310 = (EditText) findViewById(R.id.et_lai);
        et1350 = (EditText) findViewById(R.id.et_biomass);
        et1410 = (EditText) findViewById(R.id.et_chl);
        et1450 = (EditText) findViewById(R.id.et_n);
        et1510 = (EditText) findViewById(R.id.et_height);
        et1550 = (EditText) findViewById(R.id.et_tobj);
        et1610 = (EditText) findViewById(R.id.et_tenv);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        delayedHandler = new Handler();

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {//循环判断蓝牙是否开启，耗时操作放到子线程里
                   if (bluetoothAdapter.isEnabled()) {
                       Message msg = Message.obtain();
                       msg.what = OPEN_SUCCESS;
                       openLEHandler.sendMessage(msg);
                       break;
                   }
                }
            }
        }).start();

    }

    private Handler openLEHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OPEN_SUCCESS:
                    scanBluetooth();         //蓝牙开启成功，开始搜索周围的设备
                    progressBar.setVisibility(View.VISIBLE);
                    //Log.d(TAG, "handleMessage: 开始搜索");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

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
    private String compute(int requestCode) {
        DecimalFormat df = new DecimalFormat("0.###");
        switch (requestCode) {
            case REQUEST_NDVI:
                double value_ndvi = ((argR808 - argR670) / (argR808 + argR670));
                return df.format(value_ndvi);
            case REQUEST_LAI:
                double value_lai = 0.202 * Math.exp(4.535 * ((argR808 - argR670) /
                        Math.sqrt(argR808 - argR670)));
                return df.format(value_lai) ;
            case REQUEST_BIO:
                double value_bio = 54.81 * (0.202 * Math.exp(4.535 * ((argR808 - argR670) /
                        Math.sqrt(argR808 - argR670)))) + 42.39;
                return df.format(value_bio);
            case REQUEST_CHL:
                double value_chl = -668.319 + 12.652 * argR808 / argR670 - 5675.885 *
                        (argR735 / argR690 - 1) + 2316.909 * (argR735 - argR690) / (argR690 - argR670);
                return df.format(value_chl);
            case REQUEST_N:
                double value_n = 242.2 * Math.pow(-668.319 + 12.652 * argR808 / argR670 - 5675.885 *
                        (argR735 / argR690 - 1) + 2316.909 * (argR735 - argR690) /
                        (argR690 - argR670), 0.457);
                if (Double.isNaN(value_n)) {
                    return "0.000";
                }
                return df.format(value_n);
            case REQUEST_HEIGHT:
                double x = argHeight * 2.5 / 4096;
                double value_h = -36.64 * Math.pow(x, 3) + 207.71 * Math.pow(x, 2) - 376.1 * x + 272.56;
                return df.format(value_h);
            case REQUEST_TOBJ:
                double value_tobj = argTobj / 1000;
                return df.format(value_tobj);
            case REQUEST_TENV:
                double value_tenv = argTonv / 1000;
                return df.format(value_tenv);
            default:
                break;
        }
        return null;
    }

    /*对蓝牙设备进行扫描*/
    private void scanBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            delayedHandler.postDelayed(new Runnable() {
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
        sendForResult(request);

    }

    public void save() {
        if (TextUtils.isEmpty(response)) {
            Toast.makeText(MainActivity.this, "无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory(), "soil data.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("-----------------------------" + "\n");
            bw.write("日期:" + dateText + "\n");
            bw.write("1270:" + result1270 + "\n");
            bw.write("1310:" + result1310 + "\n");
            bw.write("1350:" + result1350 + "\n");
            bw.write("1410:" + result1410 + "\n");
            bw.write("1450:" + result1450 + "\n");
            bw.write("1510:" + result1510 + "\n");
            bw.write("1550:" + result1550 + "\n");
            bw.write("1610:" + result1610 + "\n");
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
                private boolean isOver = false;
                private String tempResponse = "";

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
                                    } else {
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
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        System.out.println(gatt.getDevice().getName() + " write " +
                                " -> " + new String(characteristic.getValue()));
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        String value = UtilOnStr.parseBytesToHexString(characteristic.getValue());
                        System.out.println(gatt.getDevice().getName() + " read " + " -> " + value);
                    }
                }

               /* @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    String TAG="MainActivity";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        for (BluetoothGattService gattService : gatt.getServices()) {
                            //-----Service的字段信息-----//
                            int type = gattService.getType();
                            Log.e(TAG,"-->service type:"+UtilOnStr.getServiceType(type));
                            Log.e(TAG,"-->includedServices size:"+gattService.getIncludedServices().size());
                            Log.e(TAG,"-->service uuid:"+gattService.getUuid());

                            //-----Characteristics的字段信息-----//
                            List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
                            for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                                Log.e(TAG, "---->char uuid:" + gattCharacteristic.getUuid());

                                int permission = gattCharacteristic.getPermissions();
                                Log.e(TAG, "---->char permission:" + UtilOnStr.getCharPermission(permission));

                                int property = gattCharacteristic.getProperties();
                                Log.e(TAG, "---->char property:" + UtilOnStr.getCharPropertie(property));

                                byte[] data = gattCharacteristic.getValue();
                                if (data != null && data.length > 0) {
                                    Log.e(TAG, "---->char value:" + new String(data));
                                }


                                //-----Descriptors的字段信息-----//
                                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                                    Log.e(TAG, "-------->desc uuid:" + gattDescriptor.getUuid());
                                    int descPermission = gattDescriptor.getPermissions();
                                    Log.e(TAG, "-------->desc permission:" + UtilOnStr.getDescPermission(descPermission));

                                    byte[] desData = gattDescriptor.getValue();
                                    if (desData != null && desData.length > 0) {
                                        Log.e(TAG, "-------->desc value:" + new String(desData));
                                    }
                                }

                            }
                        }
                    }
                }*/

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        String temp = UtilOnStr.parseBytesToHexString(characteristic.getValue());
                        tempResponse = tempResponse + temp;

                        if (!isOver) {  //从传感器收集的数据分两次传输，判断是否传输完
                            isOver = true;
                            return;
                        }
                        response = tempResponse;
                        System.out.println(gatt.getDevice().getName() + " changed " + " -> " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showData();
                                tempResponse = "";

                            }
                        });
                        isOver = false;

                    }
                    ;

                }
            });
        }
    }

    /*向设备发送并接收返回数据*/
    private void sendForResult(String value) {
        BluetoothGattService service = null;
        boolean status = false;

        /*写入数据*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            service = bluetoothGatt.getService(SERVICE_UUID);
            if (service!=null) {
                characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic !=null) {
                    bluetoothGatt.setCharacteristicNotification(characteristic, true);  //设置characteristic的通知，触发bluetoothGatt.onCharacteristicWrite()事件。
                    characteristic.setValue(UtilOnStr.parseHexStringToBytes(value));
                    status = bluetoothGatt.writeCharacteristic(characteristic);
                    if (status) {
                        System.out.println("characteristic写入成功");
                    }
                    /*BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    boolean b = descriptor.setValue("010400000003b00b".getBytes());
                    if (b) {
                        System.out.println("descriptor 写入成功");
                    }*/

                    delayedHandler.postDelayed(new Runnable() {
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

    }

    private void showData() {
        //把字符数组表示的十六进制转换成double格式
        if (!TextUtils.isEmpty(response)) {
           /* argR735 = UtilOnStr.parseHex(response, 6)/2750;
            argR690 = UtilOnStr.parseHex(response, 10)/2750;
            argR670 = UtilOnStr.parseHex(response, 14)/2600;
            argR808 = UtilOnStr.parseHex(response, 18)/2600;
            argHeight = UtilOnStr.parseHex(response, 26);
            argTobj = UtilOnStr.parseHex(response, 34);
            argTonv = UtilOnStr.parseHex(response, 38);*/

            /*显示改需求后的返回数据,ADC*/
            result1610 = convertADCToVoltage(UtilOnStr.parseHex(response, 6));
            result1550 = convertADCToVoltage(UtilOnStr.parseHex(response, 10));
            result1510 = convertADCToVoltage(UtilOnStr.parseHex(response, 14));
            result1450 = convertADCToVoltage(UtilOnStr.parseHex(response, 18));
            result1270 = convertADCToVoltage(UtilOnStr.parseHex(response, 22));
            result1310 = convertADCToVoltage(UtilOnStr.parseHex(response, 26));
            result1350  = convertADCToVoltage(UtilOnStr.parseHex(response, 30));
            result1410 = convertADCToVoltage(UtilOnStr.parseHex(response, 34));

        }
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yy年MM月dd日 HH:mm:ss");
        dateText = format.format(date);

           /*resultNdvi = compute(REQUEST_NDVI);
        resultLAI = compute(REQUEST_LAI);
        resultBio = compute(REQUEST_BIO);
        resultCHL = compute(REQUEST_CHL);
        resultN = compute(REQUEST_N);
        resultHeight = compute(REQUEST_HEIGHT);
        resultTobj = compute(REQUEST_TOBJ);
        resultTenv = compute(REQUEST_TENV);*/
        /*
        *//*计算后得到的结果显示到界面上*//*
        etDate.setText(dateText);
        etNdvi.setText(resultNdvi + "");
        etLai.setText(resultLAI + "");
        etBiomass.setText(resultBio + "");
        etChl.setText(resultCHL + "");
        etN.setText(resultN + "");
        etHeight.setText(resultHeight + "");
        etTobj.setText(resultTobj + "");
        etTenv.setText(resultTenv + "");*/

        etDate.setText(dateText);
        et1270.setText(result1270);
        et1310.setText(result1310);
        et1350.setText(result1350);
        et1410.setText(result1410);
        et1450.setText(result1450);
        et1510.setText(result1510);
        et1550.setText(result1550);
        et1610.setText(result1610);


    }

    /**
     *
     * @param adc 通过返回的信息截取得到ADC
     * @return   换算成电压值返回,保留三位小数
     */
    private String convertADCToVoltage(double adc) {
        double temp = adc*2.5/4096;
        BigDecimal b = new BigDecimal(temp);
        double d = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue(); //设置保留三位小数
        String result = String.valueOf(d);
        return result;
    }

}
