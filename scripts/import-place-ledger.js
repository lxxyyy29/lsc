/**
 * 场所资源台账导入脚本
 * 读取 拔蛟窝社区各类场所台账.xlsx 并生成 SQL 插入语句
 * 用法: node scripts/import-place-ledger.js > /tmp/place_ledger_import.sql
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
  const match = str.match(/^(.+?)(\d{11})$/);
  if (match) return { name: match[1].trim(), phone: match[2] };
  return { name: str, phone: '' };
}

function escapeSql(val) {
  if (val === null || val === undefined || val === '') return 'NULL';
  const str = String(val).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
  return `'${str}'`;
}

function buildExtraData(category, row) {
  const extra = {};
  switch (category) {
    case 'SMALL_SHOP':
      if (row.place_type) extra.place_type = row.place_type;
      break;
    case 'SMALL_WORKSHOP':
      if (row.production_type) extra.production_type = row.production_type;
      if (row.employee_count) extra.employee_count = Number(row.employee_count) || 0;
      break;
    case 'RENTAL_HOUSE':
      if (row.floor_count) extra.floor_count = Number(row.floor_count) || 0;
      if (row.resident_count) extra.resident_count = Number(row.resident_count) || 0;
      break;
    case 'INDUSTRIAL_PARK':
      if (row.is_village_level) extra.is_village_level = row.is_village_level;
      if (row.building_count) extra.building_count = Number(row.building_count) || 0;
      if (row.tenant_count) extra.tenant_count = Number(row.tenant_count) || 0;
      break;
    case 'RESIDENTIAL':
      if (row.building_count) extra.building_count = Number(row.building_count) || 0;
      break;
    case 'OTHER':
      if (row.place_type) extra.place_type = row.place_type;
      break;
  }
  return Object.keys(extra).length > 0 ? JSON.stringify(extra) : null;
}

const wb = xlsx.readFile(FILE_PATH);
const allSql = [];

allSql.push('-- 场所资源台账导入数据 (真实数据)');
allSql.push('-- 来源: 拔蛟窝社区各类场所.xlsx (2025年2月24日填报)');
allSql.push(`-- 导入批次: ${IMPORT_BATCH}`);
allSql.push('');

for (const sheetName of wb.SheetNames) {
  const ws = wb.Sheets[sheetName];
  const data = xlsx.utils.sheet_to_json(ws, { header: 1 });
  const headers = data[2]; // Row 3 is header
  const category = getCategory(sheetName);

  if (!category) continue;

  allSql.push(`-- === ${sheetName} (${category}) ===`);

  for (let i = 3; i < data.length; i++) {
    const row = data[i];
    if (!row || !row[1]) continue; // Skip empty rows

    const name = String(row[1] || '').trim();
    if (!name || name === '拔蛟窝社区无工业园') continue;

    const address = row[2] ? String(row[2]).trim() : '';
    const areaRaw = row[5] || row[4] || null;
    const area = areaRaw ? Number(areaRaw) || null : null;

    let responsible = '', responsiblePhone = '';
    let partyCadre = '', partyCadrePhone = '';
    let fireInspector = '', fireInspectorPhone = '';
    let remark = '';

    // Parse based on category
    if (category === 'SMALL_SHOP') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[8] ? String(row[8]).trim() : '';
    } else if (category === 'SMALL_WORKSHOP') {
      const cp = parsePhone(row[6]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[7]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[8]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[9] ? String(row[9]).trim() : '';
    } else if (category === 'RENTAL_HOUSE') {
      responsible = row[6] ? String(row[6]).trim() : '';
      responsiblePhone = row[7] ? String(row[7]).trim() : '';
      const pc = parsePhone(row[8]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[9]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[10] ? String(row[10]).trim() : '';
    } else if (category === 'INDUSTRIAL_PARK') {
      const cp = parsePhone(row[7]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[8]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[9]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[10] ? String(row[10]).trim() : '';
    } else if (category === 'RESIDENTIAL') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[8] ? String(row[8]).trim() : '';
    } else if (category === 'OTHER') {
      const cp = parsePhone(row[5]);
      responsible = cp.name; responsiblePhone = cp.phone;
      const pc = parsePhone(row[6]);
      partyCadre = pc.name; partyCadrePhone = pc.phone;
      const fc = parsePhone(row[7]);
      fireInspector = fc.name; fireInspectorPhone = fc.phone;
      remark = row[8] ? String(row[8]).trim() : '';
    }

    const extra = buildExtraData(category, {
      place_type: row[3] || '',
      production_type: row[3] || '',
      employee_count: row[4] || '',
      floor_count: row[3] || '',
      resident_count: row[4] || '',
      is_village_level: row[3] || '',
      building_count: row[4] || row[3] || '',
      tenant_count: row[5] || '',
    });

    const sql = `INSERT INTO cmn_place_ledger (place_category, place_name, address, area_sqm, responsible_person, responsible_phone, party_cadre, party_cadre_phone, fire_inspector, fire_inspector_phone, remark, extra_data, source_sheet, import_batch) VALUES (${escapeSql(category)}, ${escapeSql(name)}, ${escapeSql(address)}, ${escapeSql(area)}, ${escapeSql(responsible)}, ${escapeSql(responsiblePhone)}, ${escapeSql(partyCadre)}, ${escapeSql(partyCadrePhone)}, ${escapeSql(fireInspector)}, ${escapeSql(fireInspectorPhone)}, ${escapeSql(remark)}, ${escapeSql(extra)}, ${escapeSql(sheetName)}, ${escapeSql(IMPORT_BATCH)});`;

    allSql.push(sql);
  }
  allSql.push('');
}

console.log(allSql.join('\n'));
