# LghHttp
A simple but powerful Http tool for Android


  Created by 林冠宏（指尖下的幽灵） on 2016/8/11.
 
  Blog : http://www.cnblogs.com/linguanh/;
 
  Name : http tool
  
  2017-2-16 补充，原测试链接可能已经失效，因本人不再承担它所在阿里云服务器的费用，建议大家切换到自己的URL
 
  前言：
      希望大家能够和我一起来完善它，该类肯定有很多不足的，但总体来说，还是不错的。
 
  下面是简介和拓展：
 
      1, 考虑到网络请求必不可少，采用了静态内部类单例模式
 
      2, 采用 newFixedThreadPool 线程池来管理并发线程，
         如果要替换，建议使用 newCacheThreadPool
 
      3, 功能方面提供三（+1）种常见操作：
            1）Get请求
            2）Post请求
            3）图片上传
            4）直接运行 runnable 接口（2016-9-12日更新）
      4, 优点:
            1) 绝对的轻量级，可以提升 APK 体积优化
            2）内存管理方面可以放心
           3）请求速度方法是纯系统的 HttpUrlConnection 请求，
              没有过多的代码片段
 
      5，可以进一步解耦拆分类，分为：
            1）公共部分
            2）数据部分
            3）请求核心部分

      6, 加入视频上传部分

### 使用例子(Demos)
First,add this to your project
```java
public class ThisApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /** init */
        LghHttp.getInstance().init();
    }
}
```
Some examples below

```java
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
```

