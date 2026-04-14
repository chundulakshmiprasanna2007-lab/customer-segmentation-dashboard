import java.sql.*;
import java.io.*;

public class CustomerSegmentationApp {

    static final String URL = "jdbc:mysql://localhost:3306/customer_segmentation";
    static final String USER = "root";
    static final String PASSWORD = "Prasi@2007";

    public static void main(String[] args) {
        Connection con = null;
        try {
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database Connected Successfully\n");

            showCustomers(con);
            showTransactions(con);
            showCustomerSegments(con);
            showAboveAverageSpenders(con);
            showTransactionLogs(con);
    
            // Write real data to JSON for HTML dashboard
            writeSegmentsToJSON(con);
            writeAllDataToJS(con);
            System.out.println("\nJSON file generated: customers.js");

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                if (con != null && !con.isClosed()) {
                    con.close();
                    System.out.println("Connection closed.");
                }
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    public static void insertNewCustomer(
    Connection con,
    String name, int age, String gender,
    float income, String location, float amount
) throws Exception {

    // Step 1: Insert into Customers table
    PreparedStatement ps1 = con.prepareStatement(
        "INSERT INTO Customers(name,age,gender,income,location) VALUES(?,?,?,?,?)",
        Statement.RETURN_GENERATED_KEYS
    );
    ps1.setString(1, name);
    ps1.setInt(2, age);
    ps1.setString(3, gender);
    ps1.setFloat(4, income);
    ps1.setString(5, location);
    ps1.executeUpdate();

    // Step 2: Get the new customer_id that was just created
    ResultSet keys = ps1.getGeneratedKeys();
    int newCustomerId = 0;
    if(keys.next()) {
        newCustomerId = keys.getInt(1);
    }

    // Step 3: Insert transaction
    // Trigger fires automatically here!
    PreparedStatement ps2 = con.prepareStatement(
        "INSERT INTO Transactions(customer_id,amount,transaction_date) " +
        "VALUES(?,?,CURDATE())"
    );
    ps2.setInt(1, newCustomerId);
    ps2.setFloat(2, amount);
    ps2.executeUpdate();

    System.out.println("New customer inserted: " + name);
    System.out.println("Trigger will auto-log if amount > 5000");
}
    public static void writeAllDataToJS(Connection con) throws Exception {

    // ── Above Average Spenders ──
    Statement st1 = con.createStatement();
    ResultSet rs1 = st1.executeQuery("SELECT * FROM AboveAverageSpenders");

    StringBuilder aboveAvg = new StringBuilder();
    aboveAvg.append("const aboveAverage = [\n");
    while(rs1.next()) {
        aboveAvg.append("  {name:'").append(rs1.getString("name"))
                .append("', spent:").append(rs1.getFloat("total_spent"))
                .append("},\n");
    }
    int c1 = aboveAvg.lastIndexOf(",");
    if(c1 != -1) aboveAvg.deleteCharAt(c1);
    aboveAvg.append("];\n\n");

    // ── Transaction Log (Trigger) ──
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

    // ── First Purchase Log (Trigger) ──
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
                .append("', date:'").append(rs3.getDate("first_purchase_date"))
                .append("', note:'").append(rs3.getString("note"))
                .append("'},\n");
    }
    int c3 = firstLog.lastIndexOf(",");
    if(c3 != -1) firstLog.deleteCharAt(c3);
    firstLog.append("];\n");

    // ── Write everything to data.js ──
    FileWriter fw = new FileWriter("D:\\CustomerSegmentationProject\\data.js");
    fw.write(aboveAvg.toString());
    fw.write(txnLog.toString());
    fw.write(firstLog.toString());
    fw.close();

    System.out.println("data.js generated successfully!");
}
    public static void showCustomers(Connection con) throws Exception {
        System.out.println("===== CUSTOMER DETAILS =====");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM Customers");
        while (rs.next()) {
            System.out.println(
                rs.getInt("customer_id") + " | " +
                rs.getString("name") + " | " +
                rs.getInt("age") + " | " +
                rs.getString("gender") + " | " +
                rs.getFloat("income") + " | " +
                rs.getString("location")
            );
        }
        System.out.println();
    }

    public static void showTransactions(Connection con) throws Exception {
        System.out.println("===== TRANSACTION DETAILS =====");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM Transactions");
        while (rs.next()) {
            System.out.println(
                rs.getInt("transaction_id") + " | " +
                rs.getInt("customer_id") + " | " +
                rs.getFloat("amount") + " | " +
                rs.getDate("transaction_date")
            );
        }
        System.out.println();
    }

    public static void showCustomerSegments(Connection con) throws Exception {
        System.out.println("===== CUSTOMER SEGMENTATION (Stored Procedure) =====");
        CallableStatement cs = con.prepareCall("{CALL GetCustomerSegment()}");
        ResultSet rs = cs.executeQuery();
        while (rs.next()) {
            System.out.println(
                rs.getString("name") + " | " +
                rs.getFloat("total_spent") + " | " +
                rs.getString("Segment")
            );
        }
        System.out.println();
    }

    public static void showAboveAverageSpenders(Connection con) throws Exception {
        System.out.println("===== ABOVE AVERAGE SPENDERS (Nested Query View) =====");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM AboveAverageSpenders");
        while (rs.next()) {
            System.out.println(
                rs.getString("name") + " | " +
                rs.getFloat("total_spent")
            );
        }
        System.out.println();
    }

    public static void showTransactionLogs(Connection con) throws Exception {
        System.out.println("===== TRANSACTION LOG (Trigger Output) =====");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(
            "SELECT tl.log_id, c.name, tl.amount, tl.log_time, tl.note " +
            "FROM TransactionLog tl " +
            "JOIN Customers c ON tl.customer_id = c.customer_id"
        );
        while (rs.next()) {
            System.out.println(
                rs.getInt("log_id") + " | " +
                rs.getString("name") + " | " +
                rs.getFloat("amount") + " | " +
                rs.getTimestamp("log_time") + " | " +
                rs.getString("note")
            );
        }
        System.out.println();
    }

    // KEY METHOD: Writes real MySQL data into customers.js
    // so your HTML dashboard shows live data not static data
    public static void writeSegmentsToJSON(Connection con) throws Exception {
        CallableStatement cs = con.prepareCall("{CALL GetCustomerSegment()}");
        ResultSet rs = cs.executeQuery();

        StringBuilder json = new StringBuilder();
        json.append("const customers = [\n");

        while (rs.next()) {
            String name = rs.getString("name");
            float spent = rs.getFloat("total_spent");
            String segment = rs.getString("Segment");
            json.append("  {name:'").append(name)
                .append("', spent:").append(spent)
                .append(", segment:'").append(segment)
                .append("'},\n");
        }

        // Remove last comma
        int lastComma = json.lastIndexOf(",");
        if (lastComma != -1) json.deleteCharAt(lastComma);

        json.append("];\n");

        // Write to customers.js so HTML reads real data
        FileWriter fw = new FileWriter("D:\\CustomerSegmentationProject\\customers.js");
        fw.write(json.toString());
        fw.close();
    }
}
