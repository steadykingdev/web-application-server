package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

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

            log.info("line = {}", line);

            String[] tokens = line.split(" ");
            String httpMethod = tokens[0];
            String url = tokens[1];
            if (line == null) {
                return;
            }

            log.debug("httpMethod = {}", httpMethod);
            log.debug("url = {}", url);

            // get 회원가입
//            String[] urlSplit = url.split("\\?");

//            if (urlSplit[0].equals("/user/create")) {
//                String[] queryString = urlSplit[1].split(" ");
//                Map<String, String> queryMap = HttpRequestUtils.parseQueryString(queryString[0]);
//                user = new User(queryMap.get("userId"), queryMap.get("password"), queryMap.get("name"), queryMap.get("email"));
//            }
//
//            log.debug("get request user = {}", user);

            int contentLength = 0;

            // header
            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
                if(line.contains("Content-Length")) {
                    String[] contentLengthSplit = line.split(" ");
                    contentLength = Integer.parseInt(contentLengthSplit[1]);
                }
            }

            // post 회원가입
            if (httpMethod.equals("POST") && url.equals("/user/create")) {
                String body = IOUtils.readData(br, contentLength);
                log.debug("body = {}", body);

                Map<String, String> queryMap = HttpRequestUtils.parseQueryString(body);
                user = new User(queryMap.get("userId"), queryMap.get("password"), queryMap.get("name"), queryMap.get("email"));

                byte[] redirectPath = Files.readAllBytes(new File("./webapp/index.html").toPath());

                DataOutputStream dos = new DataOutputStream(out);

                response302Header(dos, redirectPath.length);
                responseBody(dos, redirectPath);

                log.debug("user = {}", user);

                return;
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

    private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
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
