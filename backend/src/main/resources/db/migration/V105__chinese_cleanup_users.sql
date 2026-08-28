-- V105: 数据清洗补充——测试账号真实姓名中文化（登录账号 username 按需求允许英文，不处理）
UPDATE sys_user SET real_name = '权限联调测试' WHERE real_name = 'RBAC联调测试';
UPDATE sys_user SET real_name = '李晓宇' WHERE real_name = 'lxy';
