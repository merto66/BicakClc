-- Migration script to add missing fields to invoices table
-- Run this if your existing database is missing these columns

-- Add discount_amount column if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoices]') AND name = 'discount_amount')
BEGIN
    ALTER TABLE [dbo].[invoices] ADD [discount_amount] [decimal](15, 2) NOT NULL DEFAULT(0);
END
GO

-- Add labor_cost_amount column if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoices]') AND name = 'labor_cost_amount')
BEGIN
    ALTER TABLE [dbo].[invoices] ADD [labor_cost_amount] [decimal](15, 2) NOT NULL DEFAULT(0);
END
GO

-- Add total_quantity column if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoices]') AND name = 'total_quantity')
BEGIN
    ALTER TABLE [dbo].[invoices] ADD [total_quantity] [int] NOT NULL DEFAULT(0);
END
GO

-- Add company_id column if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoices]') AND name = 'company_id')
BEGIN
    ALTER TABLE [dbo].[invoices] ADD [company_id] [int] NULL;
END
GO

-- Ensure discount_percentage exists (should already be there from create script)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoices]') AND name = 'discount_percentage')
BEGIN
    ALTER TABLE [dbo].[invoices] ADD [discount_percentage] [decimal](5, 2) NOT NULL DEFAULT(0);
END
GO

-- Add labor_cost column to invoice_items table for row-based labor costs
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[invoice_items]') AND name = 'labor_cost')
BEGIN
    ALTER TABLE [dbo].[invoice_items] ADD [labor_cost] [decimal](10, 2) NOT NULL DEFAULT(0);
END
GO

-- Index on customers.company_name for fast autocomplete search (optional, run if customers table exists)
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'customers')
   AND NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_customers_company_name' AND object_id = OBJECT_ID(N'[dbo].[customers]'))
BEGIN
    CREATE NONCLUSTERED INDEX [IX_customers_company_name] ON [dbo].[customers] ([company_name] ASC);
END
GO

PRINT 'Migration completed successfully';
