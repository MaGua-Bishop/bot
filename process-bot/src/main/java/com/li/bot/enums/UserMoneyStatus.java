package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public enum UserMoneyStatus {

    Recharge(0, "充值"),
    Reduce(3, "管理员减少余额"),
    Add(4, "管理员增加余额");

    private final Integer code;
    private final String message;

    UserMoneyStatus(Integer code, String message) {
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
        for (UserMoneyStatus status : UserMoneyStatus.values()) {
            if (status.getCode().equals(code)) {
                return status.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (UserMoneyStatus status : UserMoneyStatus.values()) {
            if (status.getMessage().equals(message)) {
                return status.getCode();
            }
        }
        return null;
    }


}
