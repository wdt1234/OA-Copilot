CREATE TABLE IF NOT EXISTS sql_history (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    prompt      TEXT    NOT NULL,
    sql_result  TEXT    NOT NULL,
    is_pinned   INTEGER NOT NULL DEFAULT 0,
    create_time TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS dee_history (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    template_type  TEXT    NOT NULL,
    description    TEXT    NOT NULL,
    result_json    TEXT    NOT NULL,
    is_pinned      INTEGER NOT NULL DEFAULT 0,
    create_time    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS field_mapping_history (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    form_id      TEXT    NOT NULL,
    input_fields TEXT    NOT NULL,
    result_json  TEXT    NOT NULL,
    is_pinned    INTEGER NOT NULL DEFAULT 0,
    create_time  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS data_dictionary (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    form_code     TEXT    NOT NULL UNIQUE,
    form_name     TEXT    NOT NULL,
    table_name    TEXT    NOT NULL,
    dictionary_json TEXT  NOT NULL,
    raw_text      TEXT,
    is_pinned     INTEGER NOT NULL DEFAULT 0,
    create_time   TEXT    NOT NULL,
    update_time   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS sql_cache (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    cache_key     TEXT    NOT NULL UNIQUE,
    prompt        TEXT    NOT NULL,
    form_code     TEXT,
    sql_result    TEXT    NOT NULL,
    create_time   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS sql_task (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id       TEXT    NOT NULL UNIQUE,
    prompt        TEXT    NOT NULL,
    form_code     TEXT,
    status        TEXT    NOT NULL DEFAULT 'PENDING',
    sql_result    TEXT,
    error_message TEXT,
    create_time   TEXT    NOT NULL,
    update_time   TEXT    NOT NULL
);
