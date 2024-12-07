package com.mhc.springbootclouddisk.common.response;

import com.mhc.springbootclouddisk.common.constants.HttpStatus;

import java.io.Serial;
import java.util.HashMap;

public class CloudDiskResult extends HashMap<String, Object> {
    @Serial
    private static final long serialVersionUID = 1L;
    public static final String STATUS_TAG = "status";
    public static final String CODE_TAG = "code";
    public static final String INFO_TAG = "info";
    public static final String DATA_TAG = "data";

    public CloudDiskResult() {
    }

    public CloudDiskResult(String status, int code, String info, Object data) {
        super.put(STATUS_TAG, status);
        super.put(CODE_TAG, code);
        super.put(INFO_TAG, info);
        super.put(DATA_TAG, data);
    }

    public static CloudDiskResult success() {
        return CloudDiskResult.success("success", HttpStatus.SUCCESS, "请求成功");
    }

    public static CloudDiskResult success(String status, int code, String info) {
        return CloudDiskResult.success(status, code, info, null);
    }

    public static CloudDiskResult success(Object data) {
        return CloudDiskResult.success("success", HttpStatus.SUCCESS, "请求成功", data);
    }

    public static CloudDiskResult success(String status, int code, String info, Object data) {
        return new CloudDiskResult(status, code, info, data);
    }

    @Override
    public CloudDiskResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
