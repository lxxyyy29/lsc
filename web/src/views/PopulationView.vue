<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">实有人口库</h2>
        <p style="font-size:13px;color:#6b7280;">常住人口、流动人口</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="openFieldConfig" class="filter-action ghost">
          <i class="fas fa-cog"></i> 字段配置
        </button>
        <button @click="showImport = true" class="filter-action ghost">
          <i class="fas fa-file-import"></i> 导入
        </button>
        <button @click="exportData" class="filter-action ghost">
          <i class="fas fa-download"></i> 导出Excel
        </button>
        <button @click="openCreate()" class="filter-action">
          <i class="fas fa-plus"></i> 新增人员
        </button>
      </div>
    </div>

    <!-- 常驻 / 流动 Tab -->
    <div style="display:flex;gap:8px;margin-bottom:16px;border-bottom:2px solid #e5e7eb;padding-bottom:12px;">
      <button v-for="tab in populationTabs" :key="tab.value" @click="switchTab(tab.value)"
              :class="['filter-btn', activeTab === tab.value ? 'active' : '']">
        {{ tab.label }}
      </button>
    </div>

    <!-- 统一筛选栏：关键字模糊搜索 + 户籍类型(仅常驻) + 网格 -->
    <div class="card" style="padding:16px 24px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <input v-model="filters.keyword" class="filter-input" style="width:220px;"
               placeholder="模糊搜索姓名 / 电话 / 地址 / 身份证" @keyup.enter="fetchData" />
        <select v-if="isResidentTab" v-model="filters.householdType" class="filter-select">
          <option value="">全部户籍类型</option>
          <option v-for="t in residentHouseholdTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
        <select v-model="filters.gridId" class="filter-select">
          <option :value="null">全部网格</option>
          <option v-for="g in grids" :key="g.id" :value="Number(g.id)">{{ g.gridName }}</option>
        </select>
        <button @click="fetchData" class="filter-action"><i class="fas fa-search"></i> 查询</button>
        <button @click="resetFilters" class="filter-action ghost">重置</button>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <!-- 常驻：按户分组卡片 + 整户展开收起（后端已是树形结构，直接消费） -->
        <PopulationCardList v-if="isResidentTab" :households="list" @edit="openEdit" @delete="handleDelete"
                            @add-member="onAddMember" @change-head="onChangeHead"
                            @remove-member="handleRemoveMember" />
        <!-- 流动：普通列表（保持原表格效果，无户主概念） -->
        <table v-else class="table">
          <thead><tr>
            <th>姓名</th><th>性别</th><th>年龄</th><th>电话</th>
            <th>地址</th><th>楼栋/房号</th><th>网格</th><th>操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in list" :key="p.id">
              <td>
                {{ p.name }}
                <span v-if="p.isPartyMember === 1" class="party-chip">党员</span>
              </td>
              <td>{{ p.gender || '-' }}</td>
              <td>{{ p.age != null ? p.age : '-' }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td>{{ p.address || '-' }}</td>
              <td>{{ p.buildingNo ? p.buildingNo + (p.roomNo ? '-' + p.roomNo : '') : '-' }}</td>
              <td>{{ p.gridName || '-' }}</td>
              <td>
                <div style="display:flex;gap:6px;">
                  <button @click="openEdit(p)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                  <button @click="handleDelete(p)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!isResidentTab && !list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <!-- 新增/编辑弹窗（EP 弹层） -->
    <el-dialog v-model="showForm" :title="form.id ? '编辑人员' : '新增人员'" width="680px"
               class="pop-form-dialog ui-dialog" align-center :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <!-- 所属户：常驻人口可挂到具体户下；选择后自动带出地址与网格 -->
        <el-form-item v-if="isResidentTab" label="所属户（户口）">
          <el-select v-model="form.householdId" placeholder="不归户（独立人员）" filterable clearable
                     style="width:100%;" @change="onHouseholdChange">
            <el-option v-for="h in households" :key="h.id" :label="householdLabel(h)" :value="h.id" />
          </el-select>
          <div v-if="!households.length" style="font-size:12px;color:#9ca3af;margin-top:4px;">
            暂无可选户，可不归户直接添加为独立人员。
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <template v-for="f in formFields" :key="f.fieldKey">
            <el-col v-if="isFormVisible(f) && !(isKey(f, 'specialPopulationType') && form.specialPopulation != 1)" :span="isFullField(f) ? 24 : 12">
              <el-form-item :label="f.fieldLabel" :required="f.required == 1" :prop="camel(f.fieldKey)">
                <!-- 特殊人群勾选 -->
                <el-checkbox v-if="isKey(f, 'specialPopulation')" v-model="form.specialPopulation"
                             :true-value="1" :false-value="0" @change="onSpecialChange">是特殊人群</el-checkbox>
                <!-- 通用勾选（如「党员」）：按字段配置的 checkbox 类型渲染 -->
                <el-checkbox v-else-if="f.fieldType === 'checkbox'" v-model="form[camel(f.fieldKey)]"
                             :true-value="1" :false-value="0">{{ f.fieldLabel }}</el-checkbox>
                <!-- 特殊人群类型（勾选后显示；自定义...为显式入口） -->
                <template v-else-if="isKey(f, 'specialPopulationType') && form.specialPopulation == 1">
                  <el-select v-if="!customEditing['specialPopulationType']"
                             :model-value="form.specialPopulationType" placeholder="请选择" clearable style="width:100%;"
                             @update:model-value="onTypeUpdate($event, 'specialPopulationType')">
                    <el-option v-for="opt in selectOptions(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                  <div v-else style="display:flex;gap:6px;align-items:center;">
                    <el-input v-model="form.specialPopulationType" placeholder="请输入特殊人群类型" style="flex:1;" @keyup.enter="confirmCustom('specialPopulationType')" />
                    <el-button style="height:42px;flex-shrink:0;" @click="confirmCustom('specialPopulationType')">确定</el-button>
                  </div>
                </template>
                <!-- 日期字段：出生日期由身份证自动推算（只读），其余自定义日期字段可正常选择 -->
                <el-date-picker v-else-if="f.fieldType === 'date'" v-model="form[camel(f.fieldKey)]" type="date"
                                value-format="YYYY-MM-DD"
                                :placeholder="isKey(f, 'birthday') ? '填写身份证后自动带出' : '请选择日期'"
                                style="width:100%;"
                                :disabled="isKey(f, 'birthday')"
                                @change="isKey(f, 'birthday') ? autoFillAge() : undefined" />
                <!-- 与户主关系（自定义...为显式入口） -->
                <template v-else-if="isKey(f, 'relation')">
                  <el-select v-if="!customEditing['relation']"
                             :model-value="form.relation" placeholder="请选择" clearable style="width:100%;"
                             @update:model-value="onTypeUpdate($event, 'relation')">
                    <el-option v-for="opt in selectOptions(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                  <div v-else style="display:flex;gap:6px;align-items:center;">
                    <el-input v-model="form.relation" :placeholder="'请输入' + f.fieldLabel" style="flex:1;" @keyup.enter="confirmCustom('relation')" />
                    <el-button style="height:42px;flex-shrink:0;" @click="confirmCustom('relation')">确定</el-button>
                  </div>
                </template>
                <!-- 身份证号：输入后自动推算出生日期(年龄)与性别 -->
                <el-input v-else-if="isKey(f, 'idCard')" v-model="form.idCard" placeholder="请输入身份证号（自动推算年龄和性别）"
                          @input="autoFillByIdCard" />
                <!-- 通用下拉（含网格、性别）：性别由身份证推算，只读 -->
                <el-select v-else-if="f.fieldType === 'select'" v-model="form[camel(f.fieldKey)]"
                           :placeholder="isComputedField(f) ? '根据身份证号自动计算' : '请选择'"
                           :disabled="isComputedField(f)" style="width:100%;">
                  <el-option v-for="opt in selectOptions(f)" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <!-- 文本域 -->
                <el-input v-else-if="f.fieldType === 'textarea'" v-model="form[camel(f.fieldKey)]" type="textarea" :rows="2" placeholder="选填" />
                <!-- 通用输入（年龄由身份证推算，只读） -->
                <el-input v-else v-model="form[camel(f.fieldKey)]"
                          :placeholder="isComputedField(f) ? '根据身份证号自动计算' : '请输入' + f.fieldLabel"
                          :disabled="isComputedField(f)" />
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
      <!-- 按钮固定右下角 -->
      <template #footer>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <el-button @click="showForm = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">{{ form.id ? '保存' : '添加' }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 字段配置弹窗 -->
    <div v-if="showConfig" class="modal-overlay">
      <div class="modal-box" style="width:680px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">字段配置</h3>
        <p style="font-size:12px;color:#6b7280;margin-bottom:12px;">
          勾选启用的字段将显示在新增/编辑表单中，可调整排序与必填；可新增自定义字段、删除不需要的字段。
          <b>新增与删除在点「保存配置」后生效。</b>（导入模板列固定，不受此配置影响）
        </p>

        <!-- 新增字段 -->
        <div style="border:1px dashed #d1d5db;border-radius:8px;padding:10px 12px;margin-bottom:12px;background:#fafafa;">
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
            <input v-model="newField.fieldKey" class="form-input" style="width:132px;padding:4px 8px;font-size:12px;"
                   placeholder="字段键 如 nativePlace" />
            <input v-model="newField.fieldLabel" class="form-input" style="width:110px;padding:4px 8px;font-size:12px;"
                   placeholder="显示名 如 籍贯" />
            <select v-model="newField.fieldType" class="filter-select" style="height:28px;font-size:12px;padding:0 8px;">
              <option v-for="t in FIELD_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
            <button @click="addConfigField" class="btn btn-primary" style="padding:4px 12px;font-size:12px;">+ 添加字段</button>
          </div>
          <div v-if="newField.fieldType === 'select'" style="margin-top:8px;">
            <input v-model="newField.options" class="form-input" style="width:100%;padding:4px 8px;font-size:12px;"
                   placeholder="下拉选项，逗号分隔；需要「值:显示名」时可写成 1:本地,2:外地" />
          </div>
          <p style="font-size:11px;color:#9ca3af;margin-top:6px;line-height:1.6;">
            字段键用于存储，建议英文且创建后不要改动。自定义字段的值保存在该人员记录的扩展字段中，
            不参与关键字搜索与 Excel 导出。
          </p>
        </div>

        <div style="max-height:46vh;overflow-y:auto;">
          <div v-for="(f, idx) in configFields" :key="f.id != null ? f.id : f._tmpKey"
               style="display:flex;align-items:center;gap:8px;padding:8px 4px;border-bottom:1px solid #f3f4f6;">
            <input type="checkbox" v-model="f.enabled" :true-value="1" :false-value="0" style="accent-color:#1890ff;" />
            <input v-model="f.fieldLabel" class="form-input" style="width:110px;padding:4px 8px;font-size:12px;" />
            <span style="flex:1;font-size:12px;color:#9ca3af;">
              {{ f.fieldKey }}
              <span v-if="f.id == null" style="margin-left:6px;padding:1px 6px;border-radius:999px;background:#e6f4ff;color:#0958d9;font-size:11px;">待新增</span>
            </span>
            <label style="display:flex;align-items:center;gap:4px;font-size:12px;color:#6b7280;cursor:pointer;">
              <input type="checkbox" v-model="f.required" :true-value="1" :false-value="0" style="accent-color:#ff4d4f;" />必填
            </label>
            <div style="display:flex;gap:4px;">
              <button @click="moveConfig(idx, -1)" :disabled="idx === 0" class="btn btn-default" style="padding:2px 8px;font-size:11px;">↑</button>
              <button @click="moveConfig(idx, 1)" :disabled="idx === configFields.length - 1" class="btn btn-default" style="padding:2px 8px;font-size:11px;">↓</button>
              <button @click="removeConfigField(idx)" :disabled="isSystemField(f)"
                      :title="isSystemField(f) ? '系统必需字段，不能删除' : '删除该字段'"
                      class="btn btn-danger" style="padding:2px 8px;font-size:11px;">删除</button>
            </div>
          </div>
        </div>
        <p style="font-size:11px;color:#9ca3af;margin-top:10px;">
          姓名、身份证号、与户主关系、所属网格为系统必需字段，不可删除（可改显示名或取消勾选停用）。
          删除字段不会清除已录入的数据，仅使其不再出现在表单中。
        </p>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showConfig = false" class="btn btn-default">取消</button>
          <button @click="saveConfig" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存配置' }}</button>
        </div>
      </div>
    </div>

    <!-- 变更户主弹窗：选择新户主 → 预览关系重算结果 → 确认 -->
    <el-dialog v-model="showHeadDialog" title="变更户主" width="640px"
               class="ui-dialog" align-center :close-on-click-modal="false">
      <p style="font-size:13px;color:#6b7280;margin-bottom:12px;">
        户：<b style="color:#26221d;">{{ headTarget?.address || '-' }}</b>
        <span v-if="headTarget?.headName"> · 现户主：{{ headTarget.headName }}</span>
        <span v-else> · 尚未指定户主</span>
      </p>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="新户主">
              <el-select v-model="headForm.newHeadId" placeholder="请选择" style="width:100%;" @change="onNewHeadChange">
                <el-option v-for="m in headMembers" :key="m.id"
                           :label="`${m.name}${m.relation ? '（' + m.relation + '）' : ''}`" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="新户主与原户主的关系">
              <el-select v-model="headForm.relationToOldHead" placeholder="用于推算其他成员关系" clearable
                         style="width:100%;" @change="previewHead">
                <el-option v-for="r in anchorRelations" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div v-if="headPreview.length" style="max-height:40vh;overflow-y:auto;border:1px solid #f0f0f0;border-radius:8px;">
        <table class="table" style="margin:0;">
          <thead><tr><th>姓名</th><th>性别</th><th>原与户主关系</th><th>变更后关系</th></tr></thead>
          <tbody>
            <tr v-for="c in headPreview" :key="c.id">
              <td>
                {{ c.name }}
                <span v-if="c.newHead" style="margin-left:6px;padding:1px 7px;border-radius:999px;font-size:11px;background:#fde8e8;color:#a32d2d;">新户主</span>
              </td>
              <td>{{ c.gender || '-' }}</td>
              <td>{{ c.oldRelation || '-' }}</td>
              <td>
                <span :style="{ color: c.changed ? '#c2547a' : '#9ca3af', fontWeight: c.changed ? 600 : 400 }">
                  {{ c.newRelation || '-' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="headPreview.length" style="font-size:12px;color:#9ca3af;margin-top:8px;">
        标注为「其他」的成员无法自动推算，请在变更后于成员编辑中人工修正。
      </p>
      <template #footer>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <el-button @click="showHeadDialog = false">取消</el-button>
          <el-button :disabled="!headForm.newHeadId" @click="previewHead">预览关系变更</el-button>
          <el-button type="primary" :loading="saving" :disabled="!headForm.newHeadId" @click="confirmChangeHead">确认变更</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <ImportDialog v-model:visible="showImport" type="population" :columns="importColumns" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import http from '../api'
import { getHouseholdTypeName } from '../utils/eventTypes'
import { getFormFieldConfig, saveFormFieldConfig } from '../api'
import ImportDialog from '../components/ImportDialog.vue'
import PopulationCardList from '../components/PopulationCardList.vue'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

// 常驻 / 流动 Tab
const populationTabs = [
  { value: 'RESIDENT', label: '常驻人口' },
  { value: 'FLOATING', label: '流动人口' },
] as const
const activeTab = ref<'RESIDENT' | 'FLOATING'>('RESIDENT')
const isResidentTab = computed(() => activeTab.value === 'RESIDENT')

// 户籍类型选项（常驻专用；流动库取消），与后端 PopulationController.HOUSEHOLD_LABELS 保持一致。
// 注意：必须覆盖库中实际出现过的全部枚举，否则编辑历史数据时 el-select 匹配不到会原样显示英文代码。
const residentHouseholdTypes = [
  { value: 'LOCAL', label: '本地户籍' },
  { value: 'NON_LOCAL', label: '外地户籍' },
  { value: 'LOW_INCOME', label: '低保户' },
  { value: 'SPECIAL_CARE', label: '优抚对象' },
  { value: 'OTHER', label: '其他' },
]

// 户籍按当前页签自动归类：常驻人口页 → 本地户籍，流动人口页 → 流动人口。
// 「户籍类型」不再在新增/编辑表单中展示（见 isFormVisible），由页面上下文决定，避免人工选错造成口径不一致。
const RESIDENT_HOUSEHOLD_TYPE = 'LOCAL'
const FLOATING_HOUSEHOLD_TYPE = 'FLOATING'

// 特殊人群类型预置 + 自定义
const specialPopulationTypes = ['低保户', '优抚对象', '残疾人', '孤寡老人', '困境儿童']
// 与户主关系预置
const relationTypes = ['户主', '配偶', '儿子', '女儿', '父亲', '母亲']

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const showImport = ref(false)
const showForm = ref(false)
const showConfig = ref(false)

// ===== 户相关状态 =====
const households = ref<any[]>([])
const showHeadDialog = ref(false)
const headTarget = ref<any>(null)
const headMembers = ref<any[]>([])
const headPreview = ref<any[]>([])
const headForm = ref<{ newHeadId: number | null; relationToOldHead: string }>({ newHeadId: null, relationToOldHead: '' })
// 新户主相对原户主的关系候选（关系推算的锚点）
const anchorRelations = ['儿子', '女儿', '配偶', '父亲', '母亲', '兄弟', '姐妹', '其他']

// 字段键归一化（snake↔camel）
function camel(fieldKey: any): any {
  return String(fieldKey || '').replace(/_([a-z])/g, (_, c: string) => c.toUpperCase())
}
// 键匹配判断（兼容命名）
function isKey(f: any, key: string) {
  return camel(f.fieldKey) === key
}
// 整行字段（占满两列）
function isFullField(f: any) {
  return ['address', 'gridId', 'remark'].includes(camel(f.fieldKey))
}
// 由身份证号自动推算的字段：只读展示，不允许手工填写
const COMPUTED_FIELDS = ['gender', 'age']
function isComputedField(f: any) {
  return COMPUTED_FIELDS.includes(camel(f.fieldKey))
}

// 字段配置器数据
const configFields = ref<any[]>([])
// 关系排在特殊人群前
const formFields = computed(() => {
  const enabled = configFields.value.filter(f => f.enabled == 1)
  const relIdx = enabled.findIndex(f => isKey(f, 'relation'))
  const spIdx = enabled.findIndex(f => isKey(f, 'specialPopulation'))
  if (relIdx > -1 && spIdx > -1 && relIdx > spIdx) {
    const [rel] = enabled.splice(relIdx, 1)
    enabled.splice(spIdx, 0, rel)
  }
  // 身份证号排在性别/年龄之前：后两者由身份证自动推算（只读），先填身份证才看得到结果
  const idCardIdx = enabled.findIndex(f => isKey(f, 'idCard'))
  const computedIdx = enabled.findIndex(f => COMPUTED_FIELDS.includes(camel(f.fieldKey)))
  if (idCardIdx > -1 && computedIdx > -1 && idCardIdx > computedIdx) {
    const [idCardField] = enabled.splice(idCardIdx, 1)
    enabled.splice(computedIdx, 0, idCardField)
  }
  return enabled
})

// 导入列：固定完整列序（与后端 ImportService 固定索引 0..9 对齐），避免动态缩减导致列错位
const IMPORT_COLUMNS = ['gridName', 'householdFlag', 'name', 'age', 'gender', 'address', 'phone', 'relation', 'remark']
const importColumns = computed(() => IMPORT_COLUMNS)

const filters = reactive({
  keyword: '',
  householdType: '',
  gridId: null as number | null,
})

const emptyForm = () => {
  const base: any = {
    id: null as number | null,
    householdId: null as number | null,
    name: '', gender: '', age: null as number | null, phone: '', idCard: '', birthday: '',
    householdType: '', specialPopulation: 0, specialPopulationType: '', isPartyMember: 0, relation: '',
    address: '', buildingNo: '', roomNo: '',
    gridId: null as number | null, remark: '',
    status: 'ACTIVE',
  }
  // 字段配置器新增的自定义字段补默认值：保证表单 v-model 有初值，避免提交时缺失该键
  for (const f of formFields.value) {
    const key = camel(f.fieldKey)
    if (!(key in base)) base[key] = f.fieldType === 'checkbox' ? 0 : ''
  }
  return base
}
const form = ref(emptyForm())
const formRef = ref<any>()

// 必填校验（按字段配置）
const formRules = computed<Record<string, any>>(() => {
  const rules: Record<string, any> = {}
  for (const f of formFields.value) {
    // 性别/年龄由身份证自动推算且只读，不参与必填校验（用户无法填写）
    if (f.required == 1 && !isComputedField(f)) {
      const key = String(camel(f.fieldKey))
      rules[key] = f.fieldType === 'select'
        ? { required: true, message: '请选择' + f.fieldLabel, trigger: 'change' }
        : { required: true, message: '请输入' + f.fieldLabel, trigger: 'blur' }
      if (key === 'idCard') {
        rules[key] = [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { validator: validateIdCard, trigger: 'blur' },
        ]
      }
    }
  }
  return rules
})

// 身份证号校验（18 位 + 校验位）
function validateIdCard(_rule: any, value: any, callback: any) {
  const v = String(value || '').trim()
  if (!v) return callback()
  if (!/^\d{17}[\dXx]$/.test(v)) return callback(new Error('身份证号格式不正确'))
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const codes = '10X98765432'
  let sum = 0
  for (let i = 0; i < 17; i++) sum += Number(v[i]) * weights[i]
  if (v[17].toUpperCase() !== codes[sum % 11]) return callback(new Error('身份证号校验位不正确'))
  callback()
}

// 字段配置器中 select 类型解析选项
function selectOptions(f: any) {
  if (isKey(f, 'specialPopulationType')) {
    return [...specialPopulationTypes.map(v => ({ value: v, label: v })), { value: '__custom__', label: '自定义...' }]
  }
  if (isKey(f, 'relation')) {
    return [...relationTypes.map(v => ({ value: v, label: v })), { value: '__custom__', label: '自定义...' }]
  }
  if (isKey(f, 'householdType')) return residentHouseholdTypes
  // 网格选项取列表
  if (isKey(f, 'gridId')) {
    return grids.value.map(g => ({ value: Number(g.id), label: g.gridName }))
  }
  if (f.options) {
    return f.options.split(',').map((o: string) => {
      const [value, label] = o.split(':')
      return { value: value || o, label: label || value || o }
    })
  }
  return []
}

// 自定义...转内联输入（拦截哨兵值，不落 v-model）
const customEditing = reactive<Record<string, boolean>>({})
function onTypeUpdate(v: any, field: 'specialPopulationType' | 'relation') {
  if (v === '__custom__') {
    form.value[field] = ''
    customEditing[field] = true
  } else {
    form.value[field] = v
  }
}

// 自定义确认：非空且非哨兵值才退出
function confirmCustom(field: 'specialPopulationType' | 'relation') {
  const v = String(form.value[field] || '').trim()
  if (!v) {
    showMessage(`请填写${field === 'specialPopulationType' ? '特殊人群类型' : '与户主关系'}`, 'warning')
    return
  }
  if (v === '__custom__') {
    showMessage('请勿输入保留字', 'warning')
    return
  }
  customEditing[field] = false
}

// 户籍类型统一不在新增/编辑表单中展示：由当前页签自动匹配（常驻→本地户籍，流动→流动人口）
function isFormVisible(f: any) {
  if (isKey(f, 'householdType')) return false
  return true
}

// 勾选联动：取消清空类型与自定义态
function onSpecialChange(v: any) {
  if (!v) form.value.specialPopulationType = ''
  customEditing['specialPopulationType'] = false
}

// 是否户主（用于列表高亮）
function isHead(p: any) {
  return isResidentTab.value && p.relation === '户主'
}

// 出生日期自动推算年龄
function autoFillAge() {
  if (!form.value.birthday) return
  const b = new Date(form.value.birthday)
  if (isNaN(b.getTime())) return
  const now = new Date()
  let age = now.getFullYear() - b.getFullYear()
  const m = now.getMonth() - b.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--
  form.value.age = age >= 0 ? age : null
}

// 身份证号自动推算性别与出生日期(年龄)：支持 18 位与 15 位
function autoFillByIdCard() {
  const id = String(form.value.idCard || '').trim()
  let birth = ''
  let genderDigit = ''
  if (/^\d{17}[\dXx]$/.test(id)) {
    birth = id.slice(6, 14) // YYYYMMDD
    genderDigit = id.charAt(16) // 第17位，奇男偶女
  } else if (/^\d{15}$/.test(id)) {
    birth = '19' + id.slice(6, 12) // 15位：YYMMDD，出生年前补 19
    genderDigit = id.charAt(14)
  } else {
    // 身份证被清空时同步清空推算结果，避免旧值残留（输入中途不清空）
    if (!id) {
      form.value.gender = ''
      form.value.age = null
      form.value.birthday = ''
    }
    return
  }
  if (genderDigit && /^\d$/.test(genderDigit)) {
    form.value.gender = Number(genderDigit) % 2 === 1 ? '男' : '女'
  }
  if (birth) {
    const y = Number(birth.slice(0, 4))
    const m = Number(birth.slice(4, 6))
    const d = Number(birth.slice(6, 8))
    const now = new Date()
    if (y >= 1900 && y <= now.getFullYear() && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
      const pad = (n: number) => String(n).padStart(2, '0')
      form.value.birthday = `${y}-${pad(m)}-${pad(d)}`
      autoFillAge()
    }
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { populationType: activeTab.value }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (isResidentTab.value && filters.householdType) params.householdType = filters.householdType
    if (filters.gridId) params.gridId = filters.gridId
    const res: any = await http.get('/community/population', { params }) || []
    // 常住返回"户→成员"树（带 children），流动返回扁平列表
    list.value = res
    // 户下拉与当前筛选保持一致
    if (isResidentTab.value) fetchHouseholds()
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function switchTab(tab: 'RESIDENT' | 'FLOATING') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  resetFilters()
}

function resetFilters() {
  filters.keyword = ''
  filters.householdType = ''
  filters.gridId = null
  fetchData()
}

async function fetchGrids() {
  try {
    const tree = await http.get('/community/grids/tree') || []
    const all: any[] = []
    const walk = (nodes: any[]) => {
      for (const n of nodes) {
        all.push(n)
        if (n.children) walk(n.children)
      }
    }
    walk(tree)
    grids.value = all
  } catch(e) {}
}

async function loadFieldConfig() {
  try {
    configFields.value = await getFormFieldConfig('population') || []
  } catch(e) {
    configFields.value = []
  }
}

// 字段类型选项（需与后端 FormFieldConfigController.ALLOWED_TYPES 及表单渲染分支一致）
const FIELD_TYPES = [
  { value: 'text', label: '单行文本' },
  { value: 'textarea', label: '多行文本' },
  { value: 'select', label: '下拉选择' },
  { value: 'date', label: '日期' },
  { value: 'checkbox', label: '勾选' },
]

// 系统必需字段（归一化键，与后端 FormFieldConfigMapper.SYSTEM_REQUIRED_KEYS 一致）：不可删除
const SYSTEM_FIELD_KEYS = ['name', 'idcard', 'relation', 'gridid']
function isSystemField(f: any): boolean {
  if (f?.systemRequired === true) return true
  return SYSTEM_FIELD_KEYS.includes(String(f?.fieldKey || '').replace(/_/g, '').toLowerCase())
}

// 新增字段的草稿
const newField = ref<{ fieldKey: string; fieldLabel: string; fieldType: string; options: string }>({
  fieldKey: '', fieldLabel: '', fieldType: 'text', options: '',
})

// 字段键归一化（去下划线 + 小写），用于「重复字段」判断
const normalizeFieldKey = (key: any) => String(key || '').replace(/_/g, '').toLowerCase()

function addConfigField() {
  const key = newField.value.fieldKey.trim()
  const label = newField.value.fieldLabel.trim()
  if (!key) { showMessage('请填写字段键', 'warning'); return }
  if (!/^[A-Za-z][A-Za-z0-9_]{0,31}$/.test(key)) {
    showMessage('字段键需以字母开头，只能包含字母、数字、下划线，最长 32 位', 'warning'); return
  }
  if (!label) { showMessage('请填写字段显示名', 'warning'); return }
  if (configFields.value.some(f => normalizeFieldKey(f.fieldKey) === normalizeFieldKey(key))) {
    showMessage(`字段键「${key}」已存在`, 'warning'); return
  }
  if (newField.value.fieldType === 'select' && !newField.value.options.trim()) {
    showMessage('下拉类型必须填写选项', 'warning'); return
  }
  configFields.value.push({
    // id 为 null 表示本次新增，保存时由后端插入
    id: null,
    _tmpKey: 'new-' + Date.now() + '-' + Math.random().toString(36).slice(2, 7),
    module: 'population',
    fieldKey: key,
    fieldLabel: label,
    fieldType: newField.value.fieldType,
    options: newField.value.options.trim() || null,
    enabled: 1,
    required: 0,
    sortOrder: configFields.value.length + 1,
  })
  newField.value = { fieldKey: '', fieldLabel: '', fieldType: 'text', options: '' }
}

async function removeConfigField(idx: number) {
  const f = configFields.value[idx]
  if (!f) return
  if (isSystemField(f)) {
    showMessage('系统必需字段不能删除', 'warning')
    return
  }
  // 已保存的字段删除影响面更大（表单少一项），先确认；未保存的新增行直接移除
  if (f.id != null) {
    const ok = await confirmDialog({
      message: `确定删除字段「${f.fieldLabel || f.fieldKey}」？点「保存配置」后该字段将不再出现在表单中（已录入的数据不会被清除）。`,
      danger: true,
      okText: '删除',
    })
    if (!ok) return
  }
  configFields.value.splice(idx, 1)
  configFields.value.forEach((x, i) => { x.sortOrder = i + 1 })
}

function openFieldConfig() {
  newField.value = { fieldKey: '', fieldLabel: '', fieldType: 'text', options: '' }
  showConfig.value = true
}

function moveConfig(idx: number, dir: number) {
  const target = idx + dir
  if (target < 0 || target >= configFields.value.length) return
  const arr = configFields.value
  const tmp = arr[idx]
  arr[idx] = arr[target]
  arr[target] = tmp
  arr.forEach((f, i) => { f.sortOrder = i + 1 })
}

async function saveConfig() {
  saving.value = true
  try {
    // 后端按「对账」处理：有 id 更新、无 id 新增、名单里缺失的删除
    const payload = configFields.value.map((f, i) => ({
      id: f.id ?? null,
      module: 'population',
      fieldKey: f.fieldKey,
      fieldLabel: f.fieldLabel,
      fieldType: f.fieldType,
      options: f.options ?? null,
      enabled: f.enabled,
      required: f.required,
      sortOrder: i + 1,
    }))
    await saveFormFieldConfig(payload)
    showMessage('配置已保存')
    showConfig.value = false
    await loadFieldConfig()
  } catch(e: any) {
    showMessage(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function openCreate(prefill?: { householdId?: number | null; address?: string; gridId?: number | null }) {
  form.value = emptyForm()
  // 户籍按当前页签自动归类（表单不展示该选项）
  form.value.householdType = isResidentTab.value ? RESIDENT_HOUSEHOLD_TYPE : FLOATING_HOUSEHOLD_TYPE
  if (prefill?.householdId) {
    form.value.householdId = prefill.householdId
    form.value.address = prefill.address || ''
    form.value.gridId = prefill.gridId ?? null
  }
  showForm.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(p: any) {
  // 列表（常驻为"户→成员"树）返回的行可能不带自定义字段值，
  // 统一按 id 取一次详情再回填，避免编辑保存时把已有自定义字段值覆盖成空
  let full: any = p
  if (p?.id) {
    try {
      const detail: any = await http.get(`/community/population/${p.id}`)
      if (detail && typeof detail === 'object') full = detail
    } catch (e) {
      // 详情获取失败时退化为用列表行数据回填
    }
  }
  form.value = {
    ...emptyForm(),
    id: full.id,
    householdId: full.householdId || null,
    name: full.name || '', gender: full.gender || '', age: full.age != null ? full.age : null,
    phone: full.phone || '', idCard: full.idCard || '', birthday: full.birthday || '',
    householdType: full.householdType || '',
    specialPopulation: full.specialPopulation || 0,
    specialPopulationType: full.specialPopulationType || '',
    isPartyMember: full.isPartyMember || 0,
    relation: full.relation || '',
    address: full.address || '', buildingNo: full.buildingNo || '', roomNo: full.roomNo || '',
    gridId: full.gridId || null, remark: full.remark || '',
    status: full.status || 'ACTIVE',
    // 自定义字段值原样回填
    ...(full.extraFields && typeof full.extraFields === 'object' ? full.extraFields : {}),
  }
  showForm.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    if (!payload.age) payload.age = null
    if (!payload.birthday) payload.birthday = null
    if (!payload.gridId) payload.gridId = null
    if (!isResidentTab.value) {
      // 流动人口不参与归户，户籍固定为「流动人口」
      payload.householdType = FLOATING_HOUSEHOLD_TYPE
      payload.householdId = null
      payload.relation = ''
    } else if (!form.value.id) {
      // 新增常驻人员：户籍按当前页签自动归为「本地户籍」（表单已不展示该选项）
      payload.householdType = RESIDENT_HOUSEHOLD_TYPE
    }
    // 编辑常驻人员时保留其原有户籍分类（外地户籍/低保户/优抚对象等），避免自动归类覆盖历史数据
    if (form.value.id) {
      await http.put(`/community/population/${form.value.id}`, payload)
      showMessage('保存成功')
    } else {
      await http.post('/community/population', payload)
      showMessage('添加成功')
    }
    showForm.value = false
    await fetchData()
  } catch(e: any) {
    showMessage(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(p: any) {
  if (!await confirmDialog({ message: `确定删除人员「${p.name}」吗？删除后不可恢复。`, danger: true, okText: '删除' })) return
  try {
    await http.delete(`/community/population/${p.id}`)
    showMessage('删除成功')
    fetchData()
  } catch(e: any) {
    showMessage(e?.message || '删除失败')
  }
}

// ==================== 户管理 ====================

function householdLabel(h: any) {
  const addr = h.address || '未填写地址'
  return h.headName ? `${addr}（户主：${h.headName}）` : `${addr}（暂无户主，${h.memberCount || 0}人）`
}

async function fetchHouseholds() {
  try {
    const params: any = {}
    if (filters.gridId) params.gridId = filters.gridId
    households.value = await http.get('/community/household', { params }) || []
  } catch (e) {
    households.value = []
  }
}

// 选择所属户后自动带出地址与网格
function onHouseholdChange(id: any) {
  const h = households.value.find(x => x.id === id)
  if (!h) return
  if (!form.value.address) form.value.address = h.address || ''
  if (!form.value.gridId) form.value.gridId = h.gridId || null
}

// 户卡片「新增成员」：打开人员表单并预填所属户
function onAddMember(household: any) {
  openCreate({
    householdId: household?.householdId || household?.id || null,
    address: household?.address,
    gridId: household?.gridId,
  })
}

// 户卡片「变更户主」
async function onChangeHead(household: any) {
  const householdId = household?.householdId || household?.id
  if (!householdId) {
    showMessage('该户尚未归户，无法变更户主', 'warning')
    return
  }
  headTarget.value = { ...household, householdId, headName: household?.head?.name }
  headForm.value = { newHeadId: null, relationToOldHead: '' }
  headPreview.value = []
  try {
    const res: any = await http.get(`/community/household/${householdId}`)
    headMembers.value = res?.members || []
    if (res?.household) headTarget.value = { ...res.household, householdId }
  } catch (e: any) {
    showMessage(e?.message || '加载户成员失败')
    return
  }
  showHeadDialog.value = true
}

// 选择新户主后：用其当前关系自动推断锚点关系并立即预览
function onNewHeadChange(id: any) {
  const m = headMembers.value.find(x => x.id === id)
  headForm.value.relationToOldHead = m?.relation && m.relation !== '户主' ? m.relation : ''
  previewHead()
}

async function previewHead() {
  const householdId = headTarget.value?.householdId
  if (!householdId || !headForm.value.newHeadId) return
  try {
    headPreview.value = await http.post(`/community/household/${householdId}/change-head/preview`, {
      newHeadId: headForm.value.newHeadId,
      relationToOldHead: headForm.value.relationToOldHead || null,
    }) || []
  } catch (e: any) {
    headPreview.value = []
    showMessage(e?.message || '预览失败')
  }
}

async function confirmChangeHead() {
  const householdId = headTarget.value?.householdId
  if (!householdId || !headForm.value.newHeadId) return
  if (!await confirmDialog({
    message: '确认变更户主？系统将按亲属规则自动重算其他成员「与户主关系」。',
    okText: '确认变更',
  })) return
  saving.value = true
  try {
    const res: any = await http.post(`/community/household/${householdId}/change-head`, {
      newHeadId: headForm.value.newHeadId,
      relationToOldHead: headForm.value.relationToOldHead || null,
    })
    if (res?.manualReview) {
      showMessage('户主已变更；部分成员关系无法自动推算，已置为「其他」，请人工核对', 'warning')
    } else {
      showMessage('户主变更成功')
    }
    showHeadDialog.value = false
    await fetchData()
    await fetchHouseholds()
  } catch (e: any) {
    showMessage(e?.message || '变更失败')
  } finally {
    saving.value = false
  }
}

// 把成员移出户（仍保留在人口库）
async function handleRemoveMember(p: any) {
  if (!p?.householdId) {
    showMessage('该人员未归户', 'warning')
    return
  }
  if (!await confirmDialog({
    message: `将「${p.name}」移出该户？移出后其仍保留在人口库中。`,
    okText: '移出',
  })) return
  try {
    await http.delete(`/community/household/${p.householdId}/members/${p.id}`)
    showMessage('已移出该户')
    await fetchData()
    await fetchHouseholds()
  } catch (e: any) {
    showMessage(e?.message || '移出失败')
  }
}

// 导出：携带当前 tab/户籍/网格/关键字筛选条件
async function exportData() {
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  const qs = new URLSearchParams()
  qs.set('populationType', activeTab.value)
  if (filters.keyword.trim()) qs.set('keyword', filters.keyword.trim())
  if (isResidentTab.value && filters.householdType) qs.set('householdType', filters.householdType)
  if (filters.gridId) qs.set('gridId', String(filters.gridId))
  try {
    const res = await fetch(`/api/community/population/export?${qs.toString()}`, {
      headers: { Authorization: `Bearer ${session.token}` }
    })
    if (!res.ok) {
      showMessage(res.status === 401 ? '登录已过期，请重新登录' : '导出失败，请稍后重试')
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = activeTab.value === 'RESIDENT' ? '常驻人口台账.xlsx' : '流动人口台账.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
    showMessage('导出失败，请检查网络')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
  loadFieldConfig()
  fetchHouseholds()
})
</script>

<style>
/* 表单滚动，按钮固定右下角；外观见 .ui-dialog */
.pop-form-dialog .el-dialog__body {
  max-height: calc(88vh - 150px);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 8px 0;
}
/* 「党员」标识（流动人口表格） */
.party-chip {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  line-height: 1.5;
  background: #fff7e6;
  color: #ad6800;
  border: 0.5px solid #ffd591;
}

/* 由身份证自动推算的只读字段（出生日期 / 年龄 / 性别）：
   保持只读语义，但外观与其它可输入文本框保持一致（默认置灰样式会让它们看起来像不可用） */
.pop-form-dialog .el-input.is-disabled .el-input__wrapper,
.pop-form-dialog .el-select.is-disabled .el-select__wrapper,
.pop-form-dialog .el-date-editor.is-disabled .el-input__wrapper {
  background-color: #fff;
  box-shadow: none;
}
.pop-form-dialog .el-input.is-disabled .el-input__inner,
.pop-form-dialog .el-select.is-disabled .el-select__selected-item,
.pop-form-dialog .el-date-editor.is-disabled .el-input__inner {
  color: #606266;
  -webkit-text-fill-color: #606266;
}
</style>
