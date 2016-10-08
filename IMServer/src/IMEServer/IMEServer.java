package IMEServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.security.cert.Extension;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by LiuToTo.365ime on 16/9/30.
 */
public class IMEServer {
    private  int port = 8888;
    // 解码buffer
    private Charset cs = Charset.forName("gbk");
    // 接收数据缓冲区
    private ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
    // 发送数据缓冲区
    private  ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
    // 客户端channel 映射
    private Map<String ,SocketChannel> clientsMap = new HashMap<String, SocketChannel>();

    // 选择器
    private  static Selector selector;

    public  IMEServer(int port){
        this.port = port;
        try{
            init();;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void init() throws IOException{
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(new InetSocketAddress("192.168.60.96",port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.print("server start on port:"+port);
    }

    // 事件监听
    private  void listen() {
        while (true){
            try{
                int selectCount  =  selector.select();
                System.out.print("本次触发的事件个数："+ selectCount);
                if (selectCount <=0){
                    continue;
                }

                System.out.print("1");
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key: selectionKeys) {
                    handle(key);
                }

                selectionKeys.clear();;
            }catch (Exception e){
                e.printStackTrace();
                break;
            }
        }
    }

    // 事件处理
    private void handle(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = null;
        SocketChannel client = null;
        String receiveText = null;
        int count = 0;

        if (selectionKey.isAcceptable()){
            server = (ServerSocketChannel)selectionKey.channel();
            client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        } else if(selectionKey.isReadable()){
            client = (SocketChannel) selectionKey.channel();
            receiveBuffer.clear();

            count = client.read(receiveBuffer);
            if (count > 0){
                receiveBuffer.flip();
                receiveText = String.valueOf(cs.decode(receiveBuffer).array());
                System.out.println(client.toString()+":"+receiveText);

                dispatch(client,receiveText);
                client = (SocketChannel)selectionKey.channel();
                client.register(selector,SelectionKey.OP_READ);
            }
        }
    }

    private  void dispatch(SocketChannel client,String info) throws IOException{
        Socket s = client.socket();
        String name = "["+s.getInetAddress().toString().substring(1)+":"+Integer.toHexString(client.hashCode())+"]";

        if (!clientsMap.isEmpty()){
            for (Map.Entry<String, SocketChannel> entry: clientsMap.entrySet()) {
                SocketChannel temp = entry.getValue();
                if (!client.equals(temp)){
                    sendBuffer.clear();
                    sendBuffer.put((name+":"+info).getBytes());
                    sendBuffer.flip();

                    // 输出到通道
                    temp.write(sendBuffer);
                }
            }

        }
        clientsMap.put(name,client);
    }

    public  static  void startServer() throws  IOException{
        IMEServer imServer = new IMEServer(8888);
        imServer.listen();
    }
}
