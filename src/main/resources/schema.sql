create table if not exists subject(
    id int GENERATED BY DEFAULT AS IDENTITY,
    title text not null,
    federal_district text not null,
    type_of_sport_ids integer[],
    participant_ids integer[]
);

create table if not exists type_of_sport(
    id int GENERATED BY DEFAULT AS IDENTITY,
    title text not null,
    season text,
    sport_filter_type text,
    subject_ids integer[],
    discipline_ids integer[]
);

create table if not exists discipline(
    id int GENERATED BY DEFAULT AS IDENTITY,
    title text not null,
    type_of_sport_id int,
    age_group_ids integer[]
);

create table if not exists age_group(
    id int GENERATED BY DEFAULT AS IDENTITY,
    title text not null,
    min_age int not null,
    max_age int not null,
    discipline_id int,
    qualification_ids integer[]
);

create table if not exists qualification(
    id int GENERATED BY DEFAULT AS IDENTITY,
    category text not null,
    age_group_id int,
    participant_id int
);

create table if not exists participant(
    id int GENERATED BY DEFAULT AS IDENTITY,
    name text,
    middle_name text,
    lastname text not null,
    birthday date not null,
    subject_ids integer[],
    qualification_ids integer[]
);