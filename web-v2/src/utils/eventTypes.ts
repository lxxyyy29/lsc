// 事件类型中英文映射
export const EVENT_TYPES: Record<string, string> = {
  // 英文代码 → 中文
  ROAD: '道路损坏',
  LIGHT: '路灯故障',
  PIPE: '管道破损',
  ENV: '环境卫生',
  ENVIRONMENT: '环境卫生',
  SAFE: '安全隐患',
  SAFETY: '安全隐患',
  NOISE: '噪音扰民',
  OTHER: '其他问题',
  FIRE: '消防安全',
  ILLEGAL_BUILDING: '违章建筑',
  PUBLIC_SAFETY: '公共安全',
  DRONE_ALARM: '无人机告警',
  COMPLAINT: '市民投诉',
  REPAIR: '物业报修',
  HEALTH: '卫生事件',
  LOW_INCOME: '低保户',
  // 中文 → 中文（直接返回）
  '市容环境': '市容环境',
  '消防安全': '消防安全',
  '矛盾纠纷': '矛盾纠纷',
  '安全生产': '安全生产',
  '民生诉求': '民生诉求',
  '防汛防台风': '防汛防台风',
  '违建': '违建',
  '其他': '其他',
};

/**
 * 获取事件类型的中文标签
 */
export function getEventTypeName(type: string): string {
  if (!type) return '-';
  return EVENT_TYPES[type] || EVENT_TYPES[type.toUpperCase()] || type;
}

// 户籍类型中英文映射
export const HOUSEHOLD_TYPES: Record<string, string> = {
  LOCAL: '本地户籍',
  NON_LOCAL: '外地户籍',
  FLOATING: '流动人口',
  LOW_INCOME: '低保户',
  SPECIAL_CARE: '优抚对象',
  OTHER: '其他',
};

/**
 * 获取户籍类型的中文标签
 */
export function getHouseholdTypeName(type: string): string {
  if (!type) return '-';
  return HOUSEHOLD_TYPES[type] || HOUSEHOLD_TYPES[type.toUpperCase()] || type;
}

/**
 * 获取所有事件类型选项（用于下拉选择）
 */
export function getEventTypeOptions(): { value: string; label: string }[] {
  return [
    { value: '市容环境', label: '市容环境' },
    { value: '消防安全', label: '消防安全' },
    { value: '矛盾纠纷', label: '矛盾纠纷' },
    { value: '安全生产', label: '安全生产' },
    { value: '民生诉求', label: '民生诉求' },
    { value: '防汛防台风', label: '防汛防台风' },
    { value: '违建', label: '违建' },
    { value: '其他', label: '其他' },
  ];
}
