package lgh.httpdemo;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by 林冠宏（指尖下的幽灵） on 2016/8/11.
 *
 * Blog : http://www.cnblogs.com/linguanh/;
 *
 * Name : http 工具类
 *
 * 前言：
 *     希望大家能够和我一起来完善它，该类肯定有很多不足的，但总体来说，还是不错的。
 *
 * 下面是简介和拓展：
 *
 *     1, 考虑到网络请求必不可少，采用了静态内部类单例模式
 *
 *     2, 采用 newFixedThreadPool 线程池来管理并发线程，
 *        如果要替换，建议使用 newCacheThreadPool
 *
 *     3, 功能方面提供三种常见操作：
 *           1）Get请求
 *           2）Post请求
 *           3）图片上传
 *     4, 优点:
 *           1) 绝对的轻量级，可以提升 APK 体积优化
 *           2）内存管理方面可以放心
 *           3）请求速度方法是纯系统的 HttpUrlConnection 请求，
 *              没有过多的代码片段
 *
 *     5，可以进一步解耦拆分类，分为：
 *           1）公共部分
 *           2）数据部分
 *           3）请求核心部分
 *
 *     6, 加入视频上传部分
 *
 */

public class LghHttp {

    private final static String TAG = "zzzzz";

    public final static int Success = 0x10;
    public final static int UrlFailed = 0x11;
    public final static int TimeOut = 0x12;
    public final static int ProtocolFailed = 0x13;
    public final static int EncodingFailed = 0x14;
    public final static int IOFailed = 0x15;

    private final static boolean IsOpenCompress = true;/** 是否开启压缩 */
    private final static int CompressLimit = 500;      /** 压缩级别，单位是 K */

    private ThreadPoolExecutor threadPool;
    private Handler handler;
    /**
     * 全局回调接口 GloblelghHttpListeners
     * 注意：
     *     个人建议，如果请求页面多的，那就不要使用全局接口。尽量采用singleInterface
     *     否则，你需要在用户层页面的每次onResume重新设置
     * */
    private LghHttpGlobleListener GloblelghHttpListeners;

    public static LghHttp getInstance(){
        return LghHttpStatic.singleLghHttp;
    }

    private static class LghHttpStatic{
        private static LghHttp singleLghHttp = new LghHttp();
    }

