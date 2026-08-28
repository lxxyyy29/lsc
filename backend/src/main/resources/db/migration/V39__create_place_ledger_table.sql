-- =============================================================================
-- V39: create place ledger table (real data from Bajiaowo community)
-- =============================================================================

CREATE TABLE IF NOT EXISTS cmn_place_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_category VARCHAR(32) NOT NULL COMMENT 'SMALL_SHOP/SMALL_WORKSHOP/RENTAL_HOUSE/INDUSTRIAL_PARK/RESIDENTIAL/OTHER',
    place_name VARCHAR(200) NOT NULL COMMENT 'place/company name',
    address VARCHAR(500) COMMENT 'address',
    area_sqm DECIMAL(10,2) COMMENT 'building area (sqm)',
    responsible_person VARCHAR(50) COMMENT 'responsible person name',
    responsible_phone VARCHAR(30) COMMENT 'responsible person phone',
    party_cadre VARCHAR(50) COMMENT 'community party cadre',
    party_cadre_phone VARCHAR(30) COMMENT 'party cadre phone',
    fire_inspector VARCHAR(50) COMMENT 'fire inspector',
    fire_inspector_phone VARCHAR(30) COMMENT 'fire inspector phone',
    remark VARCHAR(500) COMMENT 'remark',
    extra_data JSON COMMENT 'type-specific fields as JSON',
    source_sheet VARCHAR(100) COMMENT 'source sheet name',
    import_batch VARCHAR(32) COMMENT 'import batch',
    grid_id BIGINT COMMENT 'grid id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (place_category),
    INDEX idx_grid (grid_id),
    INDEX idx_name (place_name),
    INDEX idx_address (address),
    INDEX idx_import_batch (import_batch)
) COMMENT='place ledger - real data (different from cmn_place test table)' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
