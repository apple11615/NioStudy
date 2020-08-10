package com.suhong.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
//聊天室服务端

public class GroupChartServer {

//
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private static final int PORT = 8888;

    public GroupChartServer(){

        try {
            selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.socket().bind(new InetSocketAddress("127.0.0.1",PORT));

            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void listening(){

        try {

            while (true) {
                int count = selector.select(2000);
                if(count>0){
                    //遍历
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        SocketChannel socketChannel = null;
                        if(key.isAcceptable()){

                            String clientIp = "";
                            try {
                                socketChannel = serverSocketChannel.accept();
                                socketChannel.configureBlocking(false);
                                //注册到channel
                                socketChannel.register(selector, SelectionKey.OP_READ);
                                //提示
                                clientIp = socketChannel.getLocalAddress().toString();
                                key.interestOps(SelectionKey.OP_ACCEPT);
                                System.out.println(socketChannel.getRemoteAddress() + " 上线....");
                            }catch (Exception e){
                                System.out.println(clientIp + " 离线了..");
                                //取消注册
                                key.cancel();
                                //关闭通道
                               key.channel().close();
                            }
                        }else if(key.isReadable()){
                            try {
                                readData(key);
                                //注册到channel
                                key.interestOps(SelectionKey.OP_READ);
                            }catch (Exception e){
                                System.out.println( " 离线了..");
                                //取消注册
                                key.cancel();
                                //关闭通道
                                key.channel().close();
                            }
                        }
                        iterator.remove();
                    }


                }else{
                    System.out.println("未有新连接进来.......");
                }
            }
        } catch (Exception e) {
           e.printStackTrace();
        } finally {

        }

    }

    public void readData(SelectionKey key) throws IOException {

        SocketChannel socketChannel = null;
        try {
            //处理
            socketChannel = (SocketChannel)key.channel();
            //创建Buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int count = socketChannel.read(byteBuffer);
            //根据count的值进行处理
            if(count > 0 ){
                String msg = new String(byteBuffer.array());
                System.out.println("from 客户端:"+msg);

                //向其他客户端转发消息(去掉自己)，专门写个方法处理
                sendInfoToOthers(msg,socketChannel);

            }

        } catch (Exception e) {
            System.out.println(socketChannel.getLocalAddress() + " 离线了..");
            //取消注册
            key.cancel();;
            //关闭通道
            socketChannel.close();
            e.printStackTrace();
        } finally {
        }

    }

//    转发给其他通道客户
    private void sendInfoToOthers(String msg,SocketChannel selfChannel) throws IOException {
         System.out.println("服务端消息转发中......");
        try {
            for(SelectionKey key : selector.keys()){
                //通过key取出我们的channel客户端
                Channel targetChannel = key.channel();
                System.out.println("获取已连接通道:"+targetChannel.toString());
   //             排除自己
                if(targetChannel instanceof SocketChannel && targetChannel != selfChannel){
                    //转型
                    SocketChannel channel = (SocketChannel)targetChannel;

                    System.out.println("转型通道:"+targetChannel.toString());

                    ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
                    channel.write(byteBuffer);

                }

            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static void main(String[] args){

          new GroupChartServer().listening();

    }

}
