--liquibase formatted sql
--changeset zen.liu:init-2019.11.19
create table store (
                                   id                            integer not null,
                                   status_update_at              timestamp not null,
                                   gender                        integer not null,
                                   type                          varchar(255) not null,
                                   phone                         varchar(255),
                                   identity                      varchar(255) not null,
                                   context                       clob not null,
                                   name                          varchar(255) not null,
                                   comment                       varchar(255) not null,
                                   ref_code                      varchar(255),
                                   age                           varchar(255) not null,
                                   status                        integer not null,
                                   constraint pk_store primary key (id)
);

