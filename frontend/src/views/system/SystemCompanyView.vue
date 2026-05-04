<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">公司管理</span>
    </div>

    <div class="page-header">
      <span class="page-title">🏢 公司管理</span>
      <div class="page-actions">
        <el-button @click="exportData"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增公司</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="公司名称" min-width="180">
          <template #default="{ row }">
            <span style="font-weight:600;">{{ row.companyName }}</span>
            <el-tag v-if="row.isDefault === 1" size="small" type="success" style="margin-left:6px;">默认</el-tag>
            <el-tag v-if="row.companyType === 'headquarters'" size="small" type="primary" style="margin-left:4px;">总公司</el-tag>
            <el-tag v-else-if="row.companyType === 'branch'" size="small" type="warning" style="margin-left:4px;">门店</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="bankName" label="开户银行" width="150" show-overflow-tooltip />
        <el-table-column prop="bankAccount" label="银行账号" width="170" />
        <el-table-column prop="taxNo" label="税号" width="160" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="editCompany(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="toggleDefault(row)" :disabled="row.isDefault === 1">
              {{ row.isDefault === 1 ? '已默认' : '设为默认' }}
            </el-button>
            <el-button link type="danger" size="small" @click="deleteCompany(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑公司弹窗 -->
    <el-dialog v-model="showDialog" :title="editForm.id ? '🏢 编辑公司' : '🏢 新增公司'" width="680px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="公司名称" required>
              <el-input v-model="editForm.companyName" placeholder="请输入公司全称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="公司地址">
              <el-input v-model="editForm.address" placeholder="请输入公司地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.phone" placeholder="电话/手机" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="editForm.contactPerson" placeholder="主要负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传真">
              <el-input v-model="editForm.fax" placeholder="传真号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司类型">
              <el-select v-model="editForm.companyType" placeholder="请选择" style="width:100%">
                <el-option label="总公司" value="headquarters" />
                <el-option label="分公司/门店" value="branch" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="电子邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为默认">
              <el-switch v-model="isDefaultSwitch" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">💰 开户信息</el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开户银行">
              <el-input v-model="editForm.bankName" placeholder="如：中国工商银行上海分行" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行账号">
              <el-input v-model="editForm.bankAccount" placeholder="银行账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税号">
              <el-input v-model="editForm.taxNo" placeholder="纳税人识别号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="editForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCompany" :loading="saving">
          {{ editForm.id ? '💾 保存修改' : '💾 创建公司' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { systemApi } from '@/api/modules/system'
import { exportToExcel } from '@/utils/excelExport'

const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const tableData = ref<any[]>([])
const isDefaultSwitch = ref(false)
const editForm = reactive<any>({
  id: null,
  companyName: '',
  address: '',
  phone: '',
  contactPerson: '',
  fax: '',
  email: '',
  bankName: '',
  bankAccount: '',
  taxNo: '',
  logoUrl: '',
  isDefault: 0,
  companyType: '',
  status: 1,
})

async function loadData() {
  loading.value = true
  try {
    const res = await systemApi.getCompanyList()
    tableData.value = res.data || []
  } catch {
    tableData.value = []
  }
  loading.value = false
}

function openAdd() {
  Object.assign(editForm, {
    id: null, companyName: '', address: '', phone: '', contactPerson: '', fax: '',
    email: '', bankName: '', bankAccount: '', taxNo: '', logoUrl: '',
    isDefault: 0, companyType: '', status: 1,
  })
  isDefaultSwitch.value = false
  showDialog.value = true
}

function editCompany(row: any) {
  Object.assign(editForm, {
    id: row.id, companyName: row.companyName, address: row.address || '',
    phone: row.phone || '', contactPerson: row.contactPerson || '',
    fax: row.fax || '', email: row.email || '',
    bankName: row.bankName || '', bankAccount: row.bankAccount || '',
    taxNo: row.taxNo || '', logoUrl: row.logoUrl || '',
    isDefault: row.isDefault || 0, companyType: row.companyType || '', status: row.status ?? 1,
  })
  isDefaultSwitch.value = row.isDefault === 1
  showDialog.value = true
}

async function saveCompany() {
  if (!editForm.companyName?.trim()) {
    ElMessage.warning('公司名称不能为空')
    return
  }
  saving.value = true
  try {
    editForm.isDefault = isDefaultSwitch.value ? 1 : 0
    if (editForm.id) {
      await systemApi.updateCompany(editForm.id, { ...editForm })
      ElMessage.success('公司信息已保存')
    } else {
      await systemApi.createCompany({ ...editForm })
      ElMessage.success('公司创建成功')
    }
    showDialog.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败，请重试')
  }
  saving.value = false
}

async function toggleDefault(row: any) {
  try {
    await systemApi.updateCompany(row.id, { isDefault: 1 })
    ElMessage.success('已设为默认公司')
    loadData()
  } catch {
    ElMessage.error('设置失败')
  }
}

async function deleteCompany(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公司「${row.companyName}」吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await systemApi.deleteCompany(row.id)
    ElMessage.success('公司已删除')
    loadData()
  } catch {
    // 用户取消
  }
}

function exportData() {
  exportToExcel({
    filename: '公司信息',
    header: ['公司名称', '地址', '电话', '传真', '邮箱', '开户银行', '银行账号', '税号', '状态'],
    data: tableData.value.map(row => [
      row.companyName || '-',
      row.address || '-',
      row.phone || '-',
      row.fax || '-',
      row.email || '-',
      row.bankName || '-',
      row.bankAccount || '-',
      row.taxNo || '-',
      row.status === 1 ? '启用' : '禁用',
    ]),
    infoRows: [[`导出时间：${new Date().toLocaleString()}`], [`共 ${tableData.value.length} 家公司`]],
  })
}

onMounted(loadData)
</script>
