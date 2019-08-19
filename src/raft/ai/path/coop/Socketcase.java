package raft.ai.path.coop;

import java.io.*;
import java.net.Socket;

public class Socketcase {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost",10086);
        OutputStream os = socket.getOutputStream();
        PrintWriter pw = new PrintWriter(os);
        pw.write("我是谁");
        pw.flush();
        socket.shutdownOutput();

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String data = null;
        while((data = br.readLine())!= null){
            System.out.println(data);
        }

        br.close();
        isr.close();
        is.close();

        pw.close();
        os.close();
    }
}
