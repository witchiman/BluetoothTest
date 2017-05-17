# BluetoothTest

该应用通过蓝牙4.0连接到指定的激光传感器收集数据到手机并进行处理。

* 基于Android API 23开发

* 通过BluetoothLeScanner实现了对周围的低功耗蓝牙设备的扫描和基于使用设定的UUID进行连接的功能。

* 通过发送相关指令，借助获取的BluetoothGattService可以进行通信，获取到传感器收集的数据。

* 在本地可对收集的数据进行筛选和进一步处理或保存。
