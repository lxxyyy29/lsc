-- =============================================================================
-- V37: 修复综合监管大屏菜单 component 字段
-- =============================================================================

UPDATE sys_permission SET component = 'BigScreenView' WHERE permission_code = 'menu:big-screen:view' AND component IS NULL;
