package Tool;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    ServerSocket serverSocket = null;
    Socket socket = null;
    //在线Map
    ArrayList<Socket> arrayList = null;

    int port;

    /**
     * flag：当flag为false,服务器退出监听状态
     */
    public boolean flag = true;
    ExecutorService service = null;

    private final ReentrantLock lock=new ReentrantLock();

    public Server(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);

        //1.添加线程池，
        // 客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销，
        // 因此通过线程池机制改善
        service = Executors.newFixedThreadPool(50);

        //2.在线Map
        arrayList = new ArrayList<>();
        System.out.println("Server Init over!");
    }

    /**
     * 开启后一直监听指定端口，flag决定服务器最终是否停止运行
     *
     * 功能：服务器对每个连接使用一个线程处理，完成接收消息并转发回客户端
     *
     * @throws IOException
     */
    public void Accept() throws IOException {
        System.out.println("Server connect ......");
        while (flag){
            socket = serverSocket.accept();

            //<hostName, socket>
            arrayList.add(socket);
            System.out.println("Server connect success!");
            System.out.println(arrayList);
            service.execute(() -> {
                try {
                    System.out.println(socket.getInetAddress().getHostAddress() + "连接进入");
                    //绑定输入和输出流
                    BufferedReader br =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    //获取登录请求用户信息
                    String loginInfo = br.readLine();
                    User user = JSON.parseObject(loginInfo, User.class);
                    System.out.println(user);


                    //接收消息为bye结束此连接
                    String line = null;
                    while(!(line = br.readLine()).equals("bye")) {
                        String sendMsg = user.getName() + ":" + line;
                        System.out.println(sendMsg);

                        //群聊
                        for(int i = 0; i < arrayList.size(); i++){
                            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(arrayList.get(i).getOutputStream()));
                            w.write(sendMsg);
                            w.newLine();
                            w.flush();
                        }
                        /*
                        单聊
                        bw.write(user.getName() + ":" + line);
                        bw.newLine();
                        bw.flush();
                        */
                    }
                    br.close();
                    bw.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Server connect over!");

                    //显式加锁，用于线程对公共资源hashMap的访问操作
                    lock.lock();
                    try{
                        //移除在线集合,保证线程安全的代码;
                        if(arrayList.contains(socket)) {
                            arrayList.remove(socket);
                            System.out.println("从在线arrayList中移除一个连接用户的Socket!");
                        }
                    }finally{
                        lock.unlock();
                    }
                    System.out.println(arrayList);
                }
            });
        }
        service.shutdown();
        serverSocket.close();
        System.out.println("Server connect over!");
    }

}
