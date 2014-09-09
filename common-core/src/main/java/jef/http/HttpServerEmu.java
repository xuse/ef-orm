package jef.http;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.common.base.Function;

/**
 * HTTP服务的模拟器，用于一些自动化测试等场合模拟环境用
 * 
 * @author jiyi
 * 
 */
public class HttpServerEmu implements Closeable{
	private boolean shutdown;
	private boolean closed;
	private int port = 80;
	private File root;
	private Function<Response, Void> handler;

	/**
	 * 构造模拟器
	 */
	public HttpServerEmu() {
	}
	
	/**
	 * 构造模拟器
	 * @param port 端口
	 */
	public HttpServerEmu(int port) {
		this.port=port;
	}

	private void await() {
		if (root == null) {
			root = new File(System.getProperty("user.dir"));
		}
		ServerSocket serverSocket = null;
		closed=false;
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
			System.out.println("Server started. root=" + root + ",port=" + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!shutdown) {
			Socket socket = accept(serverSocket);
			try {
				InputStream input = socket.getInputStream();
				OutputStream output = socket.getOutputStream();
				Request request = new Request(input);
				request.parse();
				Response response = new Response(output);
				response.setRequest(request);
				
				if("/!shutdown".equals(request.getUri())){
					response.send404();
					shutdown=true;
					return;
				}
				{
					if (handler != null) {
						handler.apply(response);
					} else {
						File file = new File(root, request.getUri());
						if(file.isFile()){
							response.sendStaticResource(file);
						}else{
							response.send404();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			} finally {
				close(socket);
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		closed=true;
		System.out.println("Server at port="+port+" closed.");
	}

	private void close(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Socket accept(ServerSocket serverSocket) {
		try {
			return serverSocket.accept();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Function<Response, Void> getHandler() {
		return handler;
	}
	
	/**
	 * 得到资源根目录
	 * @return
	 */
	public File getRoot() {
		return root;
	}

	/**
	 * 设置资源根目录
	 * @param root
	 */
	public void setRoot(File root) {
		this.root = root;
	}

	/**
	 * 设置处理句柄
	 * @param handler
	 * @return
	 */
	public HttpServerEmu setHandler(Function<Response, Void> handler) {
		this.handler = handler;
		return this;
	}
	
	/**
	 * 启动服务器
	 */
	public void start(){
		Thread t=new Thread(){
			public void run() {
				await();
			}
			
		};
		t.setName("HttpServer Emulator");
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * 关闭服务器
	 */
	public void close() throws IOException{
		if(!closed){
			Socket s=new Socket("127.0.0.1",port);
			s.getOutputStream().write("GET /!shutdown HTTP/1.1\r\n".getBytes());
			s.close();
		}
	}
	
	public static void main(String[] args) {
		HttpServerEmu s=new HttpServerEmu();
		s.await();
	}
}
