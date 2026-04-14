CREATE DATABASE customer_segmentation;
USE customer_segmentation;
CREATE TABLE Customers(
customer_id INT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(50),
age INT,
gender VARCHAR(10),
income FLOAT,
location VARCHAR(50)
);
CREATE TABLE Transactions(
transaction_id INT PRIMARY KEY AUTO_INCREMENT,
customer_id INT,
amount FLOAT,
transaction_date DATE,
FOREIGN KEY(customer_id) REFERENCES Customers(customer_id)
);
INSERT INTO Customers(name,age,gender,income,location) VALUES
('Raj',30,'Male',50000,'Delhi'),
('Anu',25,'Female',60000,'Mumbai'),
('Ravi',40,'Male',80000,'Hyderabad'),
('Priya',28,'Female',45000,'Chennai'),
('Arjun',35,'Male',70000,'Bangalore'),
('Sneha',27,'Female',52000,'Pune'),
('Kiran',32,'Male',48000,'Kolkata'),
('Meena',29,'Female',55000,'Ahmedabad'),
('Vikram',45,'Male',90000,'Delhi'),
('Neha',26,'Female',47000,'Mumbai');
INSERT INTO Transactions(customer_id,amount,transaction_date) VALUES
(1,2500,'2025-04-01'),
(1,4000,'2025-04-10'),
(2,3000,'2025-04-02'),
(2,1500,'2025-04-11'),
(3,7000,'2025-04-03'),
(3,2000,'2025-04-12'),
(4,1200,'2025-04-04'),
(4,900,'2025-04-13'),
(5,8000,'2025-04-05'),
(5,3500,'2025-04-14'),
(6,2200,'2025-04-06'),
(6,1800,'2025-04-15'),
(7,500,'2025-04-07'),
(7,700,'2025-04-16'),
(8,2600,'2025-04-08'),
(8,1400,'2025-04-17'),
(9,9000,'2025-04-09'),
(9,3000,'2025-04-18'),
(10,1100,'2025-04-10'),
(10,900,'2025-04-19');
CREATE VIEW CustomerSpending AS
SELECT 
c.customer_id,
c.name,
SUM(t.amount) AS total_spent
FROM Customers c
JOIN Transactions t
ON c.customer_id = t.customer_id
GROUP BY c.customer_id;
DELIMITER $$

CREATE PROCEDURE GetCustomerSegment()
BEGIN
SELECT
name,
total_spent,
CASE
WHEN total_spent > 8000 THEN 'High Value'
WHEN total_spent BETWEEN 3000 AND 7999 THEN 'Medium Value'
ELSE 'Low Value'
END AS Segment
FROM CustomerSpending;
END $$

DELIMITER ;
