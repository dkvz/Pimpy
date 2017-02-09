create table test (id INTEGER PRIMARY KEY, value TEXT NOT NULL DEFAULT 'truc')


-- The true one (need to check it though) :

create table subm_sentences_hist (id INTEGER PRIMARY KEY, sentence TEXT NOT NULL, to_learn INTEGER(1), revised INTEGER(1), timestamp INTEGER DEFAULT 0);
create table word (id INTEGER PRIMARY KEY, word TEXT NOT NULL, CONSTRAINT word_unq UNIQUE (word));
create table sentence (id INTEGER PRIMARY KEY, word_count INTEGER NOT NULL, question INTEGER(1), answer TEXT NOT NULL, answer_context TEXT NULL DEFAULT NULL);
create table sentence_words (
  sentence_id INTEGER NOT NULL,
  word_id INTEGER NOT NULL,
  position INTEGER(2) NOT NULL,
  CONSTRAINT triplet_unq UNIQUE (sentence_id, word_id, position),
  CONSTRAINT sentence_ref FOREIGN KEY (sentence_id) REFERENCES sentence(id) ON DELETE CASCADE,
  CONSTRAINT word_ref FOREIGN KEY (word_id) REFERENCES word(id) ON DELETE CASCADE
);
create table unknown_word (id INTEGER PRIMARY KEY, word TEXT NOT NULL, CONSTRAINT word_unq UNIQUE (word));

create table sentence_words (sentence_id INTEGER NOT NULL, word_id INTEGER NOT NULL, position INTEGER(2) NOT NULL, CONSTRAINT triplet_unq UNIQUE (sentence_id, word_id, position), CONSTRAINT sentence_ref FOREIGN KEY (sentence_id) REFERENCES sentence(id) ON DELETE CASCADE, CONSTRAINT word_ref FOREIGN KEY (word_id) REFERENCES word(id) ON DELETE CASCADE);

alter table sentence add column type INTEGER(2) NOT NULL DEFAULT 0;

create table sentence_type (id INTEGER(2) PRIMARY KEY, name TEXT NOT NULL);
insert into sentence_type values (0, 'Normal');
insert into sentence_type values (1, 'Hello');
insert into sentence_type values (2, 'Closure');
insert into sentence_type values (3, 'Question rebound');
insert into sentence_type values (4, 'Getaround');
insert into sentence_type values (5, 'NotUnderstood');

create table pattern (
  id INTEGER PRIMARY KEY,
  regex TEXT NOT NULL
);

create table pattern_answer (
  id INTEGER PRIMARY KEY,
  pattern_id INTEGER NULL,
  answer TEXT NOT NULL,
  CONSTRAINT patt_fk FOREIGN KEY (pattern_id) REFERENCES pattern(id) ON DELETE CASCADE
);

alter table pattern add column context_vars TEXT;
alter table pattern add column case_s INTEGER(1) NOT NULL DEFAULT 0;


create table db_info (
  author TEXT NOT NULL,
  description TEXT NOT NULL,
  created_timestamp INTEGER DEFAULT 0
);


-------------

drop table db_info;

create table db_info (
  key_id TEXT PRIMARY KEY,
  value TEXT NULL
);

insert into db_info values('author', 'John Van Bouc');
insert into db_info values('description', 'Knowledge base based on various modified online conversation logs');
insert into db_info values('created', '1258554415');
insert into db_info values('screen_name', 'Joe');





Mega queries :
select count(sentence_id) as match_count, sentence_id from sentence_words where word_id = 5 or word_id = 7 or word_id = 6 group by sentence_id order by match_count desc limit 100

select count(sentence_words.sentence_id) as match_count, sentence_words.sentence_id, sentence.word_count, sentence.question, sentence.answer, sentence.answer_context, sentence.type from sentence_words, sentence where sentence_words.sentence_id = sentence.id AND (sentence_words.word_id = 5 or sentence_words.word_id = 7 or sentence_words.word_id = 6) group by sentence_words.sentence_id order by match_count desc limit 100


=> un truc qui a 16 fois le meme mot va donner un gros match_count... Alors que c'est probablement foireux. Faut garder tous les results > et un peu plus < que le word_count de la phrase soumise.