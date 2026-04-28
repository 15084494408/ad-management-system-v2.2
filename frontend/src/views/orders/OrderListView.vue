<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">订单管理</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">📋 订单列表</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="exportVisible = true">⬇️ 导出</button>
        <button class="btn btn-primary" @click="createVisible = true">+ 创建订单</button>
      </div>
    </div>

    <!-- 状态流程 -->
    <div class="card">
      <div class="status-flow">
        <div class="flow-step" v-for="(s, i) in statusFlow" :key="i">
          <div class="flow-dot" :class="{ completed: i < activeStatusIdx, active: i === activeStatusIdx }">
            {{ i < activeStatusIdx ? '✓' : i + 1 }}
          </div>
          <span class="flow-label" :class="{ active: i <= activeStatusIdx }">{{ s }}</span>
        </div>
        <div class="flow-line" v-for="i in statusFlow.length - 1" :key="'line-' + i"></div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-form">
      <div class="form-group">
        <label>订单编号</label>
        <input type="text" v-model="query.keyword" class="form-control" placeholder="请输入订单编号" @keyup.enter="loadList">
      </div>
      <div class="form-group">
        <label>客户名称</label>
        <input type="text" v-model="query.customerName" class="form-control" placeholder="请输入客户名称" @keyup.enter="loadList">
      </div>
      <div class="form-group">
        <label>订单状态</label>
        <select v-model="query.status" class="form-control">
          <option value="">全部</option>
          <option value="1">待确认</option>
          <option value="2">进行中</option>
          <option value="3">已完成</option>
          <option value="4">已取消</option>
        </select>
      </div>
      <div class="form-group" style="align-self:flex-end;">
        <button class="btn btn-primary" @click="loadList">🔍 搜索</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th><input type="checkbox" v-model="selectAll" @change="toggleSelectAll"></th>
            <th>订单编号</th>
            <th>客户名称</th>
            <th>订单名称</th>
            <th>订单金额</th>
            <th>订单状态</th>
            <th>交付日期</th>
            <th>设计师</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orderStore.list" :key="order.id">
            <td><input type="checkbox" :value="order.id" v-model="selectedIds"></td>
            <td style="font-family:monospace;">{{ order.orderNo }}</td>
            <td>{{ order.customerName }}</td>
            <td>{{ order.title }}</td>
            <td style="font-weight:600;">¥{{ formatMoney(order.totalAmount) }}</td>
            <td>
              <span class="status-tag" :class="'status-' + statusKeyMap[order.status]">
                {{ statusLabelMap[order.status] || '未知' }}
              </span>
            </td>
            <td>{{ order.deliveryDate || '-' }}</td>
            <td>{{ order.designerName || '待分配' }}</td>
            <td>{{ formatTime(order.createTime) }}</td>
            <td class="action-btns">
              <button class="action-btn view" @click="openDetail(order)">查看</button>
              <button class="action-btn edit" v-if="order.status !== 3 && order.status !== 4" @click="openProcess(order)">
                {{ order.status === 1 ? '处理' : (order.status === 2 ? '确认交付' : '处理') }}
              </button>
            </td>
          </tr>
          <tr v-if="!orderStore.list.length">
            <td colspan="10" style="text-align:center;color:#909399;padding:40px 0;">暂无订单数据</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination">
        <div class="pagination-info">共 {{ orderStore.total }} 条记录</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="query.current <= 1" @click="query.current--; loadList()">«</button>
          <button class="page-btn" v-for="p in pageNumbers" :key="p" :class="{ active: query.current === p }" @click="query.current = p; loadList()">{{ p }}</button>
          <button class="page-btn" :disabled="query.current >= totalPages" @click="query.current++; loadList()">»</button>
        </div>
      </div>
    </div>

    <!-- ===== 创建订单弹窗 ===== -->
    <div class="modal-overlay" v-if="createVisible" @click.self="createVisible = false">
      <div class="modal-container" style="max-width:860px;">
        <div class="modal-header">
          <h3>📋 创建订单</h3>
          <button class="modal-close" @click="createVisible = false">&times;</button>
        </div>
        <div class="modal-body" style="max-height:70vh;overflow-y:auto;">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">客户名称 *</label>
              <select class="form-input" v-model="createForm.customerId" @change="onCustomerChange">
                <option value="">请选择客户</option>
                <option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.customerName }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">订单名称 *</label>
              <input type="text" class="form-input" v-model="createForm.title" placeholder="请输入订单名称">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">订单类型</label>
              <select class="form-input" v-model="createForm.orderType">
                <option :value="1">图文打印</option>
                <option :value="2">广告制作</option>
                <option :value="3">设计服务</option>
                <option :value="4">装订服务</option>
                <option :value="5">其他</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">交付日期 *</label>
              <input type="date" class="form-input" v-model="createForm.deliveryDate">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">设计师</label>
              <select class="form-input" v-model="createForm.designerName">
                <option value="">待分配</option>
                <option>李明</option>
                <option>王设计</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">订单优先级</label>
              <select class="form-input" v-model="createForm.priority">
                <option :value="1">普通</option>
                <option :value="2">紧急</option>
                <option :value="3">加急</option>
              </select>
            </div>
          </div>
          <div class="form-row full">
            <div class="form-group">
              <label class="form-label">订单描述</label>
              <textarea class="form-input" v-model="createForm.description" rows="3" placeholder="请详细描述订单需求..."></textarea>
            </div>
          </div>

          <!-- 物料明细 -->
          <div style="margin-top:16px;padding:15px;background:#fdf6ec;border-radius:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
              <span class="tag tag-warning" style="cursor:pointer;" @click="addMaterialRow">+ 添加物料明细</span>
              <span style="font-size:12px;color:#909399;">共 {{ createForm.materials.length }} 项</span>
            </div>
            <table v-if="createForm.materials.length" style="font-size:13px;">
              <thead>
                <tr>
                  <th style="width:28%;">物料名称</th>
                  <th style="width:18%;">规格</th>
                  <th style="width:12%;">数量</th>
                  <th style="width:14%;">单价(¥)</th>
                  <th style="width:14%;">小计</th>
                  <th style="width:14%;">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(m, i) in createForm.materials" :key="i">
                  <td><input type="text" class="form-input" style="font-size:12px;" v-model="m.materialName" placeholder="物料名称"></td>
                  <td><input type="text" class="form-input" style="font-size:12px;" v-model="m.spec" placeholder="规格"></td>
                  <td><input type="number" class="form-input" style="font-size:12px;" v-model.number="m.quantity" min="0" @input="calcMaterialSubtotal(m)"></td>
                  <td><input type="number" class="form-input" style="font-size:12px;" v-model.number="m.unitPrice" min="0" step="0.01" @input="calcMaterialSubtotal(m)"></td>
                  <td style="font-weight:600;color:#409eff;">¥{{ formatMoney(m.amount) }}</td>
                  <td><button class="action-btn delete" @click="createForm.materials.splice(i, 1)">删除</button></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div style="margin-top:16px;padding:15px;background:#f5f7fa;border-radius:8px;display:flex;justify-content:space-between;align-items:center;">
            <span>订单总金额：</span>
            <span style="font-size:20px;font-weight:700;color:#409eff;">¥ {{ formatMoney(createMaterialTotal) }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="createVisible = false">取消</button>
          <button class="btn btn-primary" @click="submitCreate" :disabled="submitting">💾 保存订单</button>
        </div>
      </div>
    </div>

    <!-- ===== 查看详情弹窗 ===== -->
    <div class="modal-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="modal-container" style="max-width:780px;">
        <div class="modal-header">
          <h3>👁️ 订单详情 — #{{ detailData?.orderNo }}</h3>
          <button class="modal-close" @click="detailVisible = false">&times;</button>
        </div>
        <div class="modal-body" style="max-height:70vh;overflow-y:auto;">
          <!-- Tab 栏 -->
          <div class="detail-tabs">
            <div class="detail-tab" :class="{ active: detailTab === 'basic' }" @click="detailTab = 'basic'">📋 基本信息</div>
            <div class="detail-tab" :class="{ active: detailTab === 'material' }" @click="detailTab = 'material'">📦 物料明细</div>
            <div class="detail-tab" :class="{ active: detailTab === 'finance' }" @click="detailTab = 'finance'">💰 财务信息</div>
          </div>

          <!-- 基本信息 -->
          <div v-if="detailTab === 'basic'" class="detail-tab-content">
            <div class="info-grid">
              <div class="info-cell">
                <div class="info-label">订单编号</div>
                <div class="info-value">{{ detailData?.orderNo }}</div>
              </div>
              <div class="info-cell">
                <div class="info-label">订单状态</div>
                <div><span class="status-tag" :class="'status-' + statusKeyMap[detailData?.status]">{{ statusLabelMap[detailData?.status] }}</span></div>
              </div>
              <div class="info-cell">
                <div class="info-label">客户名称</div>
                <div class="info-value">{{ detailData?.customerName }}</div>
              </div>
              <div class="info-cell">
                <div class="info-label">订单金额</div>
                <div class="info-value" style="color:#f56c6c;font-weight:700;">¥{{ formatMoney(detailData?.totalAmount) }}</div>
              </div>
              <div class="info-cell">
                <div class="info-label">交付日期</div>
                <div class="info-value">{{ detailData?.deliveryDate || '-' }}</div>
              </div>
              <div class="info-cell">
                <div class="info-label">设计师</div>
                <div class="info-value">{{ detailData?.designerName || '待分配' }}</div>
              </div>
            </div>
            <div style="margin-top:16px;">
              <div style="font-size:12px;color:#909399;margin-bottom:6px;">订单描述</div>
              <div style="background:#f5f7fa;padding:12px;border-radius:8px;font-size:13px;line-height:1.7;">{{ detailData?.description || '暂无描述' }}</div>
            </div>
            <div style="margin-top:16px;">
              <div style="font-size:12px;color:#909399;margin-bottom:10px;">处理进度</div>
              <div class="status-flow" style="justify-content:flex-start;">
                <div class="flow-step" v-for="(s, i) in statusFlow" :key="i">
                  <div class="flow-dot" :class="{ completed: i < detailStatusIdx, active: i === detailStatusIdx }">
                    {{ i < detailStatusIdx ? '✓' : i + 1 }}
                  </div>
                  <div class="flow-label" :class="{ active: i <= detailStatusIdx }">{{ s }}</div>
                </div>
                <div class="flow-line" v-for="i in statusFlow.length - 1" :key="'dline-' + i"></div>
              </div>
            </div>
          </div>

          <!-- 物料明细 -->
          <div v-if="detailTab === 'material'" class="detail-tab-content">
            <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;">
              <span style="font-size:13px;color:#606266;">共 {{ detailMaterials.length }} 项物料</span>
            </div>
            <table class="data-table" v-if="detailMaterials.length">
              <thead>
                <tr><th>物料/工艺</th><th>规格</th><th>数量</th><th>单价</th><th>小计</th></tr>
              </thead>
              <tbody>
                <tr v-for="m in detailMaterials" :key="m.id">
                  <td>{{ m.materialName }}</td>
                  <td>{{ m.spec || '-' }}</td>
                  <td>{{ m.quantity }} {{ m.unit || '' }}</td>
                  <td>¥{{ formatMoney(m.unitPrice) }}</td>
                  <td style="font-weight:600;color:#67c23a;">¥{{ formatMoney(m.amount) }}</td>
                </tr>
              </tbody>
            </table>
            <div style="margin-top:14px;padding:12px 16px;background:#f0f9eb;border-radius:8px;display:flex;justify-content:space-between;align-items:center;">
              <span style="font-size:13px;color:#606266;">物料合计：</span>
              <span style="font-size:18px;font-weight:700;color:#67c23a;">¥ {{ formatMoney(detailMaterialTotal) }}</span>
            </div>
          </div>

          <!-- 财务信息 -->
          <div v-if="detailTab === 'finance'" class="detail-tab-content">
            <div class="finance-summary">
              <div class="finance-card" style="border-color:#67c23a;">
                <div class="finance-label" style="color:#67c23a;">订单总额</div>
                <div class="finance-value" style="color:#67c23a;">¥{{ formatMoney(detailData?.totalAmount) }}</div>
              </div>
              <div class="finance-card" style="border-color:#409eff;">
                <div class="finance-label" style="color:#409eff;">已收金额</div>
                <div class="finance-value" style="color:#409eff;">¥{{ formatMoney(detailData?.paidAmount) }}</div>
              </div>
              <div class="finance-card" v-if="(detailData?.roundingAmount || 0) > 0" style="border-color:#e6a23c;">
                <div class="finance-label" style="color:#e6a23c;">抹零金额</div>
                <div class="finance-value" style="color:#e6a23c;">-¥{{ formatMoney(detailData?.roundingAmount) }}</div>
              </div>
              <div class="finance-card" style="border-color:#f56c6c;">
                <div class="finance-label" style="color:#f56c6c;">待收余额</div>
                <div class="finance-value" style="color:#f56c6c;">¥{{ formatMoney((detailData?.totalAmount || 0) - (detailData?.paidAmount || 0) - (detailData?.roundingAmount || 0)) }}</div>
              </div>
            </div>
            <div style="display:flex;gap:8px;justify-content:flex-end;margin:16px 0;">
              <button class="btn btn-success btn-sm" @click="paymentVisible = true">💰 登记收款</button>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button v-if="detailData?.status === 1" class="btn btn-success" @click="openProcess(detailData)">⚡ 处理订单</button>
          <button v-if="detailData?.status === 2" class="btn btn-success" @click="openConfirmDelivery(detailData)">📦 确认交付</button>
          <button class="btn btn-default" @click="detailVisible = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- ===== 处理订单弹窗 ===== -->
    <div class="modal-overlay" v-if="processVisible" @click.self="processVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header">
          <h3>⚡ 处理订单</h3>
          <button class="modal-close" @click="processVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div style="text-align:center;padding:20px 0;">
            <div style="width:60px;height:60px;background:#ecf5ff;border-radius:50%;display:flex;align-items:center;justify-content:center;margin:0 auto 20px;font-size:30px;">📋</div>
            <h3 style="margin-bottom:10px;">确认接收此订单？</h3>
            <p style="color:#909399;font-size:13px;">订单 {{ processData?.orderNo }} 将进入设计阶段</p>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">分配设计师 *</label>
              <select class="form-input" v-model="processForm.designerName">
                <option value="">请选择设计师</option>
                <option>李明</option>
                <option>王设计</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">交付日期</label>
              <input type="date" class="form-input" v-model="processForm.deliveryDate">
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">处理备注</label>
            <textarea class="form-input" v-model="processForm.remark" rows="2" placeholder="可选，填写处理备注..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="processVisible = false">取消</button>
          <button class="btn btn-success" @click="submitProcess">✅ 确认接收</button>
        </div>
      </div>
    </div>

    <!-- ===== 确认交付弹窗 ===== -->
    <div class="modal-overlay" v-if="deliveryVisible" @click.self="deliveryVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header">
          <h3>📦 确认交付</h3>
          <button class="modal-close" @click="deliveryVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div style="text-align:center;padding:20px 0;">
            <div style="width:60px;height:60px;background:#f0f9eb;border-radius:50%;display:flex;align-items:center;justify-content:center;margin:0 auto 20px;font-size:30px;">✅</div>
            <h3 style="margin-bottom:10px;">确认订单已完成交付？</h3>
            <p style="color:#909399;font-size:13px;">订单 {{ deliveryData?.orderNo }} 将标记为已完成</p>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">交付日期</label>
              <input type="date" class="form-input" v-model="deliveryForm.deliveryDate">
            </div>
            <div class="form-group">
              <label class="form-label">交付方式</label>
              <select class="form-input" v-model="deliveryForm.deliveryMethod">
                <option>自取</option>
                <option>送货上门</option>
                <option>快递邮寄</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">交付备注</label>
            <textarea class="form-input" v-model="deliveryForm.remark" rows="2" placeholder="可选，填写交付备注..."></textarea>
          </div>
          <div style="background:#f0f9eb;padding:10px 15px;border-radius:6px;font-size:12px;color:#67c23a;">
            ✓ 确认交付后，系统将自动发送通知给客户
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="deliveryVisible = false">取消</button>
          <button class="btn btn-success" @click="submitDelivery">✅ 确认交付</button>
        </div>
      </div>
    </div>

    <!-- ===== 登记收款弹窗 ===== -->
    <div class="modal-overlay" v-if="paymentVisible" @click.self="paymentVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header">
          <h3>💰 登记收款</h3>
          <button class="modal-close" @click="paymentVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <!-- 收款信息概览 -->
          <div style="background:#f5f7fa;border-radius:8px;padding:12px 16px;margin-bottom:16px;">
            <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
              <span style="color:#909399;">订单总额</span>
              <span style="font-weight:600;">¥{{ formatMoney(detailData?.totalAmount) }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;margin-bottom:4px;">
              <span style="color:#909399;">已付金额</span>
              <span style="font-weight:600;color:#67c23a;">¥{{ formatMoney(detailData?.paidAmount) }}</span>
            </div>
            <div style="display:flex;justify-content:space-between;">
              <span style="color:#909399;font-weight:600;">待收金额</span>
              <span style="font-weight:600;color:#f56c6c;">¥{{ formatMoney(remainingAmount) }}</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">收款金额 *</label>
            <input type="number" class="form-input" v-model.number="paymentForm.amount" placeholder="0.00" min="0" step="0.01">
          </div>
          <div class="form-group">
            <label class="form-label">收款方式</label>
            <select class="form-input" v-model="paymentForm.method">
              <option value="微信">微信</option>
              <option value="支付宝">支付宝</option>
              <option value="银行转账">银行转账</option>
              <option value="现金">现金</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">付款方</label>
            <input type="text" class="form-input" v-model="paymentForm.payer" placeholder="付款方名称">
          </div>
          <!-- 抹零选项 -->
          <div style="border:1px dashed #dcdfe6;border-radius:8px;padding:12px 16px;margin-top:8px;">
            <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-weight:600;color:#e6a23c;">
              <input type="checkbox" v-model="paymentForm.writeOff" style="width:18px;height:18px;">
              ✂️ 抹零结清
            </label>
            <div v-if="paymentForm.writeOff" style="margin-top:8px;padding:8px 12px;background:#fdf6ec;border-radius:6px;">
              <div style="display:flex;justify-content:space-between;font-size:13px;">
                <span>本次收款</span>
                <span>¥{{ formatMoney(paymentForm.amount) }}</span>
              </div>
              <div style="display:flex;justify-content:space-between;font-size:13px;color:#f56c6c;margin-top:4px;">
                <span>抹零金额</span>
                <span>¥{{ formatMoney(Math.max(remainingAmount - (paymentForm.amount || 0), 0)) }}</span>
              </div>
              <div style="display:flex;justify-content:space-between;font-size:13px;margin-top:4px;font-weight:600;">
                <span>订单状态</span>
                <span style="color:#67c23a;">抹零结清 ✅</span>
              </div>
            </div>
          </div>
          <div class="form-group" style="margin-top:12px;">
            <label class="form-label">备注</label>
            <textarea class="form-input" v-model="paymentForm.remark" rows="2" placeholder="可选备注"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="paymentVisible = false">取消</button>
          <button class="btn btn-success" @click="submitPayment">
            💰 {{ paymentForm.writeOff ? '抹零结清' : '确认收款' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 导出弹窗 ===== -->
    <div class="modal-overlay" v-if="exportVisible" @click.self="exportVisible = false">
      <div class="modal-container" style="max-width:500px;">
        <div class="modal-header">
          <h3>⬇️ 导出订单</h3>
          <button class="modal-close" @click="exportVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">导出范围</label>
              <select class="form-input" v-model="exportForm.range">
                <option value="all">全部订单</option>
                <option value="selected">已选订单</option>
                <option value="filtered">当前筛选结果</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">导出格式</label>
              <select class="form-input" v-model="exportForm.format">
                <option value="xlsx">Excel (.xlsx)</option>
                <option value="csv">CSV (.csv)</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">开始日期</label>
              <input type="date" class="form-input" v-model="exportForm.startDate">
            </div>
            <div class="form-group">
              <label class="form-label">结束日期</label>
              <input type="date" class="form-input" v-model="exportForm.endDate">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="exportVisible = false">取消</button>
          <button class="btn btn-primary" @click="doExport">⬇️ 开始导出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { orderApi } from '@/api'
import request from '@/api/request'
import { exportToExcel } from '@/utils/excelExport'

const router = useRouter()
const orderStore = useOrderStore()
const loading = ref(false)
const submitting = ref(false)

// ===== 状态映射 =====
const statusFlow = ['待确认', '进行中', '已完成', '已取消']
const statusLabelMap: Record<number, string> = { 1: '待确认', 2: '进行中', 3: '已完成', 4: '已取消' }
const statusKeyMap: Record<number, string> = { 1: 'pending', 2: 'designing', 3: 'completed', 4: 'cancelled' }
const activeStatusIdx = ref(1) // 默认展示"进行中"

// ===== 搜索 =====
const query = reactive({
  keyword: '', customerName: '', status: '',
  current: 1, size: 10,
})

const totalPages = computed(() => Math.ceil(orderStore.total / query.size))
const pageNumbers = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, query.current - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// ===== 批量选择 =====
const selectedIds = ref<number[]>([])
const selectAll = computed({
  get: () => orderStore.list.length > 0 && selectedIds.value.length === orderStore.list.length,
  set: () => {
    if (selectedIds.value.length === orderStore.list.length) {
      selectedIds.value = []
    } else {
      selectedIds.value = orderStore.list.map((o: any) => o.id)
    }
  }
})
function toggleSelectAll() { /* computed setter handles it */ }

// ===== 客户列表（创建订单用） =====
const customerList = ref<any[]>([])

// ===== 弹窗控制 =====
const createVisible = ref(false)
const detailVisible = ref(false)
const detailTab = ref('basic')
const processVisible = ref(false)
const deliveryVisible = ref(false)
const paymentVisible = ref(false)
const exportVisible = ref(false)

// ===== 创建订单表单 =====
function newMaterial() { return { materialName: '', spec: '', unit: '', quantity: 1, unitPrice: 0, amount: 0 } }

const createForm = reactive({
  customerId: '' as any,
  customerName: '',
  title: '',
  orderType: 1,
  deliveryDate: '',
  designerName: '',
  priority: 1,
  description: '',
  materials: [newMaterial()] as any[],
})

function onCustomerChange() {
  const c = customerList.value.find((item: any) => item.id === createForm.customerId)
  createForm.customerName = c ? c.customerName : ''
}
function addMaterialRow() { createForm.materials.push(newMaterial()) }
function calcMaterialSubtotal(m: any) { m.amount = (m.quantity || 0) * (m.unitPrice || 0) }
const createMaterialTotal = computed(() => createForm.materials.reduce((s: number, m: any) => s + (m.amount || 0), 0))

async function submitCreate() {
  if (!createForm.title || !createForm.customerId) { alert('请填写客户名称和订单名称'); return }
  submitting.value = true
  try {
    await orderApi.create({
      customerId: createForm.customerId,
      customerName: createForm.customerName,
      title: createForm.title,
      orderType: createForm.orderType,
      deliveryDate: createForm.deliveryDate || null,
      designerName: createForm.designerName || null,
      priority: createForm.priority,
      description: createForm.description,
      materials: createForm.materials.filter((m: any) => m.materialName),
    } as any)
    createVisible.value = false
    loadList()
  } finally { submitting.value = false }
}

// ===== 查看详情 =====
const detailData = ref<any>(null)
const detailMaterials = ref<any[]>([])
const detailMaterialTotal = computed(() => detailMaterials.value.reduce((s: number, m: any) => s + ((m.quantity || 0) * (m.unitPrice || 0)), 0))
const detailStatusIdx = computed(() => {
  const s = detailData.value?.status
  if (s === 1) return 0
  if (s === 2) return 1
  if (s === 3) return 2
  return 0
})

async function openDetail(order: any) {
  detailTab.value = 'basic'
  try {
    const res = await orderApi.getDetail(order.id)
    const d = res.data
    detailData.value = d?.order || order
    detailMaterials.value = d?.materials || []
  } catch {
    detailData.value = order
    detailMaterials.value = []
  }
  detailVisible.value = true
}

// ===== 处理订单 =====
const processData = ref<any>(null)
const processForm = reactive({ designerName: '', deliveryDate: '', remark: '' })

function openProcess(order: any) {
  processData.value = order
  processForm.designerName = order.designerName || ''
  processForm.deliveryDate = order.deliveryDate || ''
  processForm.remark = ''
  detailVisible.value = false
  processVisible.value = true
}

async function submitProcess() {
  if (!processForm.designerName) { alert('请选择设计师'); return }
  await orderApi.update(processData.value.id, {
    status: 2,
    designerName: processForm.designerName,
    deliveryDate: processForm.deliveryDate || null,
  })
  processVisible.value = false
  loadList()
}

// ===== 确认交付 =====
const deliveryData = ref<any>(null)
const deliveryForm = reactive({ deliveryDate: '', deliveryMethod: '自取', remark: '' })

function openConfirmDelivery(order: any) {
  deliveryData.value = order
  deliveryForm.deliveryDate = new Date().toISOString().slice(0, 10)
  deliveryForm.deliveryMethod = '自取'
  deliveryForm.remark = ''
  detailVisible.value = false
  deliveryVisible.value = true
}

async function submitDelivery() {
  await orderApi.updateStatus(deliveryData.value.id, 3)
  deliveryVisible.value = false
  loadList()
}

// ===== 登记收款 =====
const paymentForm = reactive({ amount: 0, method: '微信', payer: '', remark: '', writeOff: false })
const remainingAmount = computed(() => {
  const total = detailData.value?.totalAmount || 0
  const paid = detailData.value?.paidAmount || 0
  return Math.max(total - paid, 0)
})

async function submitPayment() {
  if (!paymentForm.amount || paymentForm.amount <= 0) { alert('请输入有效金额'); return }
  if (paymentForm.writeOff && paymentForm.amount > remainingAmount.value) { alert('收款金额不能超过待收金额'); return }
  await orderApi.addPayment(detailData.value.id, paymentForm)
  paymentVisible.value = false
  paymentForm.amount = 0
  paymentForm.writeOff = false
  // 刷新详情
  openDetail(detailData.value)
}

// ===== 导出 =====
const exportForm = reactive({ range: 'all', format: 'xlsx', startDate: '', endDate: '' })
function doExport() {
  // 确定数据源
  let data: any[] = []
  if (exportForm.range === 'selected' && selectedIds.value.length > 0) {
    data = orderStore.list.filter((o: any) => selectedIds.value.includes(o.id))
  } else if (exportForm.range === 'all') {
    // 全部：使用当前已加载的全部数据（orderStore.list 是当前页，如需全部可扩展）
    data = [...orderStore.list]
  } else {
    // filtered / 默认：当前页数据
    data = [...orderStore.list]
  }

  // 日期范围过滤
  if (exportForm.startDate || exportForm.endDate) {
    data = data.filter((o: any) => {
      const ct = String(o.createTime || '')
      if (!ct) return false
      const d = ct.slice(0, 10)
      if (exportForm.startDate && d < exportForm.startDate) return false
      if (exportForm.endDate && d > exportForm.endDate) return false
      return true
    })
  }

  if (data.length === 0) {
    alert('没有符合条件的数据可导出')
    exportVisible.value = false
    return
  }

  const header = ['订单编号', '客户名称', '订单名称', '订单金额', '已付金额', '状态', '交付日期', '设计师', '创建时间']
  const rows = data.map((o: any) => [
    o.orderNo || '',
    o.customerName || '',
    o.title || '',
    `¥${formatMoney(o.totalAmount)}`,
    `¥${formatMoney(o.paidAmount ?? 0)}`,
    statusLabelMap[o.status] || '未知',
    o.deliveryDate || '-',
    o.designerName || '待分配',
    formatTime(o.createTime),
  ])

  const totalAmt = data.reduce((s: number, o: any) => s + (Number(o.totalAmount) || 0), 0)
  const paidAmt = data.reduce((s: number, o: any) => s + (Number(o.paidAmount) || 0), 0)

  const rangeLabel: Record<string, string> = { all: '全部订单', selected: '已选订单', filtered: '当前筛选结果' }
  const dateDesc = exportForm.startDate || exportForm.endDate
    ? `（${exportForm.startDate || '不限'} ~ ${exportForm.endDate || '不限'}）`
    : ''

  exportToExcel({
    filename: '订单列表',
    title: '订单列表',
    header,
    data: rows,
    summaryRow: ['合计', '', '', `¥${formatMoney(totalAmt)}`, `¥${formatMoney(paidAmt)}`, '', '', '', ''],
    infoRows: [
      [`导出时间：${new Date().toLocaleString()}`],
      [`导出范围：${rangeLabel[exportForm.range] || '当前页'} ${dateDesc}`],
      [`共 ${data.length} 条记录`],
    ],
  })

  exportVisible.value = false
}

// ===== 工具函数 =====
function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatTime(t: any) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

// ===== 加载数据 =====
async function loadList() {
  loading.value = true
  try {
    const params: any = { current: query.current, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.customerName) params.customerName = query.customerName
    if (query.status) params.status = Number(query.status)
    await orderStore.fetchList(params)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
  // 加载客户列表供创建订单选择
  request.get('/customers', { params: { current: 1, size: 200 } })
    .then((res: any) => { customerList.value = res.data?.records || res.data || [] })
    .catch(() => {})
})
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

// 状态流程
.status-flow {
  display: flex; align-items: center; gap: 0; padding: 20px;
  flex-wrap: wrap;
}
.flow-step {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.flow-dot {
  width: 36px; height: 36px; border-radius: 50%;
  background: #e4e7ed; color: #909399;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600;
  &.active { background: #409eff; color: #fff; }
  &.completed { background: #67c23a; color: #fff; }
}
.flow-label {
  font-size: 12px; color: #909399;
  &.active { color: #409eff; font-weight: 600; }
}
.flow-line {
  width: 60px; height: 2px; background: #e4e7ed; margin: 0 10px;
  align-self: flex-start;
  margin-top: 18px;
}

// 弹窗
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-container {
  background: #fff; border-radius: 12px; width: 90%;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  max-height: 85vh; display: flex; flex-direction: column;
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px; border-bottom: 1px solid #f0f0f0;
  h3 { font-size: 16px; margin: 0; }
}
.modal-close {
  width: 30px; height: 30px; border: none; background: none;
  font-size: 20px; cursor: pointer; color: #909399; border-radius: 6px;
  &:hover { background: #f5f7fa; color: #303133; }
}
.modal-body { padding: 20px 24px; flex: 1; overflow-y: auto; }
.modal-footer {
  padding: 14px 24px; border-top: 1px solid #f0f0f0;
  display: flex; justify-content: flex-end; gap: 10px;
}

// 详情 Tab
.detail-tabs {
  display: flex; gap: 0; border-bottom: 1px solid #f0f0f0;
  margin: -20px -24px 20px; padding: 0 24px;
}
.detail-tab {
  padding: 12px 16px; cursor: pointer; font-size: 13px; font-weight: 500;
  border-bottom: 2px solid transparent; color: #909399; transition: all 0.2s;
  &:hover { color: #606266; }
  &.active { color: #409eff; border-bottom-color: #409eff; }
}
.detail-tab-content { padding: 4px 0; }

// 信息网格
.info-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}
.info-cell {
  background: #f5f7fa; padding: 12px; border-radius: 8px;
}
.info-label { font-size: 11px; color: #909399; margin-bottom: 4px; }
.info-value { font-size: 14px; font-weight: 600; }

// 财务摘要
.finance-summary {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;
}
.finance-card {
  background: #f5f7fa; border-radius: 8px; padding: 14px; text-align: center;
  border-top: 3px solid;
}
.finance-label { font-size: 11px; margin-bottom: 6px; }
.finance-value { font-size: 18px; font-weight: 700; }

// 表格
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td {
  padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0f0f0;
}
.data-table th { background: #fafbfc; color: #606266; font-weight: 600; font-size: 12px; }
.data-table tr:hover { background: #fafbfc; }
</style>
