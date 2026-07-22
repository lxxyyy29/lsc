package com.changping.platform.modules.drone.exception;

import com.changping.platform.common.exception.BusinessException;

/**
 * @Author tangxinglin
 * @Description //无人机API调用异常，继承BusinessException，统一使用DRONE_API_ERROR错误码封装上游平台调用失败信息
 * @Date 2026/04/18 10:00
 */
public class DroneApiException extends BusinessException {

    /**
     * @Author tangxinglin
     * @Description //使用默认错误码DRONE_API_ERROR和自定义消息构造异常
     * @Date 2026/04/18 10:00
     * @Param [message 错误消息]
     * @return void
     */
    public DroneApiException(String message) {
        super("DRONE_API_ERROR", message);
    }

    /**
     * @Author tangxinglin
     * @Description //使用自定义错误码和消息构造异常
     * @Date 2026/04/18 10:00
     * @Param [code 错误码, message 错误消息]
     * @return void
     */
    public DroneApiException(String code, String message) {
        super(code, message);
    }
}
