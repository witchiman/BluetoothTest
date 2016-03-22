package com.hui.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by HUI on 2016/3/21.
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private int resourceId;

    public DeviceAdapter(Context context, int resource, List<BluetoothDevice> objects) {
        super(context, resource, objects);
        this.resourceId = resource;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        DeviceHolder deviceHolder;
        BluetoothDevice device = getItem(position);
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);
            deviceHolder = new DeviceHolder();
            deviceHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            deviceHolder.deviceAddress = (TextView) view.findViewById(R.id.deivce_address);
            view.setTag(deviceHolder);
        } else {
            view = convertView;
            deviceHolder = (DeviceHolder) view.getTag();
        }
        if (device.getName()!=null) {
            deviceHolder.deviceName.setText(device.getName());
        }else {
            deviceHolder.deviceName.setText("Unknown Device");
        }
        deviceHolder.deviceAddress.setText(device.getAddress());
        return view;
    }

    private class DeviceHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
