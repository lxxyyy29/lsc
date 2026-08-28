-- V110: 菜单改名——工单中心→已完成工单、审核中心→异常工单
UPDATE sys_permission SET permission_name = '已完成工单' WHERE permission_code = 'web:menu:work-orders' AND permission_name = '工单中心';
UPDATE sys_permission SET permission_name = '异常工单' WHERE permission_code = 'web:menu:audits' AND permission_name = '审核中心';
