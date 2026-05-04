<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item">系统管理</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-item active">角色权限</span>
    </div>

    <div class="page-header">
      <span class="page-title">🔐 角色权限管理</span>
      <div class="page-actions">
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增角色</el-button>
      </div>
    </div>

    <div class="role-grid">
      <div v-for="role in roles" :key="role.id" class="role-card card">
        <div class="role-header">
          <div class="role-icon" :style="{background: getRoleColor(role.roleCode)}">{{ getRoleIcon(role.roleCode) }}</div>
          <div class="role-info">
            <h3>{{ role.roleName }}</h3>
            <p>{{ role.description || '暂无描述' }}</p>
          </div>
          <el-tag :type="role.status === 1 ? 'success' : 'danger'" size="small" style="margin-left:auto;">
            {{ role.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </div>
        <div class="role-permissions">
          <el-tag v-for="p in role.permissions" :key="p" size="small" type="info" style="margin:2px;">{{ getPermLabel(p) }}</el-tag>
          <span v-if="!role.permissions?.length" style="color:#909399;font-size:12px;">暂未分配权限</span>
        </div>
        <div class="role-actions">
          <el-button size="small" @click="editRole(role)">编辑</el-button>
          <el-button size="small" type="primary" plain @click="viewPermissions(role)">查看权限</el-button>
          <el-popconfirm title="确定删除此角色？" @confirm="deleteRole(role.id)" v-if="role.roleCode !== 'SUPER_ADMIN'">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="showAddDialog" :title="editForm.id ? '✏️ 编辑角色' : '🔐 新增角色'" width="600px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角色名称">
              <el-input v-model="editForm.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色标识">
              <el-input v-model="editForm.roleCode" placeholder="英文标识，如 ADMIN" :disabled="!!editForm.id" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="请描述角色职责..." />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="权限配置">
          <div class="permission-grid">
            <el-checkbox
              v-for="p in allPermissions"
              :key="p"
              :label="p"
              v-model="editForm._checkedMap[p]"
              @change="syncPermissions"
            >{{ getPermLabel(p) }}</el-checkbox>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="saving">{{ editForm.id ? '保存修改' : '创建角色' }}</el-button>
      </template>
    </el-dialog>

    <!-- 查看权限弹窗 -->
    <el-dialog v-model="showPermissionDialog" title="📋 角色权限详情" width="500px">
      <div v-if="currentRole" style="text-align:center;margin-bottom:20px;">
        <div class="role-icon" :style="{background: getRoleColor(currentRole.roleCode), width:'60px', height:'60px', fontSize:'30px', margin:'0 auto 12px', borderRadius:'12px'}">
          {{ getRoleIcon(currentRole.roleCode) }}
        </div>
        <h3>{{ currentRole.roleName }}</h3>
        <p style="color:#909399;font-size:13px;">{{ currentRole.description }}</p>
      </div>
      <div v-if="currentRole?.permissions?.length">
        <el-divider content-position="left">已授权权限（{{ currentRole.permissions.length }} 项）</el-divider>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <el-tag v-for="p in currentRole.permissions" :key="p" type="primary" style="margin:0;">{{ getPermLabel(p) }}</el-tag>
        </div>
      </div>
      <el-empty v-else description="暂未分配权限" :image-size="60" />
      <template #footer><el-button @click="showPermissionDialog = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const showAddDialog = ref(false)
const showPermissionDialog = ref(false)
const saving = ref(false)
const currentRole = ref<any>(null)
const roles = ref<any[]>([])
const allPermissions = ref<string[]>([])
const editForm = reactive<any>({
  id: null,
  roleName: '',
  roleCode: '',
  description: '',
  sort: 0,
  permissions: [] as string[],
  _checkedMap: {} as Record<string, boolean>
})

// 权限码中文映射
const permissionLabelMap: Record<string, string> = {
  'dashboard:view': '仪表盘',
  'order:view': '订单查看',
  'order:list': '订单列表',
  'order:create': '订单创建',
  'order:edit': '订单编辑',
  'order:delete': '订单删除',
  'customer:view': '客户查看',
  'customer:list': '客户列表',
  'customer:create': '客户新增',
  'customer:edit': '客户编辑',
  'customer:delete': '客户删除',
  'member:view': '会员查看',
  'member:list': '会员列表',
  'member:create': '会员新增',
  'member:edit': '会员编辑',
  'member:delete': '会员删除',
  'finance:view': '财务查看',
  'finance:list': '财务列表',
  'finance:create': '财务新增',
  'finance:edit': '财务编辑',
  'finance:delete': '财务删除',
  'material:view': '素材查看',
  'material:list': '素材列表',
  'material:create': '素材上传',
  'material:edit': '素材编辑',
  'material:delete': '素材删除',
  'factory:view': '工厂查看',
  'factory:list': '工厂列表',
  'factory:create': '工厷新增',
  'factory:edit': '工厂编辑',
  'factory:delete': '工厂删除',
  'statistics:view': '数据统计',
  'statistics:export': '统计导出',
  'system:user': '用户管理',
  'system:role': '角色管理',
  'system:permission': '权限管理',
  'system:log': '操作日志',
  'system:dict': '数据字典',
  'system:backup': '数据备份',
  'system:notice': '系统通知',
  'system:menu': '菜单管理',
  'design:file': '设计文件',
  'square:manage': '广场管理',
}

function getPermLabel(code: string): string {
  return permissionLabelMap[code] || code
}

// 角色颜色映射
const roleColorMap: Record<string, string> = {
  'SUPER_ADMIN': 'linear-gradient(135deg,#f56c6c,#f78989)',
  'ADMIN': 'linear-gradient(135deg,#409eff,#79bbff)',
  'FINANCE': 'linear-gradient(135deg,#9c27b0,#ab47bc)',
  'OPERATOR': 'linear-gradient(135deg,#67c23a,#85ce61)',
  'VIEWER': 'linear-gradient(135deg,#909399,#b1b3b8)',
}

// 角色图标映射
const roleIconMap: Record<string, string> = {
  'SUPER_ADMIN': '👑',
  'ADMIN': '🛡️',
  'FINANCE': '💰',
  'OPERATOR': '🔧',
  'VIEWER': '👁️',
}

function getRoleColor(code?: string) {
  return code ? (roleColorMap[code] || 'linear-gradient(135deg,#409eff,#79bbff)') : 'linear-gradient(135deg,#409eff,#79bbff)'
}

function getRoleIcon(code?: string) {
  return code ? (roleIconMap[code] || '👤') : '👤'
}

function openAdd() {
  Object.assign(editForm, { id: null, roleName: '', roleCode: '', description: '', sort: 0, permissions: [], _checkedMap: {} })
  showAddDialog.value = true
}

function editRole(role: any) {
  // 构建权限选中映射
  const map: Record<string, boolean> = {}
  allPermissions.value.forEach(p => { map[p] = (role.permissions || []).includes(p) })
  Object.assign(editForm, {
    id: role.id,
    roleName: role.roleName,
    roleCode: role.roleCode,
    description: role.description,
    sort: role.sort,
    permissions: [...(role.permissions || [])],
    _checkedMap: map
  })
  showAddDialog.value = true
}

function syncPermissions() {
  editForm.permissions = allPermissions.value.filter(p => editForm._checkedMap[p])
}

function viewPermissions(role: any) {
  currentRole.value = role
  showPermissionDialog.value = true
}

async function saveRole() {
  if (!editForm.roleName) { ElMessage.warning('请输入角色名称'); return }
  if (!editForm.id && !editForm.roleCode) { ElMessage.warning('请输入角色标识'); return }
  saving.value = true
  try {
    const payload = {
      roleName: editForm.roleName,
      roleCode: editForm.roleCode,
      description: editForm.description,
      sort: editForm.sort,
      permissions: editForm.permissions
    }
    if (editForm.id) {
      await request.put(`/system/roles/${editForm.id}`, payload)
    } else {
      await request.post('/system/roles', payload)
    }
    ElMessage.success(editForm.id ? '角色信息已保存！' : '角色创建成功！')
    showAddDialog.value = false
    await fetchRoles()
  } catch { ElMessage.error('操作失败') }
  saving.value = false
}

async function deleteRole(id: number) {
  try {
    await request.delete(`/system/roles/${id}`)
    ElMessage.success('角色已删除')
    await fetchRoles()
  } catch { ElMessage.error('删除失败') }
}

async function fetchRoles() {
  try {
    const res = await request.get('/system/roles')
    if (res.data) roles.value = res.data
  } catch {}
}

async function fetchPermissions() {
  try {
    const res = await request.get('/system/roles/permissions/all')
    if (res.data) allPermissions.value = res.data
  } catch {}
}

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchPermissions()])
})
</script>

<style scoped lang="scss">
.role-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; }
.role-card { padding: 20px; }
.role-header { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; }
.role-icon { width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0; }
.role-info h3 { font-size: 16px; margin-bottom: 4px; }
.role-info p { font-size: 12px; color: #909399; margin: 0; }
.role-permissions { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 15px; min-height: 30px; }
.role-actions { display: flex; gap: 10px; border-top: 1px solid #f0f0f0; padding-top: 15px; }
.permission-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  border: 1px solid #dcdfe6;
  padding: 15px;
  border-radius: 6px;
  max-height: 240px;
  overflow-y: auto;
}
</style>
