package Tool;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    ServerSocket serverSocket = null;
    Socket socket = null;
    //在线Map
    HashMap<String, Socket> hashMap = null;

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
        service = Executors.newFixedThreadPool(50);
        hashMap = new HashMap<>();
        System.out.println("Server Init over!");
    }

    public void Accept() throws IOException {
        System.out.println("Server connect ......");
        while (flag){
            socket = serverSocket.accept();
            //<ip, socket>
            hashMap.put(socket.getInetAddress().getHostAddress(), socket);
            System.out.println("Server connect success!");
            service.execute(() -> {
                try {
                    //绑定输入和输出流
                    BufferedReader br =new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    //接收消息为bye结束此连接
                    String line = null;
                    while(!(line = br.readLine()).equals("bye")) {
                        System.out.println("line:" + line);
                        bw.write("服务器返回：" + line);
                        bw.newLine();
                        bw.flush();
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
                        String ip = socket.getInetAddress().getHostAddress();
                        if(hashMap.containsKey(ip)) {
                            hashMap.remove(ip);
                        }
                    }finally{
                        lock.unlock();
                    }
                    System.out.println(hashMap);
                }
            });
        }
        service.shutdown();
        serverSocket.close();
        System.out.println("Server connect over!");
    }

}
