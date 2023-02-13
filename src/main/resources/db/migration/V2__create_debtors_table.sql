CREATE TABLE IF NOT EXISTS debtors (
  id SERIAL,
  user_id BIGINT NOT NULL,
  name VARCHAR NOT NULL,
  total_amount DECIMAL NOT NULL,
  created TIMESTAMP NOT NULL,
  updated TIMESTAMP NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
 );