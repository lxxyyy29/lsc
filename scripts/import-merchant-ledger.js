/**
 * 将场所台账数据导入 biz_merchant 表（与现有商户管理模块打通）
 * 用法: node scripts/import-merchant-ledger.js > /tmp/merchant_import.sql
 */
const xlsx = require('xlsx');
const path = require('path');

const FILE_PATH = path.join(__dirname, '..', '..', '..', '拔蛟窝社区各类场所台账.xlsx');
const IMPORT_BATCH = '20250224';

function getCategory(sheetName) {
  if (sheetName.includes('小档口') || sheetName.includes('小娱乐')) return 'SMALL_SHOP';
  if (sheetName.includes('小作坊')) return 'SMALL_WORKSHOP';
  if (sheetName.includes('出租屋')) return 'RENTAL_HOUSE';
  if (sheetName.includes('工业园')) return 'INDUSTRIAL_PARK';
  if (sheetName.includes('住宅')) return 'RESIDENTIAL';
  if (sheetName.includes('其他')) return 'OTHER';
  return 'OTHER';
}

function parsePhone(combined) {
  if (!combined) return { name: '', phone: '' };
  const str = String(combined).trim();
  // Handle newline-separated name and phone: "庾伟超\n13316651666"
  const lines = str.split(/[\n\r]+/).map(s => s.trim()).filter(Boolean);
  if (lines.length >= 2) {
    const phone = lines.find(l => /^\d{7,11}$/.test(l));
    const name = lines.find(l => !/^\d{7,11}$/.test(l)) || lines[0];
    return { name: name, phone: phone || '' };
  }
  // Handle concatenated name+phone: "庾玉娟13712999969"
  const match = str.match(/^(.+?)(\d{7,11})$/);
  if (match) return { name: match[1].trim(), phone: match[2] };
  return { name: str, phone: '' };
}

function escapeSql(val) {
  if (val === null || val === undefined || val === '') return 'NULL';
  const str = String(val).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
  return `'${str}'`;
}

const wb = xlsx.readFile(FILE_PATH);
const allSql = [];

allSql.push('-- 导入场所台账数据到 biz_merchant 表');
allSql.push('-- 来源: 拔蛟窝社区各类场所台账.xlsx (2025年2月24日填报)');
allSql.push(`-- 导入批次: ${IMPORT_BATCH}`);
allSql.push('');

for (const sheetName of wb.SheetNames) {
  const ws = wb.Sheets[sheetName];
  const data = xlsx.utils.sheet_to_json(ws, { header: 1 });
  const category = getCategory(sheetName);

  allSql.push(`-- === ${sheetName} (${category}) ===`);

  for (let i = 3; i < data.length; i++) {
    const row = data[i];
    if (!row || !row[1]) continue;

    const name = String(row[1] || '').trim();
    if (!name || name === '拔蛟窝社区无工业园') continue;

    const address = row[2] ? String(row[2]).trim() : '';
    const areaRaw = row[5] || row[4] || null;
    const area = areaRaw ? Number(areaRaw) || null : null;

    let responsible = '', responsiblePhone = '';
    let partyCadre = '', fireInspector = '';

    if (category === 'SMALL_SHOP') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name;
    } else if (category === 'SMALL_WORKSHOP') {
      const cp = parsePhone(row[6]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[7]);
      partyCadre = pc.name;
      const fc = parsePhone(row[8]);
      fireInspector = fc.name;
    } else if (category === 'RENTAL_HOUSE') {
      responsible = row[6] ? String(row[6]).trim() : '';
      responsiblePhone = row[7] ? String(row[7]).trim() : '';
      const pc = parsePhone(row[8]);
      partyCadre = pc.name;
      const fc = parsePhone(row[9]);
      fireInspector = fc.name;
    } else if (category === 'INDUSTRIAL_PARK') {
      const cp = parsePhone(row[7]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[8]);
      partyCadre = pc.name;
      const fc = parsePhone(row[9]);
      fireInspector = fc.name;
    } else if (category === 'RESIDENTIAL') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name;
    } else if (category === 'OTHER') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name;
    }

    // Build remark with extra info
    const remarkParts = [];
    if (address) remarkParts.push('地址: ' + address);
    if (area) remarkParts.push('面积: ' + area + '㎡');
    if (partyCadre) remarkParts.push('两委: ' + partyCadre);
    if (fireInspector) remarkParts.push('消防: ' + fireInspector);
    remarkParts.push('类别: ' + category);
    remarkParts.push('批次: ' + IMPORT_BATCH);
    const remark = remarkParts.join(' | ');

    const sql = `INSERT INTO biz_merchant (merchant_name, legal_person_name, legal_person_phone, remark, status, created_at, updated_at) VALUES (${escapeSql(name)}, ${escapeSql(responsible)}, ${escapeSql(responsiblePhone)}, ${escapeSql(remark)}, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);`;

    allSql.push(sql);
  }
  allSql.push('');
}

console.log(allSql.join('\n'));
