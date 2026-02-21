-- Fatura ana tablosu
CREATE TABLE [dbo].[invoices](
    [invoice_id] [int] IDENTITY(1,1) NOT NULL,
    [invoice_number] [nvarchar](50) NOT NULL,  -- Fatura numarası
    [company_name] [nvarchar](200) NOT NULL,  -- Firma adı (geçici olarak direkt tabloda tutulacak)
    [company_id] [int] NULL,  -- Müşteri referansı (opsiyonel)
    [invoice_date] [datetime2](7) NOT NULL,
    [quality] [nvarchar](100) NULL,  -- Kalite bilgisi
    [total_amount] [decimal](15, 2) NOT NULL DEFAULT(0),
    [discount_percentage] [decimal](5, 2) NOT NULL DEFAULT(0),
    [discount_amount] [decimal](15, 2) NOT NULL DEFAULT(0),
    [labor_cost_amount] [decimal](15, 2) NOT NULL DEFAULT(0),
    [final_amount] [decimal](15, 2) NOT NULL DEFAULT(0),
    [total_quantity] [int] NOT NULL DEFAULT(0),
    [notes] [nvarchar](500) NULL,  -- Ek notlar için
    [status] [nvarchar](20) NOT NULL DEFAULT('DRAFT'),  -- DRAFT, APPROVED, CANCELLED gibi
    [created_date] [datetime2](7) NOT NULL DEFAULT(getdate()),
    [created_by] [nvarchar](50) NULL,  -- Oluşturan kullanıcı
    [modified_date] [datetime2](7) NULL,
    [modified_by] [nvarchar](50) NULL,  -- Son değiştiren kullanıcı
PRIMARY KEY CLUSTERED 
(
    [invoice_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
    [invoice_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- Fatura kalemleri tablosu
CREATE TABLE [dbo].[invoice_items](
    [item_id] [int] IDENTITY(1,1) NOT NULL,
    [invoice_id] [int] NOT NULL,  -- Fatura referansı
    [product_id] [int] NOT NULL,  -- Ürün referansı
    [parent_item_id] [int] NULL,  -- Alt grup (çelik) için üst kalem referansı
    [price] [decimal](15, 2) NOT NULL DEFAULT(0),
    [cm_value] [decimal](10, 2) NULL,  -- CM değeri (opsiyonel)
    [quantity] [int] NOT NULL DEFAULT(1),
    [total] [decimal](15, 2) NOT NULL DEFAULT(0),
    [is_sub_group] [bit] NOT NULL DEFAULT(0),  -- Alt grup mu?
    [row_number] [int] NOT NULL DEFAULT(0),  -- Sıralama için
    [created_date] [datetime2](7) NOT NULL DEFAULT(getdate()),
PRIMARY KEY CLUSTERED 
(
    [item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

-- Foreign Key tanımlamaları
ALTER TABLE [dbo].[invoice_items] ADD CONSTRAINT [FK_invoice_items_invoices]
FOREIGN KEY([invoice_id]) REFERENCES [dbo].[invoices] ([invoice_id])
GO

ALTER TABLE [dbo].[invoice_items] ADD CONSTRAINT [FK_invoice_items_products]
FOREIGN KEY([product_id]) REFERENCES [dbo].[products] ([product_id])
GO

ALTER TABLE [dbo].[invoice_items] ADD CONSTRAINT [FK_invoice_items_parent]
FOREIGN KEY([parent_item_id]) REFERENCES [dbo].[invoice_items] ([item_id])
GO

-- İndeksler
CREATE NONCLUSTERED INDEX [IX_invoice_items_invoice] ON [dbo].[invoice_items]
(
    [invoice_id] ASC
)
GO

CREATE NONCLUSTERED INDEX [IX_invoice_items_product] ON [dbo].[invoice_items]
(
    [product_id] ASC
)
GO 