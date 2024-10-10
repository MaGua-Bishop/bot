package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
public enum InviteType {

    CHANNEL(0, "channel"),
    GROUP(1, "group"),
    SUPERGROUP(2, "supergroup");

    private final Integer code;
    private final String message;

    InviteType(Integer code, String message) {
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
        for (InviteType type : InviteType.values()) {
            if (type.getCode().equals(code)) {
                return type.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (InviteType type : InviteType.values()) {
            if (type.getMessage().equals(message)) {
                return type.getCode();
            }
        }
        return null;
    }
}
