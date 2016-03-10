package br.com.rtools.utilitarios;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.faces.bean.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ApplicationScoped
@ServerEndpoint("/ws_socket/{identifier}")
public class WSSocket {

    static ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private static final Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    private static final Map<String, Date> sendRow = Collections.synchronizedMap(new LinkedHashMap<>());

    public WSSocket() {
    }

    // DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    @OnOpen
    public void open(Session session, @PathParam("identifier") String identifier) {
        clients.add(session);
    }

    @OnClose
    public void close(Session session) {
        clients.remove(session);
    }

    // @OnMessage
    public static void send(String identifier) {
        for (Session sess : clients) {
            if (sess.getRequestURI().getPath().contains(identifier)) {
                try {
                    sess.getBasicRemote().sendText("1");
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                }
            }
        }
    }
}
