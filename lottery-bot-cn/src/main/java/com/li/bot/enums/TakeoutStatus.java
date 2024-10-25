package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
public enum TakeoutStatus {

    ERROR(-1,"提现失败"),
    START(0,"待审核"),

    END(1,"提现成功");

    private final Integer code;
    private final String message;

    TakeoutStatus(Integer code, String message) {
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
        for (TakeoutStatus type : TakeoutStatus.values()) {
            if (type.getCode().equals(code)) {
                return type.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (TakeoutStatus type : TakeoutStatus.values()) {
            if (type.getMessage().equals(message)) {
                return type.getCode();
            }
        }
        return null;
    }

}
