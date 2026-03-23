import java.sql.*;

public class CustomerSegmentationApp {

static final String URL="jdbc:mysql://localhost:3306/customer_segmentation";
static final String USER="root";
static final String PASSWORD="Prasi@2007";

public static void main(String[] args){

try{
Connection con=DriverManager.getConnection(URL,USER,PASSWORD);

System.out.println("Database Connected Successfully\n");

showCustomers(con);
showTransactions(con);
showCustomerSegments(con);

con.close();

}catch(Exception e){
System.out.println(e);
}

}

public static void showCustomers(Connection con) throws Exception{

System.out.println("===== CUSTOMER DETAILS =====");

Statement st=con.createStatement();
ResultSet rs=st.executeQuery("SELECT * FROM Customers");

while(rs.next()){

System.out.println(
rs.getInt("customer_id")+" | "+
rs.getString("name")+" | "+
rs.getInt("age")+" | "+
rs.getString("gender")+" | "+
rs.getFloat("income")+" | "+
rs.getString("location")
);

}

System.out.println();

}

public static void showTransactions(Connection con) throws Exception{

System.out.println("===== TRANSACTION DETAILS =====");

Statement st=con.createStatement();
ResultSet rs=st.executeQuery("SELECT * FROM Transactions");

while(rs.next()){

System.out.println(
rs.getInt("transaction_id")+" | "+
rs.getInt("customer_id")+" | "+
rs.getFloat("amount")+" | "+
rs.getDate("transaction_date")
);

}

System.out.println();

}

public static void showCustomerSegments(Connection con) throws Exception{

System.out.println("===== CUSTOMER SEGMENTATION =====");

CallableStatement cs=con.prepareCall("{CALL GetCustomerSegment()}");

ResultSet rs=cs.executeQuery();

while(rs.next()){

System.out.println(
rs.getString("name")+" | "+
rs.getFloat("total_spent")+" | "+
rs.getString("Segment")
);

}

}

}