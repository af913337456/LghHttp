package lgh.httpdemo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private LghHttp lghHttp = LghHttp.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Example 1 */
        /** 使用全局接口并发起 post 操作 */
        lghHttp.setGloblelghHttpListeners(new LghHttp.LghHttpGlobleListener() {
            @Override
            public void onFailed(int type) {
                switch (type) {
                    case LghHttp.UrlFailed:

                        break;
                    case LghHttp.Success:

                        break;
                    case LghHttp.TimeOut:

                        break;
                    /**  .... */
                }
            }

            @Override
            public void onSuccess(int requestCode, String response) {
                Log.d("zzzzz", "LghHttpGlobleListener do post response "
                        + response + " requestCode is " + requestCode);
                switch (requestCode) {
                    case 123:
                        /** 对应操作 */
                        break;
                }
            }

        });
        for(int i=0;i<30;i++){
            final int j = i;
            lghHttp.doPost
                    (
                            j,
                            "http://121.42.190.18/ydnurse/Controller/noteController.php?func=GetNote",
                            null
                    );
        }
        // 带有键值
        lghHttp.doPost
                (
                        145,
                        "http://121.42.190.18/ydnurse/Controller/noteController.php?func=GetNote",
                        new String[]{"userName","userAge","userSex"},
                        new String[]{"林冠宏","21","Box"},
                        null
                );

        /** -----测试下面的例子，要把全局的接口 LghHttpGlobleListener 设置为 NUll----- */

        /** Note that when you test the following example,
         * remember to set the global interface to NULL.
         * like:
         *      ghHttp.setGloblelghHttpListeners(null);
         * */

        /** Example 2 */
        lghHttp.doGet
                (
                        123,
                        "http://121.42.190.18/ydnurse/Controller/noteController.php?func=GetNote",
                        new LghHttp.LghHttpSingleListener() {
                            @Override
                            public void onSuccess(String response) {

                            }

                            @Override
                            public void onFailed(int type) {

                            }
                        }
                );
        /** UpPic Example 3 */

        lghHttp.doUpLoadPic(
                "http://www.xiangjiaoyun.com:8888/BCapp/BananaCloudServer/userPicUploadFile.php?" +
                        "account=13726204215&postid=0&type=2",
                "123.jpg",
                "uploadedfile",
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)
        );

        lghHttp.doUpLoadPic(
                1456,
                "http://www.xiangjiaoyun.com:8888/BCapp/BananaCloudServer/userPicUploadFile.php?" +
                        "account=13726204215&postid=0&type=2",
                "123.jpg",
                "uploadedfile",
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                new LghHttp.LghHttpSingleListener() {
                    @Override
                    public void onSuccess(String response) {
                        Toast.makeText(MainActivity.this,"上传图片成功",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(int type) {

                    }
                }
        );
    }
}
