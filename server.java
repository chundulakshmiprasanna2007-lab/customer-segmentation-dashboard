import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {

    static final String DB_URL  = "jdbc:mysql://localhost:3306/customer_segmentation";
    static final String DB_USER = "root";
    static final String DB_PASS = "Prasi@2007";

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
           new InetSocketAddress(7070), 0);
        

        server.createContext("/addCustomer", exchange -> {
            try {
                exchange.getResponseHeaders().add(
    "Access-Control-Allow-Origin", "*"
);
exchange.getResponseHeaders().add(
    "Access-Control-Allow-Methods", "POST, GET, OPTIONS"
);
exchange.getResponseHeaders().add(
    "Access-Control-Allow-Headers", "Content-Type"
);
exchange.getResponseHeaders().add(
    "Access-Control-Max-Age", "3600"
);
exchange.getResponseHeaders().add(
    "Content-Type", "text/plain"
);

                if("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if("POST".equals(exchange.getRequestMethod())) {

                    // Read data from form
                    String body = new String(
                        exchange.getRequestBody().readAllBytes()
                    );

                    // Parse values
                    Map<String,String> params = parseParams(body);
                    String name     = params.get("name");
                    int age         = Integer.parseInt(params.get("age"));
                    String gender   = params.get("gender");
                    float income    = Float.parseFloat(params.get("income"));
                    String location = params.get("location");
                    float amount    = Float.parseFloat(params.get("amount"));

                    // Connect and insert
                    Connection con = DriverManager.getConnection(
                        DB_URL, DB_USER, DB_PASS
                    );

                    insertCustomer(con, name, age, gender, income, location, amount);
                    writeSegmentsJS(con);
                    writeDataJS(con);

                    con.close();

                    String response = "Customer added successfully!";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }

            } catch(Exception e) {
                String err = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, err.length());
                OutputStream os = exchange.getResponseBody();
                os.write(err.getBytes());
                os.close();
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("✓ Server started at http://localhost:7070");
        System.out.println("✓ Keep this running while using dashboard!");
    }

    // ── Insert new customer + transaction ──
    static void insertCustomer(
        Connection con, String name, int age,
        String gender, float income,
        String location, float amount
    ) throws Exception {

        PreparedStatement ps1 = con.prepareStatement(
            "INSERT INTO Customers(name,age,gender,income,location) " +
            "VALUES(?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps1.setString(1, name);
        ps1.setInt(2, age);
        ps1.setString(3, gender);
        ps1.setFloat(4, income);
        ps1.setString(5, location);
        ps1.executeUpdate();

        ResultSet keys = ps1.getGeneratedKeys();
        int newId = 0;
        if(keys.next()) newId = keys.getInt(1);

        PreparedStatement ps2 = con.prepareStatement(
            "INSERT INTO Transactions(customer_id,amount,transaction_date) " +
            "VALUES(?,?,CURDATE())"
        );
        ps2.setInt(1, newId);
        ps2.setFloat(2, amount);
        ps2.executeUpdate();

        System.out.println("New customer inserted: " + name +
                           " | Amount: " + amount);
    }

    // ── Write customers.js from stored procedure ──
    static void writeSegmentsJS(Connection con) throws Exception {

        CallableStatement cs = con.prepareCall("{CALL GetCustomerSegment()}");
        ResultSet rs = cs.executeQuery();

        StringBuilder sb = new StringBuilder();
        sb.append("const customers = [\n");

        while(rs.next()) {
            sb.append("  {name:'").append(rs.getString("name"))
              .append("', spent:").append(rs.getFloat("total_spent"))
              .append(", segment:'").append(rs.getString("Segment"))
              .append("'},\n");
        }

        int last = sb.lastIndexOf(",");
        if(last != -1) sb.deleteCharAt(last);
        sb.append("];\n");

        FileWriter fw = new FileWriter(
            "D:\\CustomerSegmentationProject\\customers.js"
        );
        fw.write(sb.toString());
        fw.close();
        System.out.println("customers.js updated!");
    }

    // ── Write data.js (above avg + trigger logs) ──
    static void writeDataJS(Connection con) throws Exception {

        // Above average spenders
        Statement st1 = con.createStatement();
        ResultSet rs1 = st1.executeQuery(
            "SELECT * FROM AboveAverageSpenders"
        );
        StringBuilder aboveAvg = new StringBuilder();
        aboveAvg.append("const aboveAverage = [\n");
        while(rs1.next()) {
            aboveAvg.append("  {name:'")
                    .append(rs1.getString("name"))
                    .append("', spent:")
                    .append(rs1.getFloat("total_spent"))
                    .append("},\n");
        }
        int c1 = aboveAvg.lastIndexOf(",");
        if(c1 != -1) aboveAvg.deleteCharAt(c1);
        aboveAvg.append("];\n\n");

        // Transaction log
        Statement st2 = con.createStatement();
        ResultSet rs2 = st2.executeQuery(
            "SELECT c.name, tl.amount, tl.log_time, tl.note " +
            "FROM TransactionLog tl " +
            "JOIN Customers c ON tl.customer_id = c.customer_id"
        );
        StringBuilder txnLog = new StringBuilder();
        txnLog.append("const transactionLog = [\n");
        while(rs2.next()) {
            txnLog.append("  {name:'").append(rs2.getString("name"))
                  .append("', amount:").append(rs2.getFloat("amount"))
                  .append(", time:'").append(rs2.getTimestamp("log_time"))
                  .append("', note:'").append(rs2.getString("note"))
                  .append("'},\n");
        }
        int c2 = txnLog.lastIndexOf(",");
        if(c2 != -1) txnLog.deleteCharAt(c2);
        txnLog.append("];\n\n");

        // First purchase log
        Statement st3 = con.createStatement();
        ResultSet rs3 = st3.executeQuery(
            "SELECT c.name, fp.first_purchase_date, fp.note " +
            "FROM FirstPurchaseLog fp " +
            "JOIN Customers c ON fp.customer_id = c.customer_id"
        );
        StringBuilder firstLog = new StringBuilder();
        firstLog.append("const firstPurchaseLog = [\n");
        while(rs3.next()) {
            firstLog.append("  {name:'").append(rs3.getString("name"))
                    .append("', date:'")
                    .append(rs3.getDate("first_purchase_date"))
                    .append("', note:'").append(rs3.getString("note"))
                    .append("'},\n");
        }
        int c3 = firstLog.lastIndexOf(",");
        if(c3 != -1) firstLog.deleteCharAt(c3);
        firstLog.append("];\n");

        FileWriter fw = new FileWriter(
            "D:\\CustomerSegmentationProject\\data.js"
        );
        fw.write(aboveAvg.toString());
        fw.write(txnLog.toString());
        fw.write(firstLog.toString());
        fw.close();
        System.out.println("data.js updated!");
    }

    // ── Parse form data ──
    static Map<String,String> parseParams(String body) throws Exception {
        Map<String,String> map = new HashMap<>();
        for(String pair : body.split("&")) {
            String[] kv = pair.split("=");
            if(kv.length == 2) {
                map.put(
                    URLDecoder.decode(kv[0], "UTF-8"),
                    URLDecoder.decode(kv[1], "UTF-8")
                );
            }
        }
        return map;
    }
}
