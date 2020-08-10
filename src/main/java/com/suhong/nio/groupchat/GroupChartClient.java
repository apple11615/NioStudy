package com.suhong.nio.groupchat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
* NIO聊天客户端
* */
public class GroupChartClient {
    private String ip = "127.0.0.1";
    private int port = 8888;

    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    private String name = "";
    private static String USER_EXIST = "system message: user exist, please change a name";
    private static String USER_CONTENT_SPILIT = "\r\n";
    private Charset charset = Charset.forName("UTF-8");

    public static ExecutorService readPool = Executors.newFixedThreadPool(10);

    public static ExecutorService sendPool = Executors.newFixedThreadPool(10);

    //构造器
    public GroupChartClient() throws IOException, InterruptedException {
//        完成初始化工作
        selector = Selector.open();

        socketChannel = SocketChannel.open(new InetSocketAddress(ip,port));

        socketChannel.configureBlocking(false);

        socketChannel.register(selector, SelectionKey.OP_READ);

        username = socketChannel.getLocalAddress().toString().substring(1);

        System.out.println(username + " is ok ...");

        readPool.execute(new ReadMsg());

        //在主线程中 从键盘读取数据输入到服务器端
        Scanner scan = new Scanner(System.in);
        while(scan.hasNextLine())
        {
            String line = scan.nextLine();
            if("".equals(line)) continue; //不允许发空消息
            if("".equals(name)) {
                name = line;
                line = name+":";
            } else {
                line = name+USER_CONTENT_SPILIT+line;
            }
            socketChannel.write(charset.encode(line));//sc既能写也能读，这边是写
        }



    }

     class ReadMsg implements Runnable {

        public void run() {
            readinfo();
        }
    }

    class SendMsg implements Runnable {

        private String msg;

        public SendMsg (String msg){
            this.msg = msg;
        }

        public void run() {
            try {
                sendInfo(msg);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //向服务器发送消息
    public void sendInfo(String info){

        try {
            info = username + "say: " + info;
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //服务器回复消息
    public void readinfo(){

        try {
            while(true) {
                //读取服务端消息
                int readChannels = selector.select(2000);
                if (readChannels > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isAcceptable()) {
                            key.interestOps(SelectionKey.OP_ACCEPT);
                        } else if (key.isReadable()) {
                            //得到相关通道
                            SocketChannel channel = (SocketChannel) key.channel();
                            //分配缓冲
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                            channel.read(byteBuffer);
                            String msg = new String(byteBuffer.array());
                            System.out.println(":" + msg.trim());
                            key.interestOps(SelectionKey.OP_READ);

                        } else {
                            System.out.println("没有可用的通道.....");
                        }
                        iterator.remove();
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        GroupChartClient groupChartClient = new GroupChartClient();



    }


}
