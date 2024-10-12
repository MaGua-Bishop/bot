package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
public enum ConvoysInviteStatus {

    IDLE(0, "空闲"),

    REVIEW(1, "待审核"),

    BOARDED(2, "已上车"),

    DISABLED(3, "被禁用");



    private final Integer code;
    private final String message;

    ConvoysInviteStatus(Integer code, String message) {
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
        for (ConvoysInviteStatus type : ConvoysInviteStatus.values()) {
            if (type.getCode().equals(code)) {
                return type.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (ConvoysInviteStatus type : ConvoysInviteStatus.values()) {
            if (type.getMessage().equals(message)) {
                return type.getCode();
            }
        }
        return null;
    }
}
