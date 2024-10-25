package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-18
 */
public enum LotteryInfoStatus {

    START(0,"未核销"),
    END(1,"已核销");

    private final Integer code;
    private final String message;

    LotteryInfoStatus(Integer code, String message) {
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
        for (LotteryInfoStatus type : LotteryInfoStatus.values()) {
            if (type.getCode().equals(code)) {
                return type.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (LotteryInfoStatus type : LotteryInfoStatus.values()) {
            if (type.getMessage().equals(message)) {
                return type.getCode();
            }
        }
        return null;
    }

}
