package com.moko.beaconsupport.task;

import com.moko.beaconsupport.callback.OrderTaskCallback;
import com.moko.beaconsupport.entity.OrderType;

/**
 * @Date 2017/12/14 0014
 * @Author wenzheng.liu
 * @Description
 * @ClassPath com.moko.beaconsupport.task.IBeaconDateTask
 */
public class IBeaconDateTask extends OrderTask {

    public byte[] data;

    public IBeaconDateTask(OrderTaskCallback callback, int sendDataType) {
        super(OrderType.iBeaconDate, callback, sendDataType);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
