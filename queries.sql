DROP TABLE logs;
DROP TABLE debtors;
DROP TABLE users;

CREATE TABLE users (
  id SERIAL,
  chat_id BIGINT NOT NULL,
  username VARCHAR NOT NULL,
  first_name VARCHAR,
  last_name VARCHAR,
  default_lang VARCHAR,
  default_currency VARCHAR,
  created TIMESTAMP NOT NULL,
  updated TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE debtors (
  id SERIAL,
  user_id BIGINT NOT NULL,
  chat_id BIGINT NOT NULL,
  name VARCHAR NOT NULL,
  total_amount DECIMAL NOT NULL,
  created TIMESTAMP NOT NULL,
  updated TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
 );

CREATE TABLE logs (
  id SERIAL,
  debtor_id BIGINT NOT NULL,
  credit DECIMAL,
  debit DECIMAL,
  created TIMESTAMP NOT NULL,
  comment VARCHAR NOT NULL,
  type VARCHAR NOT NULL,
  currency VARCHAR,
  PRIMARY KEY (id),
  CONSTRAINT fk_debtor FOREIGN KEY (debtor_id) REFERENCES debtors(id)
);

