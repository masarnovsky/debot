CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNIQUE NOT NULL,
  username VARCHAR,
  first_name VARCHAR,
  last_name VARCHAR,
  is_bot BOOLEAN NOT NULL DEFAULT FALSE,
  default_lang VARCHAR,
  default_currency VARCHAR,
  created TIMESTAMP NOT NULL,
  updated TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);