CREATE TABLE service_io_logs (
                                 id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_email        VARCHAR(255),
                                 http_method       VARCHAR(10)  NOT NULL,
                                 path              VARCHAR(500) NOT NULL,
                                 service_name      VARCHAR(255),
                                 request_body      TEXT,
                                 response_body     TEXT,
                                 status_code       INTEGER,
                                 status            VARCHAR(20)  NOT NULL,
                                 error_source      VARCHAR(1000),
                                 requested_at      TIMESTAMP    NOT NULL,
                                 responded_at      TIMESTAMP,
                                 duration_ms       BIGINT
);

CREATE INDEX idx_service_io_logs_requested_at ON service_io_logs (requested_at);
CREATE INDEX idx_service_io_logs_user_email   ON service_io_logs (user_email);
CREATE INDEX idx_service_io_logs_status       ON service_io_logs (status);
CREATE INDEX idx_service_io_logs_service_name ON service_io_logs (service_name);