CREATE TABLE IF NOT EXISTS images (
  id SERIAL,
  url VARCHAR UNIQUE NOT NULL,
  PRIMARY KEY (id)
);

INSERT INTO images(url) VALUES('https://www.meme-arsenal.com/memes/701b9306b31111a5e7ea44a7273aba5e.jpg');
INSERT INTO images(url) VALUES('https://cs4.pikabu.ru/images/big_size_comm/2016-06_6/1467293598180259615.jpg');
INSERT INTO images(url) VALUES('http://risovach.ru/upload/2014/09/mem/fraj_60159268_orig_.jpg');
INSERT INTO images(url) VALUES('http://lib.sibadi.org/wp-content/uploads/2018/10/Screenshot_1-1.jpg');
INSERT INTO images(url) VALUES('https://2img.net/h/risovach.ru/upload/2013/10/mem/gost_33298295_orig_.jpg');
INSERT INTO images(url) VALUES('http://memesmix.net/media/created/p3ad9w.jpg');
INSERT INTO images(url) VALUES('http://risovach.ru/upload/2014/01/mem/bender_41188932_orig_.jpg');
INSERT INTO images(url) VALUES('https://www.meme-arsenal.com/memes/df6f5c7803b5a86ee64e80f0c21fb9d2.jpg');
INSERT INTO images(url) VALUES('http://memesmix.net/media/created/ffe4bl.jpg');