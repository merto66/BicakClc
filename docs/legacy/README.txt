BicakClc Invoice Management System - README
===========================================

Overview
--------
BicakClc is a Java Swing-based desktop application for managing invoices, products, customers, and related business operations. It uses SQL Server for data persistence and supports exporting invoice data to Excel.

Main Features
-------------
- Product Management: Add, edit, and view products and categories.
- Invoice Creation: Generate invoices with dynamic price calculations, discounts, and labor costs.
- Invoice Viewing: List, filter, and view invoice details. Export individual invoices or all invoices to Excel.
- Customer Management: Manage customer (company) records.
- Favorites: Mark and manage favorite products.

Project Structure
-----------------
- src/main/java/com/bicakclc/
    - model/      (Data models: Invoice, InvoiceItem, Product, Customer, etc.)
    - dao/        (Data access: InvoiceDAO, ProductDAO, etc.)
    - service/    (Business logic: InvoiceService, ProductService, etc.)
    - ui/         (Swing UI panels: MainWindow, InvoicePanel, InvoiceViewPanel, etc.)
    - util/       (Utilities: DatabaseConnection, etc.)
- src/main/resources/sql/
    - create_invoice_tables.sql   (Database schema for invoices and items)
- config.properties              (Database connection settings)
- lib/                           (All required JAR libraries)

Dependencies (lib/ folder)
-------------------------
- poi-5.2.3.jar
- poi-ooxml-5.2.3.jar
- poi-ooxml-full-5.2.3.jar
- ooxml-schemas-1.4.jar
- xmlbeans-5.1.1.jar
- commons-io-2.11.0.jar
- commons-collections4-4.4.jar
- commons-compress-1.21.jar
- curvesapi-1.07.jar
- log4j-api-2.17.2.jar
- log4j-core-2.17.2.jar
- mssql-jdbc-12.10.1.jre11.jar (SQL Server JDBC driver)

Setup & Running
---------------
1. **Database:**
   - Install Microsoft SQL Server (Express or Standard).
   - Run `src/main/resources/sql/create_invoice_tables.sql` to create the required tables.
2. **Configuration:**
   - Edit `config.properties` to set your database connection string (see sample in file).
   - For Windows Authentication, ensure the correct `mssql-jdbc_auth-*.dll` is in your Java bin directory.
3. **Libraries:**
   - All required JARs are in the `lib/` folder. Add them to your project's classpath (not modulepath).
4. **Build & Run:**
   - Open the project in Eclipse or your preferred IDE.
   - Run `main.java.com.bicakclc.main.BicakClcApplication`.

Usage Notes
-----------
- **MainWindow** contains tabs for Product Management, Invoice Creation, Invoice Viewing, and Customer Management.
- **Export to Excel:**
  - In the Invoice Viewing panel, you can export all invoices or a single invoice (with details) to Excel using the provided buttons.
- **Windows Authentication:**
  - If using Windows Authentication, place the correct `mssql-jdbc_auth-*.dll` in your JDK's `bin` directory.
  - See Microsoft documentation for details.
- **Troubleshooting:**
  - If you get `NoClassDefFoundError` or `ClassNotFoundException`, check that all JARs are in the classpath.
  - For database errors, verify your SQL Server is running and the config is correct.

Contact
-------
For questions or issues, contact the project maintainer Mert.

