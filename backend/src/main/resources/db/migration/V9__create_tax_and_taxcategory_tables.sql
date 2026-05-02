CREATE TABLE taxcategory (
    taxcategory_id BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000 INCREMENT BY 1) PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    category_type  INTEGER,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_taxcategory_name_active ON taxcategory (LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE tax (
    tax_id           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1000 INCREMENT BY 1) PRIMARY KEY,
    taxcategory_id   BIGINT NOT NULL,
    label            VARCHAR(20) NOT NULL,
    rate             NUMERIC(8, 4) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT fk_tax_taxcategory FOREIGN KEY (taxcategory_id) REFERENCES taxcategory (taxcategory_id)
);

CREATE UNIQUE INDEX uq_tax_taxcategory_label_active ON tax (taxcategory_id, UPPER(label)) WHERE deleted_at IS NULL;
CREATE INDEX idx_tax_taxcategory_id ON tax (taxcategory_id);
