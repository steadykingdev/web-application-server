package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        User user = null;
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            // request
            // line : GET /index.html HTTP/1.1
            String line = br.readLine();

            String[] lineSplit = line.split("\\?");

            String[] tokens = line.split(" ");
            String url = tokens[1];
            if (line == null) {
                return;
            }

            log.debug("url = {}", url);

            String[] urlSplit = url.split("\\?");

            if (urlSplit[0].equals("/user/create")) {
                String[] queryString = urlSplit[1].split(" ");
                Map<String, String> queryMap = HttpRequestUtils.parseQueryString(queryString[0]);
                user = new User(queryMap.get("userId"), queryMap.get("password"), queryMap.get("name"), queryMap.get("email"));
            }

            log.debug("user = {}", user);

            // header
            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
            }

            // response body
            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());

            DataOutputStream dos = new DataOutputStream(out);

            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
