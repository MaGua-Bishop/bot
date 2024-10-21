package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
public enum LotteryStatus {

    START(0,"start"),
    END(1,"end");

    private final Integer code;
    private final String message;

    LotteryStatus(Integer code, String message) {
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
        for (LotteryStatus type : LotteryStatus.values()) {
            if (type.getCode().equals(code)) {
                return type.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (LotteryStatus type : LotteryStatus.values()) {
            if (type.getMessage().equals(message)) {
                return type.getCode();
            }
        }
        return null;
    }

}
