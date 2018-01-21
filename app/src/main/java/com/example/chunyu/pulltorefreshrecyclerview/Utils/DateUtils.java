package com.example.chunyu.pulltorefreshrecyclerview.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 人间一小雨 on 2018/1/21 下午9:52
 * Email: 746431278@qq.com
 */

public class DateUtils {

    public static final SimpleDateFormat birth;

    static {
        birth = new SimpleDateFormat("yyyy-MM-dd");
    }


    public static String getBirthString(long time) {
        Date d = new Date(time);
        return birth.format(d);
    }

}
