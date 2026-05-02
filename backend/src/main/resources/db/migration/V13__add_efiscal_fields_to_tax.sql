ALTER TABLE tax
    ADD COLUMN efiscal_taxlabel  VARCHAR(1),
    ADD COLUMN efiscal_taxname   VARCHAR(22),
    ADD COLUMN efiscal_taxprefix VARCHAR(22);
