package com.hui.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
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

    private BluetoothAdapter bluetoothAdapter;
    private List<String> deviceNames;
    private Set<BluetoothDevice> devices;
    private BluetoothReceiver receiver;
    private ArrayAdapter<String> arrayAdapter;

    private ProgressBar progressBar;
    private String response;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket socket;
    private BluetoothDevice selectedDevice;
    private boolean isConnected = false; //用于标记是否连接成功
    private boolean isChoose = false; //用于标记是否在搜索完成前进行了选择

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

        btList = (ListView) findViewById(R.id.bt_list);
        dataLayout = (TableLayout) findViewById(R.id.data_layout);
        connectLayout = (FrameLayout) findViewById(R.id.connect_layout);

        findViewById(R.id.button_connect).setOnClickListener(this);
        findViewById(R.id.button_convey).setOnClickListener(this);
        findViewById(R.id.button_save).setOnClickListener(this);
        findViewById(R.id.button_exit).setOnClickListener(this);

        deviceNames = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNames);
        btList.setAdapter(arrayAdapter);

        /*添加连接蓝牙的监听器*/
        btList.setOnItemClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "蓝牙不可用", Toast.LENGTH_SHORT).show();
        }
        /*搜索蓝牙设备*/
        progressBar.setVisibility(View.VISIBLE);
        /*隐式打开蓝牙*/
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        scanBluetooth();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_connect:
                setTitle("正在请求连接...");
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

        /*首先检查是否有已经配对的设备*/
        devices = bluetoothAdapter.getBondedDevices();
        deviceNames.clear();
        if (devices.size() > 0) {
            for (BluetoothDevice device : devices) {
                deviceNames.add(device.getName());
            }
            arrayAdapter.notifyDataSetChanged();
        }

        receiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
    }


    private void convey() {
        if (!isConnected) {
            Toast.makeText(MainActivity.this, "无连接", Toast.LENGTH_SHORT).show();
            return;
        }
        sendForResult();
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
            Toast.makeText(MainActivity.this, "Save successfully", Toast.LENGTH_SHORT).show();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        isChoose = true;
         /*如果检测还在搜索设备，则取消*/
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (connectLayout.getVisibility() == View.VISIBLE) {
            connectLayout.setVisibility(View.GONE);
            dataLayout.setVisibility(View.VISIBLE);
            setTitle("待连接");
        }

        String name = deviceNames.get(position);
        for (BluetoothDevice device : devices) {
            if (device.getName().equals(name)) {
                selectedDevice = device;
                Toast.makeText(MainActivity.this, "选择了设备" + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*连接到指定的蓝牙设备*/
    private void connect(final BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(MainActivity.this, "请选择一个设备", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                   /*通过反射得到BluetoothSocket*/
                    // Method method = device.getClass().getMethod("createRfcommSocket",new Class[]{int.class});
                    //socket = (BluetoothSocket) method.invoke(device,1);
                    socket.connect();
                    isConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }/* catch (NoSuchMethodException e) {
                   e.printStackTrace();
               } catch (InvocationTargetException e) {
                   e.printStackTrace();
               } catch (IllegalAccessException e) {
                   e.printStackTrace();
               }*/
                if (isConnected) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTitle("已连接上" + device.getName());
                        }
                    });
                }
            }
        }).start();

    }

    /*向设备发送并接收返回数据*/
    private void sendForResult() {
        OutputStream os = null;
        InputStream is = null;
        try {
            os = socket.getOutputStream();
            os.write("010400000003B00B".getBytes("UTF-8"));
            is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                response += line;
            }
            //把字符数组表示的十六进制转换成double格式
            argR670 = parseHex(response, 6);
            argR690 = parseHex(response, 10);
            argR735 = parseHex(response, 14);
            argR808 = parseHex(response, 18);
            argHeight = parseHex(response, 26);
            argTobj = parseHex(response, 34);
            argTonv = parseHex(response, 38);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /*解析十六进制字符串返回double数值*/
    private double parseHex(String data, int i) {
        return Integer.parseInt(data.substring(i, i + 4), 16);
    }

    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED &&
                        (deviceNames.indexOf(device.getName()) == -1)) {   //设备没有匹配过,且之前没有被添加过，加入列表
                    try {
                        Method createBondDevice = BluetoothDevice.class.getMethod("createBond"); //进行配对
                        createBondDevice.invoke(device);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    deviceNames.add(device.getName());
                    arrayAdapter.notifyDataSetChanged();             //发生变化时，ListView显示搜索到的蓝牙设备
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.INVISIBLE);
                if (!isChoose) {
                    Toast.makeText(MainActivity.this, "搜索已经完成", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
}
