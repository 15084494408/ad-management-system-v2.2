<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">⚡ 快速记账</span>
      <el-button type="primary" @click="openAdd">+ 新增记账</el-button>
    </div>
    <div class="card" style="padding:0;overflow:hidden;">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{row}">
            <el-tag :type="row.type==='income'?'success':'danger'" size="small">
              {{row.type==='income'?'收入':'支出'}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relatedName" label="公司/客户" min-width="140"/>
        <el-table-column prop="category" label="分类" width="100"/>
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{row}">
            <span :style="{color:row.type==='income'?'#67c23a':'#f56c6c',fontWeight:'700'}">
              {{row.type==='income'?'+':'-'}}¥{{Number(row.amount).toLocaleString()}}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100">
          <template #default="{row}">
            {{paymentMethodMap[row.paymentMethod] || row.paymentMethod}}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170"/>
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="{row}">
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAdd" title="快速记账" width="480px" @closed="resetForm">
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="income">收入</el-radio>
            <el-radio value="expense">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公司/客户" prop="relatedName">
          <el-select
            v-model="form.relatedName"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入公司/客户名"
            style="width:100%"
          >
            <el-option
              v-for="c in companies"
              :key="c.id || c"
              :label="c.customerName || c"
              :value="c.customerName || c"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width:100%" placeholder="选择分类">
            <el-option-group label="收入">
              <el-option label="订单收入" value="订单收入"/>
              <el-option label="充值收入" value="充值收入"/>
            </el-option-group>
            <el-option-group label="支出">
              <el-option label="采购支出" value="采购支出"/>
              <el-option label="工资" value="工资"/>
              <el-option label="房租" value="房租"/>
              <el-option label="广告费" value="广告费"/>
              <el-option label="印刷费" value="印刷费"/>
              <el-option label="设计费" value="设计费"/>
              <el-option label="材料费" value="材料费"/>
              <el-option label="退款" value="退款"/>
              <el-option label="其他" value="其他"/>
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" style="width:100%"/>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" style="width:100%" placeholder="选择支付方式">
            <el-option label="现金" value="cash"/>
            <el-option label="微信" value="wechat"/>
            <el-option label="支付宝" value="alipay"/>
            <el-option label="银行转账" value="transfer"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认记账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { financeApi, customerApi } from '@/api'
import { useFinanceStore } from '@/stores/finance'
const financeStore = useFinanceStore()

const list = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const saving = ref(false)
const companies = ref<any[]>([])
const formRef = ref<any>(null)

const paymentMethodMap: Record<string, string> = {
  cash: '现金', wechat: '微信', alipay: '支付宝', transfer: '银行转账'
}

const form = reactive({
  type: 'income' as string,
  relatedName: '' as string,
  category: '订单收入' as string,
  amount: 0 as number,
  paymentMethod: 'transfer' as string,
  remark: '' as string
})

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  relatedName: [{ required: true, message: '请选择或输入公司/客户名', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

function resetForm() {
  form.type = 'income'
  form.relatedName = ''
  form.category = '订单收入'
  form.amount = 0
  form.paymentMethod = 'transfer'
  form.remark = ''
}

function openAdd() {
  resetForm()
  showAdd.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  saving.value = true
  try {
    await financeApi.createQuickRecord({
      type: form.type,
      category: form.category,
      amount: form.amount,
      relatedName: form.relatedName,
      paymentMethod: form.paymentMethod,
      remark: form.remark
    })
    ElMessage.success('记账成功')
    showAdd.value = false
    financeStore.triggerRefresh()   // 全局通知其他页面刷新
    load()
  } catch (e: any) {
    ElMessage.error(e?.message || '记账失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除此记录？', '提示', { type: 'warning' })
    await financeApi.deleteRecord(row.id)
    ElMessage.success('已删除')
    financeStore.triggerRefresh()
    load()
  } catch {}
}

async function load() {
  loading.value = true
  try {
    const r = await financeApi.getQuickRecords()
    list.value = r.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadCompanies() {
  try {
    const r = await customerApi.getList({ current: 1, size: 200 })
    companies.value = r.data?.records || []
  } catch {
    companies.value = []
  }
}

onMounted(() => {
  load()
  loadCompanies()
})
</script>
