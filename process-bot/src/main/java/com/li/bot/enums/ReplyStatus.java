package com.li.bot.enums;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public enum ReplyStatus {

//     0 待处理 1已回复



     REVIEW(0,"待处理"),

    PENDING(1, "已回复");

    private final Integer code;
    private final String message;

    ReplyStatus(Integer code, String message) {
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
        for (ReplyStatus status : ReplyStatus.values()) {
            if (status.getCode().equals(code)) {
                return status.getMessage();
            }
        }
        return null;
    }

    public static Integer getCodeByMessage(String message) {
        for (ReplyStatus status : ReplyStatus.values()) {
            if (status.getMessage().equals(message)) {
                return status.getCode();
            }
        }
        return null;
    }



}
