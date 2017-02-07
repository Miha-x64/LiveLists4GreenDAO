package org.greenrobot.greendao.query;

/**
 * Created by miha on 07.02.17
 */

public final class GreenLists$Internal$QuerySpy {
    private GreenLists$Internal$QuerySpy() {}
    public static String getSql(Query<?> query) {
        return query.sql;
    }
    public static String[] getParameters(Query<?> query) {
        return query.parameters;
    }
}
