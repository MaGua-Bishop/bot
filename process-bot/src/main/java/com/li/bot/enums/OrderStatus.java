package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public enum OrderStatus {

//    -1 审核失败 0 待审核 1待处理 2处理中 3已完成

    Cancel(-2,"已取消"),

    Review_FAILED(-1,"审核失败"),

     REVIEW(0,"待审核"),

    PENDING(1, "待处理"),
    IN_PROGRESS(2, "处理中"),
    COMPLETED(3, "已完成");

    private final Integer code;
    private final String message;

    OrderStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static String getMessageByCode(Integer code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode().equals(code)) {
                return status.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getMessage().equals(message)) {
                return status.getCode();
            }
        }
        return null;
    }



}
