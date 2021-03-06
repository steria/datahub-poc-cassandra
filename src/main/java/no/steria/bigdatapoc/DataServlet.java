package no.steria.bigdatapoc;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;

public class DataServlet extends HttpServlet {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final AtomicLong counter2 = new AtomicLong(0);
    private Session session = Database.getInstance().getSession();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ServletInputStream inputStream = req.getInputStream();
            String json = toString(inputStream);

            JSONArray jsonArray = new JSONArray(json);
            PreparedStatement insertStation = session.prepare("INSERT INTO POCJAN.stasjon_dag (tidsstempel, dag, stasjon, kommune, kw) VALUES (?, ?, ?, ?,?)");
            BatchStatement batch = new BatchStatement();
            batch.setConsistencyLevel(ConsistencyLevel.QUORUM);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String timeStamp = jsonObject.getString("timeStamp");
                double kw = jsonObject.getDouble("kw");
                String stationId = jsonObject.getString("stationId");
                String council = jsonObject.getString("council");
                batch.add(insertStation.bind(new DateTime(timeStamp).toDate(), timeStamp.split("T")[0], stationId, council, kw));
                counter.incrementAndGet();
            }
            session.execute(batch);

            counter2.incrementAndGet();
            if (counter2.get() % 10 == 0) {
                System.out.println(counter.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resp.setStatus(HttpServletResponse.SC_OK);
        }

    }

    private static String toString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
}
