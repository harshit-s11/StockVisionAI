-- Seed curated demo stock universe (explicitly marked as demo market data)
INSERT INTO stocks (id, symbol, company_name, sector, current_price, previous_close, updated_at) VALUES
(gen_random_uuid(), 'TCS', 'Tata Consultancy Services Ltd.', 'Information Technology', 3850.00, 3810.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'INFY', 'Infosys Ltd.', 'Information Technology', 1620.00, 1605.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'RELIANCE', 'Reliance Industries Ltd.', 'Energy & Conglomerate', 2950.00, 2920.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'HDFCBANK', 'HDFC Bank Ltd.', 'Financial Services', 1480.00, 1475.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'ICICIBANK', 'ICICI Bank Ltd.', 'Financial Services', 1090.00, 1075.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'SBIN', 'State Bank of India', 'Financial Services', 760.00, 752.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'BHARTIARTL', 'Bharti Airtel Ltd.', 'Telecommunication', 1210.00, 1195.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'ITC', 'ITC Ltd.', 'Consumer Goods', 430.00, 428.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'HINDUNILVR', 'Hindustan Unilever Ltd.', 'Consumer Goods', 2420.00, 2400.00, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'LT', 'Larsen & Toubro Ltd.', 'Construction & Engineering', 3620.00, 3580.00, CURRENT_TIMESTAMP);