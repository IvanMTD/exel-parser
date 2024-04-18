create table if not exists ekp(
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    ekp text,
    num text,
    title text,
    description text,
    status text,
    beginning date,
    ending date,
    category text,
    location text,
    organization text,
    sport_id bigint,
    discipline_ids bigint[],
    logo bigint,
    image bigint,
    s float,
    d float
);

create table if not exists sport_object(
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    title text,
    location text,
    address text,
    register_date date,
    url text,
    s float,
    d float,
    logo_id bigint,
    image_ids bigint[]
);

create table if not exists sport(
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    title text,
    description text,
    season text,
    sport_status text,
    discipline_ids bigint[]
);

create table if not exists discipline(
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    title text,
    description text,
    sport_id bigint
);

create table if not exists app_user(
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    username text not null unique,
    password text not null,
    firstname text,
    lastname text,
    email text unique,
    birthday date,
    placed_at date,
    avatar_id text,
    oauth_id text,
    role text
);

create table if not exists minio_file (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    mid bigint,
    uid text,
    name text,
    type text,
    e_tag text,
    bucket text,
    path text,
    minio_url text,
    file_size float
);

create table if not exists school (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    name text,
    ogrn bigint,
    index int,
    address text,
    url text,
    s float,
    d float,

    subject text,

    logo_id bigint,
    photo_id bigint
);