import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class PollingServer {

    public static void main(String[] args) throws Exception {
        Server server = new Server(5000);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        server.setHandler(handler);

        // Create shared data state
        DataStore dataStore = new DataStore();


        handler.addServlet(new ServletHolder((Servlet) new ShortPollingServlet(dataStore)), "/shortpoll");
        handler.addServlet(new ServletHolder((Servlet) new LongPollingServlet(dataStore)), "/longpoll");


        // Start server
        server.start();
        System.out.println("Server started on http://localhost:5000");
        System.out.println("Short Polling endpoint: http://localhost:5000/shortpoll");
        System.out.println("Long Polling endpoint: http://localhost:5000/longpoll");

        // Background thread to simulate new data every 10 seconds
        new Thread(() -> {
            try {
                int count = 0;
                while (true) {
                    Thread.sleep(10000);
                    count++;
                    dataStore.updateData("Update #" + count);
                    System.out.println("Data updated: Update #" + count);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        server.join();
    }

    // Shared data store (simulating database)
    static class DataStore {
        private final AtomicInteger latestId = new AtomicInteger(0);
        private volatile String latestData = "";

        public int getLatestId() {
            return latestId.get();
        }

        public String getLatestData() {
            return latestData;
        }

        public void updateData(String newData) {
            latestData = newData;
            latestId.incrementAndGet();
        }
    }

    // Short Polling Servlet
    public static class ShortPollingServlet extends HttpServlet {
        private final DataStore dataStore;

        public ShortPollingServlet(DataStore dataStore) {
            this.dataStore = dataStore;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String lastSeenStr = req.getParameter("lastSeenId");
            int lastSeenId = lastSeenStr != null ? Integer.parseInt(lastSeenStr) : 0;

            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();

            int currentId = dataStore.getLatestId();
            if (currentId > lastSeenId) {
                out.print("{ \"newData\": \"" + dataStore.getLatestData() + "\", \"id\": " + currentId + " }");
            } else {
                out.print("{ \"newData\": null }");
            }

            out.flush();
        }
    }

    // Long Polling Servlet
    public static class LongPollingServlet extends HttpServlet {
        private final DataStore dataStore;

        public LongPollingServlet(DataStore dataStore) {
            this.dataStore = dataStore;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String lastSeenStr = req.getParameter("lastSeenId");
            int lastSeenId = lastSeenStr != null ? Integer.parseInt(lastSeenStr) : 0;

            resp.setContentType("application/json");
            PrintWriter out = resp.getWriter();

            long timeoutMillis = 20000;  // 20 seconds
            long startTime = System.currentTimeMillis();

            int currentId = dataStore.getLatestId();

            // Wait loop until new data or timeout
            while (currentId <= lastSeenId && (System.currentTimeMillis() - startTime) < timeoutMillis) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                currentId = dataStore.getLatestId();
            }

            if (currentId > lastSeenId) {
                out.print("{ \"newData\": \"" + dataStore.getLatestData() + "\", \"id\": " + currentId + " }");
            } else {
                out.print("{ \"newData\": null }");
            }
            out.flush();
        }
    }
}

