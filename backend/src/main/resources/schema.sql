CREATE TABLE IF NOT EXISTS sql_history (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    prompt      TEXT    NOT NULL,
    sql_result  TEXT    NOT NULL,
    form_code   TEXT,
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

CREATE TABLE IF NOT EXISTS api_doc_history (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    form_code       TEXT,
    form_name       TEXT,
    interface_code  TEXT    NOT NULL,
    interface_name  TEXT    NOT NULL,
    interface_type  TEXT    NOT NULL,
    connection_type TEXT    NOT NULL,
    source_system   TEXT,
    source_contact  TEXT,
    target_contact  TEXT,
    other_notes     TEXT,
    is_pinned       INTEGER NOT NULL DEFAULT 0,
    create_time     TEXT    NOT NULL
);

-- 快捷模板表
CREATE TABLE IF NOT EXISTS shortcut_template (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    category    TEXT    NOT NULL DEFAULT 'sql',
    content     TEXT    NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    create_time TEXT    NOT NULL,
    update_time TEXT    NOT NULL
);

-- 错题库表
CREATE TABLE IF NOT EXISTS error_case (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    category      TEXT    NOT NULL DEFAULT 'sql',
    title         TEXT    NOT NULL,
    symptom       TEXT,
    cause         TEXT,
    solution      TEXT,
    example_wrong TEXT,
    example_correct TEXT,
    tags          TEXT,
    is_pinned     INTEGER NOT NULL DEFAULT 0,
    create_time   TEXT    NOT NULL,
    update_time   TEXT    NOT NULL
);

-- AI配置方案表
CREATE TABLE IF NOT EXISTS ai_config (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    endpoint      TEXT    NOT NULL,
    api_key       TEXT    NOT NULL,
    model         TEXT    NOT NULL,
    timeout       INTEGER NOT NULL DEFAULT 300,
    is_active     INTEGER NOT NULL DEFAULT 0,
    create_time   TEXT    NOT NULL,
    update_time   TEXT    NOT NULL
);
