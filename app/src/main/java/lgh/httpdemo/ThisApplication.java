package lgh.httpdemo;

import android.app.Application;

/**
 * Created by 林冠宏 on 2016/8/13.
 *
 */

public class ThisApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /** init */
        LghHttp.getInstance().init();
    }
}
