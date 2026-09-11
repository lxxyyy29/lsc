#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为测试用例的步骤列添加前端导航界面名称"""

import re
import sys

# API 路径 -> (导航分组, 前端页面名称) 映射
NAV_MAP = {
    # 认证
    '/api/auth/login': ('首页概览', '登录页'),
    '/api/auth/me': ('系统设置', '个人中心'),
    '/api/auth/logout': ('系统设置', '退出登录'),
    '/api/h5/auth/login': ('H5移动端', '网格员登录'),
    '/api/h5/auth/me': ('H5移动端', '工作台'),
    # 事件
    '/api/events?': ('事件工单', '事件闭环处置'),
    '/api/events?': ('事件工单', '事件闭环处置'),
    '/api/events/': ('事件工单', '事件详情'),
    '/api/events': ('事件工单', '事件闭环处置'),
    # 工单
    '/api/work-orders': ('事件工单', '已完成工单'),
    '/api/audits': ('事件工单', '事件审核'),
    '/api/abnormal-orders': ('事件工单', '异常工单'),
    # H5 工单
    '/api/h5/workbench': ('H5移动端', '工作台'),
    '/api/h5/work-orders': ('H5移动端', '我的工单'),
    '/api/h5/work-order': ('H5移动端', '工单详情'),
    # 社区网格
    '/api/community/grids': ('网格治理', 'GIS网格可视化'),
    '/api/community/population': ('基础台账', '实有人口库'),
    '/api/community/buildings': ('基础台账', '房屋/出租屋库'),
    '/api/community/places': ('基础台账', '场所资源库'),
    '/api/community/org-members': ('网格治理', '组织人员'),
    '/api/community/resident-reports': ('居民服务', '居民上报'),
    '/api/community/policy-resources': ('居民服务', '政策资源'),
    '/api/community/export': ('基础台账', '台账导出'),
    '/api/community/import': ('基础台账', '台账导入'),
    '/api/community/dashboard': ('首页概览', '数据看板'),
    # 巡查
    '/api/community/patrol-tasks': ('巡查防控', '网格巡查'),
    '/api/community/patrol-records': ('巡查防控', '巡查记录'),
    # 安全
    '/api/safety': ('巡查防控', '安全检查'),
    '/api/parking': ('巡查防控', '停车管理'),
    # 党建
    '/api/party': ('智慧应用', '智慧党建'),
    # 无人机
    '/api/drone': ('巡查防控', '无人机管理'),
    '/api/video': ('巡查防控', '视频监控'),
    # 考核
    '/api/assessment': ('数据决策', '考核研判'),
    '/api/reports': ('数据决策', '数据报表'),
    '/api/audit-logs': ('数据决策', '审计日志'),
    # 系统
    '/api/system/users': ('系统设置', '账号管理'),
    '/api/system/roles': ('系统设置', '角色管理'),
    '/api/system/menus': ('系统设置', '菜单管理'),
    '/api/system/permissions': ('系统设置', '权限管理'),
    '/api/system/dicts': ('系统设置', '字典管理'),
    # 台账
    '/api/ledger': ('基础台账', '场所台账'),
    # 通知消息
    '/api/notifications': ('首页概览', '通知中心'),
    '/api/messaging': ('信息互通', '实时聊天'),
    # 注册
    '/api/registration': ('居民服务', '居民注册'),
    # 上传
    '/api/upload': ('通用', '文件上传'),
    '/api/media': ('通用', '媒体文件'),
    '/api/export': ('通用', '数据导出'),
    # 信息互通
    '/api/integration': ('信息互通', '外部系统'),
    '/api/integrations': ('信息互通', '告警回调'),
    # 流程
    '/api/processes': ('系统设置', '流程模板'),
    '/api/dispatch-rules': ('事件工单', '智能派单规则'),
    '/api/emergency': ('巡查防控', '应急调度'),
    '/api/mosquito': ('巡查防控', '蚊媒管控'),
    '/api/vehicle': ('巡查防控', '车辆轨迹'),
    # 业务管理
    '/api/areas': ('网格治理', '辖区管理'),
    '/api/merchants': ('基础台账', '商户管理'),
    '/api/mobile-vendors': ('基础台账', '摊贩管理'),
    '/api/violation-areas': ('巡查防控', '违禁区域'),
    # H5 业务
    '/api/h5/areas': ('H5移动端', '辖区'),
    '/api/h5/merchants': ('H5移动端', '商户'),
    '/api/h5/mobile-vendors': ('H5移动端', '摊贩'),
}

# 按路径长度降序匹配（优先匹配更具体的路径）
SORTED_NAV = sorted(NAV_MAP.items(), key=lambda x: -len(x[0]))


def find_nav(api_path: str) -> str:
    """根据 API 路径查找前端导航名称"""
    # 去掉 query string
    clean = re.sub(r'\?.*$', '', api_path)
    # 去掉末尾 {id} 占位符
    clean = re.sub(r'/\{[^}]+\}', '', clean)
    # 去掉末尾数字 id
    clean = re.sub(r'/\d+$', '', clean)
    clean = clean.rstrip('/')

    best_match = ''
    best_score = 0

    for pattern, (group, page) in SORTED_NAV:
        pat_clean = re.sub(r'\?.*$', '', pattern)
        pat_clean = re.sub(r'/\{[^}]+\}', '', pat_clean).rstrip('/')

        # 计算匹配分数
        clean_parts = [p for p in clean.split('/') if p]
        pat_parts = [p for p in pat_clean.split('/') if p]

        if not pat_parts or not clean_parts:
            continue

        # 逐段匹配
        score = 0
        for i, (cp, pp) in enumerate(zip(clean_parts, pat_parts)):
            if cp == pp:
                score = i + 1
            else:
                break

        if score > best_score:
            best_score = score
            best_match = f"{group} > {page}"

    return best_match


def process_line(line: str) -> str:
    """处理单行，在 API 后添加导航名称"""
    # 匹配包含 GET/POST/PUT/DELETE 的行
    if not re.search(r'(GET|POST|PUT|DELETE|PATCH)\s+/api/', line):
        return line

    # 分割表格列
    cols = line.split('|')
    if len(cols) < 5:
        return line

    # 步骤列通常是第4列（索引3，因为开头有空列）
    for i in range(1, len(cols) - 1):
        cell = cols[i]
        # 查找 API 调用
        m = re.search(r'((?:GET|POST|PUT|DELETE|PATCH)\s+(/api/[^\s<>,;]+))', cell)
        if m:
            api_call = m.group(1)
            api_path = m.group(2)
            nav = find_nav(api_path)
            if nav and nav not in cell:
                # 在 API 调用后添加导航名称
                cols[i] = cell.replace(api_call, f"{api_call}（{nav}）", 1)
            break  # 每行只处理第一个 API

    return '|'.join(cols)


def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else r'D:\kfkprojuct\测试用例全集.md'
    output_file = sys.argv[2] if len(sys.argv) > 2 else input_file.replace('.md', '_with_nav.md')

    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    processed = []
    for line in lines:
        processed.append(process_line(line.rstrip('\n')))

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(processed))

    print(f"Done: {output_file}")


if __name__ == '__main__':
    main()
