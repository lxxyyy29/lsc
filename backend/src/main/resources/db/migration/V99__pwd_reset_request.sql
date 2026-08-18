-- 密码重置申请：小程序端用户忘记密码提交申请（账号+注册手机号校验），
-- web 管理员收到通知后一键重置（新密码=手机号后6位），由管理员线下转达用户。
CREATE TABLE IF NOT EXISTS pwd_reset_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '申请重置密码的用户ID',
    account VARCHAR(64) NOT NULL COMMENT '账号（冗余，便于列表展示）',
    phone VARCHAR(32) NOT NULL COMMENT '申请时填写的手机号（须与注册手机号一致）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待处理/APPROVED已重置/REJECTED已驳回',
    remark VARCHAR(255) DEFAULT NULL COMMENT '处理备注/驳回原因',
    handled_by BIGINT DEFAULT NULL COMMENT '处理人（web管理员）',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_status (user_id, status),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '密码重置申请';

-- 新增"密码重置处理"权限，授予超级管理员与普通管理员（幂等）
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, status)
SELECT 'api:password-reset:handle', '密码重置申请处理', 'API', 'WEB', 'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:password-reset:handle');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR')
  AND p.permission_code = 'api:password-reset:handle'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
