CREATE TABLE IF NOT EXISTS logs (
  id SERIAL,
  debtor_id BIGINT NOT NULL,
  credit DECIMAL,
  debit DECIMAL,
  created TIMESTAMP NOT NULL,
  comment VARCHAR NOT NULL,
  type VARCHAR NOT NULL,
  currency VARCHAR,
  PRIMARY KEY (id),
  CONSTRAINT fk_debtor FOREIGN KEY (debtor_id) REFERENCES debtors(id) ON DELETE CASCADE
);