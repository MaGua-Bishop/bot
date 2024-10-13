package com.li.bot.utils;

import java.text.DecimalFormat;

/**
 * @Author: li
 * @CreateTime: 2024-10-12
 */
public class UnitConversionUtils {

    public static String toThousands(Long number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        // 计算千单位的数量，并保留两位小数
        double thousands = number / 1000.0;

        // 使用 DecimalFormat 来格式化数字
        DecimalFormat df = new DecimalFormat("#.###"); // 保留两位小数
        String formattedThousands = df.format(thousands);

        // 构造结果字符串
        return formattedThousands + "K";
    }


    public static String tensOfThousands(Long number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        // 计算万单位的数量，并保留两位小数
        double tenThousandths = number / 10000.0;

        // 使用 DecimalFormat 来格式化数字
        DecimalFormat df = new DecimalFormat("#.####"); // 保留两位小数
        String formattedTenThousandths = df.format(tenThousandths);

        // 构造结果字符串
        return formattedTenThousandths + "W人";
    }


}