    /** 销毁，内存释放善后操作 */
    public void destroy(){
        if(threadPool!=null){
            if(!threadPool.isShutdown()){
                threadPool.shutdown();
                threadPool = null;
            }
        }
        if(handler!=null){
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if(GloblelghHttpListeners!=null){
            GloblelghHttpListeners = null;
        }
        LghHttpStatic.singleLghHttp = null;
    }

    public void setGloblelghHttpListeners(LghHttpGlobleListener GloblelghHttpListeners){
        this.GloblelghHttpListeners = GloblelghHttpListeners;
    }

    /**
     * lgh.httpdemo.LghHttp 基础数据类
     * 作为 handler 传递的数据种子，只在成功时传递
     * */
    private class HttpDataBean implements Serializable{

        private String response;
        private LghHttpSingleListener listeners;

        public void setResponse(String response){
            this.response = response;
        }

        public void setListeners(LghHttpSingleListener listeners){
            this.listeners = listeners;
        }

        public String getResponse(){
            return this.response;
        }

        public LghHttpSingleListener getListeners(){
            return this.listeners;
        }
    }

    /** 初始化函数 */
    public synchronized void init(){
        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        this.handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                HttpDataBean bean = (HttpDataBean) msg.obj;
                LghHttpBaseListenr tempListener;
                if(GloblelghHttpListeners!=null){ /** 以全局的优先 */
                    tempListener = GloblelghHttpListeners;
                }else if(bean.getListeners()!=null){
                    tempListener = bean.getListeners();
                }else{
                    return;
                }
                switch (msg.what){
                    case Success:
                        if(GloblelghHttpListeners!=null){ /** 以全局的优先 */
                            GloblelghHttpListeners.onSuccess(msg.arg1,bean.getResponse());
                        }else{
                            bean.getListeners().onSuccess(bean.getResponse());
                        }
                        break;
                    case UrlFailed:
                        tempListener.onFailed(UrlFailed);
                        break;
                    case TimeOut:
                        tempListener.onFailed(TimeOut);
                        break;
                    case ProtocolFailed:
                        tempListener.onFailed(ProtocolFailed);
                        break;
                    case EncodingFailed:
                        tempListener.onFailed(EncodingFailed);
                        break;
                    case IOFailed:
                        tempListener.onFailed(IOFailed);
                        break;
                    default:
                        /** 这里不可能会进入，也当作一个留给你自己的接口吧 */
                        break;
                }
            }
        };
    }

    /** handler 发消息部分整合 */
    private void sendMessage(int what,int code,Object object){
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = code;
        msg.obj = object;
        handler.sendMessage(msg);
    }

    private void sendMessage(int what,Object object){
        sendMessage(what, -1, object);
    }

    /**
     * requestCode 请求标识符，方便区分
     * */

    /** Get 请求整合 */
    public void doGet(final String url){
        doGet(-1, url, null);
    }

    public void doGet(final int requestCode,final String url){
        doGet(requestCode, url, null);
    }

    public void doGet(
            final int requestCode,
            final String url,
            final LghHttpSingleListener lghHttpListeners)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                get(requestCode, url, lghHttpListeners);
            }
        };
        if(threadPool != null){
            threadPool.execute(runnable);
        }else{
            Log.d(TAG,"do get threadPool is null");
        }
    }

    private void get(int requestCode,String url,LghHttpSingleListener lghHttpListener){
        try {
            HttpURLConnection httpURLConnection = getHttpUrlConnection(url,"GET");
            httpURLConnection.setUseCaches(false);
            sendMessage(Success,requestCode, commonGetResult(httpURLConnection,lghHttpListener));
        } catch (MalformedURLException e) {
            dealWithException(e,lghHttpListener);
        } catch (IOException e) {
            dealWithException(e,lghHttpListener);
        }
    }

    /** Post 请求整合 */
    public void doPost(String url){
        doPost(-1, url);
    }

    public void doPost(int requestCode,String url){
        doPost(requestCode, url, null, null);
    }

    public void doPost(int requestCode,String url,LghHttpSingleListener listener){
        doPost(requestCode, url, null, null,listener);
    }

    public void doPost(int requestCode,String url,String[] keys,String[] values){
        doPost(requestCode, url, keys, values, null);
    }

    public void doPost(
            final int requestCode,
            final String url,
            final String[] keys,
            final String[] values,
            final LghHttpSingleListener listener
    ){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                post(requestCode, url,keys,values, listener);
            }
        };
        if(threadPool != null){
            threadPool.execute(runnable);
        }else{
            Log.d(TAG,"do post threadPool is null");
        }
    }

    /** 采用第一种post协议，application/x-www-form-urlencoded */
    private void post(
            int requestCode,
            String url,
            String[] keys,
            String[] values,
            LghHttpSingleListener listener
    ){
        if(url==null){
            return;
        }
        try{
            HttpURLConnection httpURLConnection = getHttpUrlConnection(url,"POST");
            httpURLConnection.setDoOutput(true); /** post 必不可少 */
            httpURLConnection.setUseCaches(false);

            if(keys!=null && values!=null){
                OutputStream outputStream = httpURLConnection.getOutputStream();
                commonCombinePostText(keys,values,outputStream);
                outputStream.flush();
                outputStream.close();
            }
            sendMessage(Success,requestCode, commonGetResult(httpURLConnection,listener));
        }catch (MalformedURLException e){
            dealWithException(e,listener);
        } catch (SocketTimeoutException e){
            dealWithException(e,listener);
        } catch (ProtocolException e) {
            dealWithException(e,listener);
        } catch (UnsupportedEncodingException e) {
            dealWithException(e,listener);
        } catch (IOException e) {
            dealWithException(e,listener);
        }
    }

    /** 上传图片部分整合 */
    public void doUpLoadPic(
            String url,
            String picName,
            String streamName,
            Bitmap bit
    ){
        doUpLoadPic(-1, url, null, null, picName, streamName, bit, null);
    }

    public void doUpLoadPic(
            int requestCode,
            String url,
            String picName,
            String streamName,
            Bitmap bit
    ){
        doUpLoadPic(requestCode, url, null, null, picName, streamName, bit, null);
    }

    public void doUpLoadPic(
            int requestCode,
            String url,
            String picName,
            String streamName,
            Bitmap bit,
            LghHttpSingleListener listener
    ){
        doUpLoadPic(requestCode, url, null, null, picName, streamName, bit, listener);
    }

    public void doUpLoadPic(
            int requestCode,
            String url,
            String[] keys,
            String[] values,
            String picName,
            String streamName,
            Bitmap bit
    ){
        doUpLoadPic(requestCode, url, keys, values, picName, streamName, bit, null);
    }

    public void doUpLoadPic(
            final int requestCode,
            final String url,
            final String[] keys,
            final String[] values,
            final String picName,
            final String streamName,
            final Bitmap bit,
            final LghHttpSingleListener listener
    ){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                UpLoadPic(requestCode, url, keys, values, picName, streamName, bit, listener);
            }
        };
        if(threadPool != null){
            threadPool.execute(runnable);
        }else{
            Log.d(TAG,"do post threadPool is null");
        }
    }

    /**
     * 此函数用来上传图片
     * post 的 两种数据包格式：
     * 1，application/x-www-form-urlencoded；用来上传文字
     * 2，multipart/form-data; 二进制传输，除了文字之外，还可以用来传输 文件，例如图片！
     * 3，multipart/form-data; 必须要带有分隔符 boundary
     * 4,在http post请求的结尾，需要有一个分界线，但是是前后都有--的：--分隔符--
     * 参数：
     *      url
     *      picName    图片的名称
     *      streamName 流体值的名称
     *      例如采用 php 接收，那么在服务器获取图片名称的写法是：$_FILES['streamName']['picName']
     **/
    private void UpLoadPic(
            int requestCode,
            String url,
            String[] keys,
            String[] values,
            String picName,
            String streamName,
            Bitmap bit,
            LghHttpSingleListener listener
    ){
        String twoHyphens = "--";   /** 一定要是 2行 */
        String boundary = "******"; /** 数据包分割线可以自定义 */
        try{
            HttpURLConnection httpURLConnection = getHttpUrlConnection(url,"POST");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setChunkedStreamingMode(1024 * 256); /** 一次传输的块大小 */
            /** 数据 --------包头-------- 格式组装 */
            httpURLConnection.setRequestProperty("Connection","Keep-Alive");
            httpURLConnection.setRequestProperty("Content-Type","multipart/form-data;boundary="+boundary);
            /** 数据 --------包体-------- 格式组装*/
            DataOutputStream body = new DataOutputStream(httpURLConnection.getOutputStream());
            /** \r\n 是换行 */
            body.writeBytes(twoHyphens+boundary+"\r\n"); /** 先写分隔符，标志和上面的头分开 */
            body.writeBytes(
                    "Content-Disposition:form-data;" +
                            "name=\"" + streamName + "\";" +
                            "filename=\"" + picName + "\"" + "\r\n"
            );
            /** 写文本数据体 */
            body.writeBytes("\r\n");
            if(keys!=null && values!=null){
                body.writeBytes(twoHyphens+boundary+"\r\n");
                body.writeBytes("Content-Disposition:form-data;");
                commonCombinePostText(keys,values,body);
                body.writeBytes("\r\n");
            }
            /** -------下面开始写图片二进制------- */
            /** 下面是先压缩 */
            int compress = 100;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bit.compress(Bitmap.CompressFormat.JPEG, compress, baos);
            if(IsOpenCompress){
                while (baos.toByteArray().length / 1024 > CompressLimit) {
                    baos.reset();
                    compress -= 10;
                    if(compress==0){
                        bit.compress(Bitmap.CompressFormat.JPEG, compress, baos);
                        break;
                    }
                    bit.compress(Bitmap.CompressFormat.JPEG, compress, baos);
                }
            }
            /** 开始写 */
            InputStream picStream = new ByteArrayInputStream(baos.toByteArray());
            byte[] buffer = new byte[10*1024];
            int count;
            while((count = picStream.read(buffer))!=-1){
                body.write(buffer,0,count);
            }
            picStream.close();
            body.writeBytes("\r\n");
            body.writeBytes(twoHyphens + boundary + twoHyphens +"\r\n");
            body.flush();
            /** 写完 */
            sendMessage(Success, requestCode, commonGetResult(httpURLConnection, listener));
            body.close();
        }catch (MalformedURLException e){
            dealWithException(e,listener);
        } catch (SocketTimeoutException e){
            dealWithException(e,listener);
        } catch (ProtocolException e) {
            dealWithException(e,listener);
        } catch (UnsupportedEncodingException e) {
            dealWithException(e,listener);
        } catch (IOException e) {
            dealWithException(e, listener);
        }
    }

    /** 公共部分，异常集合处理 */
    private void dealWithException(
            Exception e,
            LghHttpSingleListener lghHttpListeners)
    {
        HttpDataBean bean = new HttpDataBean();
        bean.setListeners(lghHttpListeners);
        if(e instanceof MalformedURLException){
            Log.d(TAG, "链接格式有问题 "+e.toString());
            sendMessage(UrlFailed,bean);
        }else if(e instanceof SocketTimeoutException){
            Log.d(TAG, "连接超时 "+e.toString());
            sendMessage(TimeOut,bean);
        }else if(e instanceof ProtocolException){
            Log.d(TAG, "协议异常，注意不要多次连接 " + e.toString());
            sendMessage(ProtocolFailed, bean);
        }else if(e instanceof UnsupportedEncodingException){
            Log.d(TAG, "编码类型异常 " + e.toString());
            sendMessage(EncodingFailed, bean);
        }else if(e instanceof IOException){
            Log.d(TAG, "io 异常 " + e.toString());
            sendMessage(IOFailed,bean);
        }
    }

    /** 获取一个HttpUrlConnection，合并一些公共部分 */
    private static HttpURLConnection getHttpUrlConnection
            (String url,String requestWay) throws IOException {
        Log.d(TAG,"url is "+url);
        URL mRrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) mRrl.openConnection();
        httpURLConnection.setRequestMethod(requestWay);
        httpURLConnection.setRequestProperty("Charset", "UTF-8");
        httpURLConnection.setConnectTimeout(5 * 1000);
        return httpURLConnection;
    }

    /** 获取结果公共部分 */
    private HttpDataBean commonGetResult(
            HttpURLConnection httpURLConnection,
            LghHttpSingleListener listener
    ) throws IOException {
        if(httpURLConnection==null){
            return null;
        }
        BufferedReader br = new BufferedReader
                (
                        new InputStreamReader(httpURLConnection.getInputStream(),"UTF-8"),
                        8*1024
                );
        StringBuffer resultBuffer = new StringBuffer("");
        String line;
        while ((line = br.readLine())!=null){
            resultBuffer.append(line);
        }
        HttpDataBean bean = new HttpDataBean();
        bean.setResponse(resultBuffer.toString());
        bean.setListeners(listener);
        br.close();
        return bean;
    }

    /** 组合 post 文本数据公共部分 */
    private OutputStream commonCombinePostText(
            String[] keys,
            String[] values,
            OutputStream outputStream) throws IOException
    {
        StringBuffer requestStr = new StringBuffer();
        int keysLength = keys.length;
        for(int i=0;i<keysLength;i++){
            requestStr.append(keys[i]+"="+values[i]+"&");
        }
        outputStream.write(requestStr.toString().getBytes());
        return outputStream;
    }

    /** 接口分离 */
    private interface LghHttpBaseListenr{
        void onFailed(int type);
//        void onUrlFailed();
//        void onTimeOut();
//        void onProtocolFailed();
//        void onEncodingFailed();
//        void onIoFailed();
    }

    /** 全局有 requestCode 区分 */
    public interface LghHttpGlobleListener extends LghHttpBaseListenr{
        void onSuccess(int requestCode, String response);
    }

    /** 单一的没 requestCode 区分 */
    public interface LghHttpSingleListener extends LghHttpBaseListenr{
        void onSuccess(String response);
    }

}
