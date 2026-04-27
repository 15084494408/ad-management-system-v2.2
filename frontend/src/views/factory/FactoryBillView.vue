<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="$router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item">客户管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">工厂账单</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🏭 工厂账单管理 <span class="v2-badge">V2.1</span></h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="showExportReport = true">📊 对账报表</button>
        <button class="btn btn-primary" @click="showCreate = true; loadFactories()">+ 生成账单</button>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:220px 1fr;gap:24px;align-items:start;">
      <!-- 工厂列表侧边栏 -->
      <div class="card" style="padding:12px;">
        <div
          class="config-sidebar-item"
          :class="{ active: selectedFactory === 'all' }"
          @click="selectFactory('all')"
        >
          <span>🏢 全部工厂</span>
        </div>
        <div
          v-for="f in factories" :key="f.id"
          class="config-sidebar-item"
          :class="{ active: selectedFactory === String(f.id) }"
          @click="selectFactory(String(f.id))"
        >
          <span>🏢 {{ f.factoryName || f.name }}</span>
        </div>
        <!-- 工厂列表为空时提示 -->
        <div v-if="factories.length === 0" style="padding:14px 12px;color:#909399;font-size:13px;text-align:center;line-height:1.6;">
          📭 暂无工厂数据<br>
          <span style="font-size:12px;">请先在系统管理中添加工厂</span>
        </div>
        <div style="margin-top:12px;padding:12px;background:var(--bg);border-radius:6px;">
          <div style="font-size:12px;color:var(--text2);margin-bottom:8px;">💡 快捷操作</div>
          <button class="btn btn-primary btn-sm" style="width:100%;font-size:13px;" @click="showCreate = true; loadFactories()">+ 生成账单</button>
        </div>
        <!-- 业务员管理入口 -->
        <div style="margin-top:8px;padding:12px;background:var(--bg);border-radius:6px;">
          <div style="font-size:12px;color:var(--text2);margin-bottom:8px;">👨‍💼 业务员管理</div>
          <button class="btn btn-default btn-sm" style="width:100%;font-size:13px;" @click="openSalesmanManager">
            📋 业务员管理
          </button>
        </div>
      </div>

      <!-- 账单内容 -->
      <div class="card">
        <div class="card-header">
          <div class="card-title">{{ cardTitle }}</div>
          <div style="display:flex;gap:10px;">
            <select v-model="monthFilter" class="form-control" style="width:130px;font-size:13px;" @change="loadBills">
              <option value="">全部月份</option>
              <option v-for="m in months" :key="m" :value="m">{{ m }}</option>
            </select>
            <select v-model="statusFilter" class="form-control" style="width:120px;font-size:13px;" @change="loadBills">
              <option value="">全部状态</option>
              <option value="未对账">未对账</option>
              <option value="已对账">已对账</option>
              <option value="已结清">已结清</option>
            </select>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-grid" style="margin-bottom:20px;">
          <div class="stat-card">
            <div class="stat-icon blue">📋</div>
            <div class="stat-info">
              <h3>{{ bills.length }}</h3>
              <p>账单数量</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon orange">💰</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalAmount }}</h3>
              <p>应付总额</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon green">✅</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalPaid }}</h3>
              <p>已付金额</p>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon red">⏳</div>
            <div class="stat-info">
              <h3>¥{{ stats.totalUnpaid }}</h3>
              <p>未付金额</p>
            </div>
          </div>
        </div>

        <!-- 当月账单状态提示（选择具体工厂 & 筛选当月 & 无账单时显示） -->
        <div v-if="selectedFactory !== 'all' && monthFilter && bills.length === 0"
             style="padding:20px 24px;background:var(--bg);border-radius:10px;display:flex;align-items:center;gap:16px;margin-bottom:20px;">
          <div style="font-size:36px;">📭</div>
          <div>
            <div style="font-size:15px;font-weight:600;color:var(--text);margin-bottom:4px;">
              {{ getFactoryNameById(selectedFactory) }} · {{ monthFilter }} 暂无账单
            </div>
            <div style="font-size:13px;color:var(--text2);">
              当月账单金额为 <strong style="color:var(--text);">¥0.00</strong>，
              <a href="javascript:void(0)" @click.prevent="showCreate = true; loadFactories()" style="color:var(--primary);">点击生成当月账单</a>
            </div>
          </div>
        </div>

        <!-- 表格 -->
        <table class="data-table">
          <thead>
            <tr>
              <th>账单编号</th>
              <th>工厂客户</th>
              <th>业务员</th>
              <th>账单月份</th>
              <th>账单金额</th>
              <th>已付金额</th>
              <th>未付金额</th>
              <th>账单状态</th>
              <th>备注</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="11" style="text-align:center;padding:40px;color:#909399;">
                <div class="loading-spinner" style="margin:0 auto 8px;"></div>加载中...
              </td>
            </tr>
            <tr v-for="b in bills" :key="b.id">
              <td><strong>{{ b.billNo || b.id }}</strong></td>
              <td>{{ b.factoryName || getFactoryName(b) }}</td>
              <td>{{ b.salesmanName || '-' }}</td>
              <td>{{ b.month }}</td>
              <td style="color:#e6a23c;font-weight:600;">¥{{ fmtMoney(b.totalAmount) }}</td>
              <td style="color:#67c23a;">¥{{ fmtMoney(b.paidAmount) }}</td>
              <td :style="{ color: getUnpaid(b) > 0 ? '#f56c6c' : '#909399', fontWeight: 600 }">
                ¥{{ fmtMoney(getUnpaid(b)) }}
              </td>
              <td>
                <span class="status-tag" :class="statusClass(b.status)">{{ statusLabel(b.status) }}</span>
              </td>
              <td style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="b.remark || ''">
                {{ b.remark || '-' }}
              </td>
              <td style="color:#909399;font-size:12px;">{{ b.createTime || b.date || '-' }}</td>
              <td class="action-btns">
                <button class="action-btn view" @click="viewDetail(b)">查看</button>
                <button class="action-btn edit" @click="reconcile(b)">对账</button>
                <button class="action-btn view" @click="handleExport">导出</button>
                <!-- 只有超级管理员才能看到删除按钮 -->
                <button
                  v-if="isSuperAdmin"
                  class="action-btn delete"
                  @click="confirmDeleteBill(b)"
                  title="删除此账单（仅超级管理员）"
                  :disabled="billDeleting">
                  删除
                </button>
              </td>
            </tr>
            <tr v-if="!loading && bills.length === 0">
              <td colspan="11" style="text-align:center;padding:40px;color:#c0c4cc;">暂无账单数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 生成账单弹窗 -->
    <div class="modal-overlay" :class="{ show: showCreate }" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">🏭 生成工厂账单</span>
          <button class="modal-close" @click="showCreate = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">工厂客户 *</label>
              <select v-model="createForm.factoryId" class="form-input" @change="onCreateFactoryChange">
                <option value="">请选择客户</option>
                <option v-for="f in factories" :key="f.id" :value="f.id">{{ f.factoryName || f.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">账单月份 *</label>
              <input v-model="createForm.month" type="month" class="form-input">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">负责业务员</label>
              <select v-model="createForm.salesmanId" class="form-input">
                <option value="">不指定（可选）</option>
                <option v-for="s in createSalesmen" :key="s.id" :value="s.id">{{ s.name }}{{ s.phone ? ' (' + s.phone + ')' : '' }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea v-model="createForm.remark" class="form-input" placeholder="可选，填写账单备注..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="createBill">💾 生成账单</button>
        </div>
      </div>
    </div>

    <!-- 对账弹窗 -->
    <div class="modal-overlay" :class="{ show: showReconcile }" @click.self="showReconcile = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">📊 账单对账</span>
          <button class="modal-close" @click="showReconcile = false">✕</button>
        </div>
        <div class="modal-body" v-if="reconcileData">
          <div style="text-align:center;padding:15px;background:#ecf5ff;border-radius:8px;margin-bottom:20px;">
            <div style="font-size:13px;color:#606266;">账单编号</div>
            <div style="font-size:18px;font-weight:600;color:#409eff;">{{ reconcileData.billNo || reconcileData.id }}</div>
            <div style="font-size:24px;font-weight:600;color:#e6a23c;margin-top:5px;">¥{{ fmtMoney(reconcileData.totalAmount) }}</div>
            <div style="font-size:12px;color:#909399;">应付总额</div>
          </div>
          <table style="width:100%;font-size:13px;">
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">订单总额</td>
              <td style="padding:10px;text-align:right;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(reconcileData.totalAmount) }}</td>
            </tr>
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">已付款项</td>
              <td style="padding:10px;text-align:right;color:#67c23a;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(reconcileData.paidAmount) }}</td>
            </tr>
            <tr>
              <td style="padding:10px;color:#909399;border-bottom:1px solid #f5f7fa;">本次应付</td>
              <td style="padding:10px;text-align:right;font-weight:600;border-bottom:1px solid #f5f7fa;">¥{{ fmtMoney(getUnpaid(reconcileData)) }}</td>
            </tr>
          </table>
          <div class="form-row" style="margin-top:20px;">
            <div class="form-group">
              <label class="form-label">付款金额</label>
              <input v-model="reconcileAmount" type="number" class="form-input" :placeholder="String(getUnpaid(reconcileData))" step="0.01">
            </div>
            <div class="form-group">
              <label class="form-label">付款方式</label>
              <select v-model="reconcileMethod" class="form-input">
                <option>银行转账</option>
                <option>现金</option>
                <option>支票</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showReconcile = false">取消</button>
          <button class="btn btn-success" @click="doReconcile">✅ 确认对账</button>
        </div>
      </div>
    </div>

    <!-- 账单详情弹窗 -->
    <div class="modal-overlay" :class="{ show: showDetail }" @click.self="showDetail = false">
      <div class="modal" style="max-width:820px;width:94%;max-height:90vh;overflow-y:auto;">
        <div class="modal-header">
          <span class="modal-title">📋 账单详情</span>
          <button class="modal-close" @click="showDetail = false">✕</button>
        </div>
        <div class="modal-body" v-if="detailData">
          <!-- 账单编号 + 状态 -->
          <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 16px;background:var(--bg);border-radius:8px;margin-bottom:16px;">
            <div>
              <div style="font-size:11px;color:var(--text2);margin-bottom:4px;">账单编号</div>
              <div style="font-size:20px;font-weight:700;color:var(--primary);">{{ detailData.billNo }}</div>
            </div>
            <span class="status-tag" :class="statusClass(detailData.status)" style="font-size:13px;padding:4px 14px;">
              {{ statusLabel(detailData.status) }}
            </span>
          </div>

          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">工厂名称</span>
                <span class="detail-value">{{ detailData.factoryName || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">账单月份</span>
                <span class="detail-value">{{ detailData.month || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">创建时间</span>
                <span class="detail-value">{{ formatDateTime(detailData.createTime) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">更新时间</span>
                <span class="detail-value">{{ formatDateTime(detailData.updateTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 金额信息 -->
          <div class="detail-section">
            <div class="detail-section-title">金额明细</div>
            <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;">
              <div class="amount-card amount-total">
                <div class="amount-label">账单总额</div>
                <div class="amount-value">¥{{ fmtMoney(detailData.totalAmount) }}</div>
              </div>
              <div class="amount-card amount-paid">
                <div class="amount-label">已付金额</div>
                <div class="amount-value">¥{{ fmtMoney(detailData.paidAmount) }}</div>
              </div>
              <div class="amount-card amount-unpaid">
                <div class="amount-label">未付金额</div>
                <div class="amount-value">¥{{ fmtMoney(getUnpaid(detailData)) }}</div>
              </div>
            </div>
          </div>

          <!-- ========== 每日登记明细列表（核心功能）========== -->
          <div class="detail-section">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
              <div class="detail-section-title" style="margin-bottom:0;">📝 每日登记明细</div>
              <div style="display:flex;gap:8px;">
                <button class="btn btn-success btn-sm" @click="openBatchAdd">📋 批量登记</button>
                <button class="btn btn-primary btn-sm" @click="showAddDetail = true">+ 单条登记</button>
              </div>
            </div>

            <!-- 明细加载中 -->
            <div v-if="detailsLoading" style="text-align:center;padding:30px;color:var(--text2);">
              <div class="loading-spinner" style="margin:0 auto 8px;"></div> 加载明细中...
            </div>

            <!-- 明细列表 -->
            <template v-else-if="billDetails.length > 0">
              <table class="data-table detail-table" style="font-size:12px;margin-top:0;">
                <thead>
                  <tr>
                    <th style="width:90px;">日期</th>
                    <th>项目名称</th>
                    <th style="width:130px;">规格说明</th>
                    <th style="width:70px;text-align:center;">计价方式</th>
                    <th style="width:60px;text-align:right;">数量/面积</th>
                    <th style="width:50px;text-align:center;">单位</th>
                    <th style="width:85px;text-align:right;">单价</th>
                    <th style="width:95px;text-align:right;font-weight:700;color:var(--primary);">小计金额</th>
                    <th style="width:50px;text-align:center;">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(d, idx) in billDetails" :key="d.id" :class="{ 'row-new': isNewRow(idx) }">
                    <td><strong>{{ d.recordDate || '-' }}</strong></td>
                    <td>{{ d.itemName }}</td>
                    <td style="color:var(--text2);">{{ d.spec || '-' }}</td>
                    <td style="text-align:center;">
                      <span class="calc-mode-badge" :class="'mode-' + d.calcMode">
                        {{ calcModeLabel(d.calcMode) }}
                      </span>
                    </td>
                    <!-- 按数量: 显示数量; 按面积: 显示面积(㎡); 固定价: 显示 - -->
                    <td style="text-align:right;">
                      <template v-if="d.calcMode === 2">
                        <strong style="color:#409eff;">{{ Number(d.areaSq || 0).toFixed(2) }}</strong>
                        <span style="font-size:10px;color:var(--text2);">㎡</span>
                      </template>
                      <template v-else-if="d.calcMode === 3">
                        <span style="color:var(--text2);">-</span>
                      </template>
                      <template v-else>
                        {{ d.quantity ?? 1 }}
                      </template>
                    </td>
                    <td style="text-align:center;">{{ d.unit || '-' }}</td>
                    <td style="text-align:right;">
                      <template v-if="d.calcMode === 2">
                        ¥{{ fmtMoney(d.unitPrice) }}/㎡
                      </template>
                      <template v-else>
                        ¥{{ fmtMoney(d.unitPrice) }}
                      </template>
                    </td>
                    <td style="text-align:right;font-weight:600;color:#e6a23c;">¥{{ fmtMoney(d.amount) }}</td>
                    <td style="text-align:center;">
                      <button class="action-btn delete-sm" title="删除此条记录"
                        @click="confirmDeleteDetail(d)" :disabled="deleteLoading">✕</button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr style="background:var(--bg);">
                    <td colspan="7" style="text-align:right;font-weight:600;color:var(--text2);padding-right:12px;">
                      明细合计（共 {{ billDetails.length }} 条）:
                    </td>
                    <td style="text-align:right;font-size:15px;font-weight:700;color:#e6a23c;border-top:2px solid var(--primary);">
                      ¥{{ fmtMoney(detailTotal) }}
                    </td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </template>

            <!-- 无明细 -->
            <div v-else style="text-align:center;padding:24px;background:var(--bg);border-radius:8px;color:var(--text2);">
              <div style="font-size:28px;margin-bottom:8px;">📭</div>
              <div style="font-size:13px;">暂无登记明细，点击上方「+ 新增登记」添加</div>
            </div>
          </div>

          <!-- 新增明细表单（内联展开） -->
          <div v-if="showAddDetail" class="detail-section" style="border:1px dashed var(--primary);padding:16px;border-radius:8px;background:linear-gradient(135deg,rgba(64,158,255,.03),rgba(102,177,255,.06));">
            <div class="detail-section-title" style="color:var(--primary);">➕ 新增登记记录</div>

            <!-- 第一排：基本信息（固定） -->
            <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;">
              <input v-model="addForm.itemName" class="form-input" placeholder="项目名称 *（如：名片印刷）" style="font-size:13px;" />
              <input v-model="addForm.spec" class="form-input" placeholder="规格说明（如：A4 双面彩印）" style="font-size:13px;" />
              <input type="date" v-model="addForm.recordDate" class="form-input" style="font-size:13px;" />
            </div>

            <!-- 计价方式选择 -->
            <div style="margin-top:12px;display:flex;gap:16px;align-items:center;flex-wrap:wrap;">
              <span style="font-size:13px;color:var(--text2);white-space:nowrap;">计价方式：</span>
              <label style="display:flex;align-items:center;gap:4px;cursor:pointer;font-size:13px;">
                <input type="radio" v-model.number="addForm.calcMode" :value="1" /> 按数量（张/本/个）
              </label>
              <label style="display:flex;align-items:center;gap:4px;cursor:pointer;font-size:13px;">
                <input type="radio" v-model.number="addForm.calcMode" :value="2" /> 📐 按面积(平方) — 广告材料常用
              </label>
              <label style="display:flex;align-items:center;gap:4px;cursor:pointer;font-size:13px;">
                <input type="radio" v-model.number="addForm.calcMode" :value="3" /> 💰 固定价格（一口价）
              </label>
            </div>

            <!-- 按数量时的输入区 -->
            <div v-if="addForm.calcMode === 1" style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-top:12px;">
              <input v-model.number="addForm.quantity" type="number" step="1" class="form-input" placeholder="数量（默认1）" style="font-size:13px;" min="0" />
              <input v-model="addForm.unit" class="form-input" :placeholder="'单位（' + (calcModeUnitHints[1] || '') + '）'" style="font-size:13px;" />
              <input v-model.number="addForm.unitPrice" type="number" step="0.01" class="form-input" :placeholder="'单价 *（元/' + (addForm.unit || '件') + '）'" style="font-size:13px;" min="0" />
            </div>

            <!-- 按面积(平方)时的输入区 -->
            <div v-if="addForm.calcMode === 2" style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:10px;margin-top:12px;">
              <input v-model.number="addForm.lengthVal" type="number" step="0.01" class="form-input" placeholder="长度 *（米）" style="font-size:13px;" min="0" />
              <input v-model.number="addForm.widthVal" type="number" step="0.01" class="form-input" placeholder="宽度 *（米）" style="font-size:13px;" min="0" />
              <input v-model="addForm.unit" class="form-input" :placeholder="'单位（' + (calcModeUnitHints[2] || '') + '）'" style="font-size:13px;" />
              <input v-model.number="addForm.unitPrice" type="number" step="0.01" class="form-input" placeholder="平米单价 *（元/㎡）" style="font-size:13px;" min="0" />
            </div>

            <!-- 按面积时的面积预览 -->
            <div v-if="addForm.calcMode === 2 && addFormPreview.area > 0"
                 style="margin-top:8px;padding:8px 12px;background:#ecf5ff;border-radius:6px;font-size:12px;color:#409eff;">
              📐 面积计算：{{ addForm.lengthVal }}m × {{ addForm.widthVal }}m = <strong>{{ addFormPreview.area.toFixed(3) }} ㎡</strong>
              &nbsp;|&nbsp; 预计金额：<strong>¥{{ fmtMoney(addFormPreview.amount) }}</strong>
            </div>

            <!-- 固定价格时的输入区 -->
            <div v-if="addForm.calcMode === 3" style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px;">
              <input v-model.number="addForm.unitPrice" type="number" step="0.01" class="form-input" placeholder="金额 *（元）" style="font-size:13px;" min="0" />
              <input v-model="addForm.unit" class="form-input" placeholder="说明（如：设计费/整单包干）" style="font-size:13px;" />
            </div>

            <!-- 金额预览（按数量/固定价时） -->
            <div v-if="(addForm.calcMode === 1 || addForm.calcMode === 3) && addFormPreview.amount > 0"
                 style="margin-top:8px;padding:8px 12px;background:#f0f9eb;border-radius:6px;font-size:12px;color:#67c23a;">
              💰 预计金额：<strong>¥{{ fmtMoney(addFormPreview.amount) }}</strong>
            </div>

            <input v-model="addForm.remark" class="form-input" placeholder="备注（可选）" style="font-size:13px;margin-top:10px;width:100%;" />
            <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:12px;">
              <button class="btn btn-default btn-sm" @click="showAddDetail = false; resetAddForm()">取消</button>
              <button class="btn btn-success btn-sm" @click="doAddDetail" :disabled="addDetailLoading">
                {{ addDetailLoading ? '保存中...' : '✅ 保存记录' }}
              </button>
            </div>
          </div>

          <!-- ========== 批量登记弹窗 ========== -->
          <div class="modal-overlay batch-modal" :class="{ show: showBatchAdd }" @click.self="showBatchAdd = false">
            <div class="batch-dialog">
              <div class="modal-header">
                <span class="modal-title">📋 批量登记明细</span>
                <button class="modal-close" @click="showBatchAdd = false">✕</button>
              </div>
              <div class="modal-body batch-body">
                <div style="font-size:12px;color:var(--text2);margin-bottom:12px;line-height:1.6;">
                  💡 在下方添加多个登记项目，填写完成后点击「批量保存」一次提交所有记录。<br>
                  每行可以独立选择计价方式（按数量/按面积/固定价格）。
                </div>

                <!-- 批量登记表格 -->
                <table class="data-table batch-table">
                  <thead>
                    <tr>
                      <th style="width:36px;text-align:center;">#</th>
                      <th style="width:115px;">日期</th>
                      <th style="width:180px;">项目名称 *</th>
                      <th style="width:100px;">规格</th>
                      <th style="width:85px;text-align:center;">计价方式</th>
                      <th style="width:75px;text-align:right;">数量/长度</th>
                      <th style="width:70px;text-align:right;">单位/宽度</th>
                      <th style="width:85px;text-align:right;">单价</th>
                      <th style="width:110px;">业务员</th>
                      <th style="width:95px;text-align:right;font-weight:600;">小计</th>
                      <th style="width:44px;"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in batchItems" :key="idx">
                      <td style="text-align:center;color:var(--text2);">{{ idx + 1 }}</td>
                      <td><input type="date" v-model="item.recordDate" class="form-input" style="font-size:12px;padding:5px 8px;"></td>
                      <td><input v-model="item.itemName" class="form-input" style="font-size:12px;padding:5px 8px;" placeholder="项目名称"></td>
                      <td><input v-model="item.spec" class="form-input" style="font-size:12px;padding:5px 8px;" placeholder="规格"></td>
                      <td>
                        <select v-model.number="item.calcMode" class="form-input" style="font-size:12px;padding:5px 4px;">
                          <option :value="1">按数量</option>
                          <option :value="2">按面积</option>
                          <option :value="3">固定价</option>
                        </select>
                      </td>
                      <td style="text-align:right;">
                        <input v-if="item.calcMode !== 3" v-model.number="item.quantity" type="number" step="0.01" class="form-input" style="font-size:12px;padding:5px 8px;text-align:right;" :placeholder="item.calcMode === 2 ? '长度(m)' : '数量'">
                        <span v-else style="color:var(--text2);">-</span>
                      </td>
                      <td style="text-align:right;">
                        <input v-if="item.calcMode === 2" v-model.number="item.widthVal" type="number" step="0.01" class="form-input" style="font-size:12px;padding:5px 8px;text-align:right;" placeholder="宽度(m)">
                        <input v-else v-model="item.unit" class="form-input" style="font-size:12px;padding:5px 8px;" placeholder="单位">
                      </td>
                      <td style="text-align:right;">
                        <input v-model.number="item.unitPrice" type="number" step="0.01" class="form-input" style="font-size:12px;padding:5px 8px;text-align:right;" placeholder="单价">
                      </td>
                      <td>
                        <select v-model="item.salesmanId" class="form-input" style="font-size:12px;padding:5px 4px;">
                          <option :value="null">不指定</option>
                          <option v-for="s in batchSalesmen" :key="s.id" :value="s.id">{{ s.name }}</option>
                        </select>
                      </td>
                      <td style="text-align:right;font-weight:600;color:var(--primary);">
                        ¥{{ calcBatchItemAmount(item).toFixed(2) }}
                      </td>
                      <td>
                        <button class="action-btn delete-sm" @click="removeBatchItem(idx)" v-if="batchItems.length > 1">✕</button>
                      </td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr style="background:var(--bg);">
                      <td colspan="9" style="text-align:right;font-weight:600;color:var(--text2);">
                        合计（共 {{ batchItems.length }} 项）：
                      </td>
                      <td style="text-align:right;font-weight:700;color:var(--primary);font-size:14px;">
                        ¥{{ batchTotal.toFixed(2) }}
                      </td>
                      <td></td>
                    </tr>
                  </tfoot>
                </table>

                <div style="margin-top:14px;display:flex;align-items:center;gap:10px;padding:10px 0;border-top:1px dashed #e4e7ed;">
                  <button class="btn btn-primary btn-sm" @click="addBatchRow">+ 添加一行</button>
                  <span style="font-size:12px;color:var(--text2);">点击按钮添加更多登记项目，支持多种计价方式混录</span>
                </div>
              </div>
              <div class="modal-footer">
                <button class="btn btn-default" @click="showBatchAdd = false">取消</button>
                <button class="btn btn-success" @click="doBatchAdd" :disabled="batchLoading">
                  {{ batchLoading ? '保存中...' : '💾 批量保存（' + batchItems.length + ' 项）' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 备注信息 -->
          <div class="detail-section" v-if="detailData.remark">
            <div class="detail-section-title">备注说明</div>
            <div style="padding:10px 14px;background:var(--bg);border-radius:6px;font-size:13px;color:var(--text2);line-height:1.6;">
              {{ detailData.remark }}
            </div>
          </div>

          <!-- 对账文件 -->
          <div class="detail-section" v-if="detailData.reconcileFile">
            <div class="detail-section-title">对账文件</div>
            <div style="padding:10px 14px;background:var(--bg);border-radius:6px;">
              <a :href="detailData.reconcileFile" target="_blank" style="color:var(--primary);font-size:13px;text-decoration:none;">
                📎 查看对账文件
              </a>
            </div>
          </div>
        </div>
        <div class="modal-body" v-else style="text-align:center;padding:40px;color:var(--text2);">
          <div class="loading-spinner" style="margin:0 auto 10px;"></div>
          加载中...
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showDetail = false">关闭</button>
          <button class="btn btn-primary" @click="reconcile(detailData!); showDetail = false">💳 去对账</button>
        </div>
      </div>
    </div>

    <!-- 对账报表弹窗 -->
    <div class="modal-overlay" :class="{ show: showExportReport }" @click.self="showExportReport = false">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">📊 对账报表</span>
          <button class="modal-close" @click="showExportReport = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">工厂客户</label>
              <select class="form-input">
                <option value="">全部客户</option>
                <option v-for="f in factories" :key="f.id">{{ f.factoryName || f.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">报表周期</label>
              <select class="form-input">
                <option>本月</option>
                <option>本季度</option>
                <option>本年</option>
                <option>自定义</option>
              </select>
            </div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:15px;">
            <input type="date" class="form-input" value="2026-01-01">
            <input type="date" class="form-input">
          </div>
          <div style="margin-top:20px;">
            <label style="font-size:13px;color:#606266;">导出内容</label>
            <div style="display:flex;flex-wrap:wrap;gap:10px;margin-top:10px;">
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox" checked> 账单汇总</label>
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox" checked> 订单明细</label>
              <label style="display:flex;align-items:center;gap:5px;cursor:pointer;"><input type="checkbox"> 付款记录</label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showExportReport = false">取消</button>
          <button class="btn btn-primary" @click="showExportReport = false; ElMessage.info('报表导出中...')">⬇️ 导出报表</button>
        </div>
      </div>
    </div>

    <!-- ========== 业务员管理弹窗 ========== -->
    <div class="modal-overlay" :class="{ show: showSalesmanManager }" @click.self="showSalesmanManager = false">
      <div class="modal" style="max-width:780px;width:96%;max-height:88vh;overflow-y:auto;">
        <div class="modal-header">
          <span class="modal-title">👨‍💼 工厂业务员管理</span>
          <button class="modal-close" @click="showSalesmanManager = false">✕</button>
        </div>
        <div class="modal-body">
          <!-- 业务员列表 -->
          <table class="data-table" style="font-size:13px;">
            <thead>
              <tr>
                <th style="width:40px;">#</th>
                <th>姓名</th>
                <th>电话</th>
                <th>邮箱</th>
                <th>所属工厂</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody v-if="!salesmanLoading">
              <tr v-for="(s, idx) in salesmenList" :key="s.id">
                <td style="color:#909399;">{{ idx + 1 }}</td>
                <td><strong>{{ s.name }}</strong></td>
                <td>{{ s.phone || '-' }}</td>
                <td style="color:#909399;font-size:12px;">{{ s.email || '-' }}</td>
                <td>{{ s.factoryName || '-' }}</td>
                <td>
                  <span class="status-tag" :class="s.status === 1 ? 'status-active' : 'status-pending'">
                    {{ s.status === 1 ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="action-btns">
                  <button class="action-btn edit" @click="editSalesman(s)">编辑</button>
                  <button class="action-btn delete-sm" title="删除"
                    @click="confirmDeleteSalesman(s)" :disabled="salesmanLoading">✕</button>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr><td colspan="7" style="text-align:center;padding:30px;color:#909399;">加载中...</td></tr>
            </tbody>
            <tfoot v-if="salesmenList.length === 0 && !salesmanLoading">
              <tr><td colspan="7" style="text-align:center;padding:30px;color:#c0c4cc;">暂无业务员数据，点击下方添加</td></tr>
            </tfoot>
          </table>

          <!-- 新增/编辑业务员表单 -->
          <div v-if="showSalesmanForm" style="margin-top:20px;border-top:1px dashed #e4e7ed;padding-top:16px;">
            <div style="font-size:14px;font-weight:600;margin-bottom:12px;color:var(--primary);">
              {{ salesmanForm.id ? '编辑' : '新增' }} 业务员
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
              <input v-model="salesmanForm.name" class="form-input" placeholder="姓名 *" />
              <input v-model="salesmanForm.phone" class="form-input" placeholder="联系电话" />
              <input v-model="salesmanForm.email" class="form-input" placeholder="邮箱（可选）" />
              <select v-model="salesmanForm.factoryId" class="form-input">
                <option :value="null">不绑定工厂（全局）</option>
                <option v-for="f in factories" :key="f.id" :value="f.id">{{ f.customerName || f.factoryName || f.name }}</option>
              </select>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr auto;gap:10px;margin-top:10px;">
              <input v-model="salesmanForm.remark" class="form-input" placeholder="备注（可选）" />
              <select v-model.number="salesmanForm.status" class="form-input">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
              <div style="display:flex;gap:8px;">
                <button class="btn btn-default" @click="cancelSalesmanForm">取消</button>
                <button class="btn btn-primary" @click="saveSalesman" :disabled="salesmanSaving">
                  {{ salesmanSaving ? '保存中...' : '💾 保存' }}
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showSalesmanManager = false">关闭</button>
          <button v-if="!showSalesmanForm" class="btn btn-primary" @click="addNewSalesman">+ 新增业务员</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { factoryApi, customerApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// 使用 computed 包装确保响应性 — 直接读取 store 的 isSuperAdmin（它内部依赖 userInfo）
const isSuperAdmin = computed(() => {
  // 兼容多种角色判断
  const roles = authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || roles.includes('ADMIN')
})

const factories = ref<any[]>([])
const bills = ref<any[]>([])
const selectedFactory = ref('all')
const monthFilter = ref('')
const statusFilter = ref('')
const loading = ref(false)
const showCreate = ref(false)
const showReconcile = ref(false)
const showExportReport = ref(false)
const showDetail = ref(false)
// 生成账单表单
const createForm = ref({ factoryId: null as number | null, month: '', salesmanId: null as number | null, remark: '' })
// 业务员管理相关
const showSalesmanManager = ref(false)
const salesmenList = ref<any[]>([])
const salesmanLoading = ref(false)
const salesmanSaving = ref(false)
const showSalesmanForm = ref(false)
const salesmanForm = ref({ id: null as number | null, name: '', phone: '', email: '', factoryId: null as number | null, status: 1, remark: '' })
// 生成账单时的业务员选项
const createSalesmen = ref<any[]>([])
const detailData = ref<any>(null)
const billDetails = ref<any[]>([])
const detailsLoading = ref(false)
// 新增明细表单
const showAddDetail = ref(false)
const addDetailLoading = ref(false)
const deleteLoading = ref(false)
const addForm = ref({
  recordDate: new Date().toISOString().split('T')[0],
  itemName: '',
  spec: '',
  // 计价方式: 1=按数量 2=按面积 3=固定价
  calcMode: 1,
  // 按数量时
  quantity: 1,
  unit: '',
  unitPrice: null as number | null,
  // 按面积时
  lengthVal: null as number | null,   // 长度(m)
  widthVal: null as number | null,    // 宽度(m)
  remark: ''
})

// ========== 批量登记相关 ==========
const showBatchAdd = ref(false)
const batchLoading = ref(false)
const batchSalesmen = ref<any[]>([]) // 批量登记时的业务员选项
const batchItems = ref<any[]>([
  {
    recordDate: new Date().toISOString().split('T')[0],
    itemName: '',
    spec: '',
    calcMode: 1,
    quantity: 1,
    unit: '',
    unitPrice: null,
    lengthVal: null,
    widthVal: null,
    salesmanId: null, // 业务员ID
    remark: ''
  }
])

async function openBatchAdd() {
  resetBatchForm()
  // 加载当前账单所属工厂的业务员选项
  const factoryId = detailData.value?.customerId || detailData.value?.factoryId
  if (factoryId) {
    try {
      const res = await factoryApi.getSalesmanOptions(factoryId)
      batchSalesmen.value = res.data || []
    } catch {
      batchSalesmen.value = []
    }
  } else {
    batchSalesmen.value = []
  }
  showBatchAdd.value = true
}

function resetBatchForm() {
  batchItems.value = [
    {
      recordDate: new Date().toISOString().split('T')[0],
      itemName: '',
      spec: '',
      calcMode: 1,
      quantity: 1,
      unit: '',
      unitPrice: null,
      lengthVal: null,
      widthVal: null,
      salesmanId: null,
      remark: ''
    }
  ]
}

function addBatchRow() {
  batchItems.value.push({
    recordDate: new Date().toISOString().split('T')[0],
    itemName: '',
    spec: '',
    calcMode: 1,
    quantity: 1,
    unit: '',
    unitPrice: null,
    lengthVal: null,
    widthVal: null,
    salesmanId: null,
    remark: ''
  })
}

function removeBatchItem(idx: number) {
  if (batchItems.value.length <= 1) return
  batchItems.value.splice(idx, 1)
}

function calcBatchItemAmount(item: any): number {
  const mode = item.calcMode || 1
  if (mode === 2) {
    // 按面积: 长 × 宽 × 平米单价
    const l = Number(item.lengthVal || 0)
    const w = Number(item.widthVal || 0)
    const p = Number(item.unitPrice || 0)
    return l * w * p
  } else if (mode === 3) {
    // 固定价格
    return Number(item.unitPrice || 0)
  } else {
    // 按数量: 数量 × 单价
    return (Number(item.quantity || 0)) * (Number(item.unitPrice || 0))
  }
}

const batchTotal = computed(() => {
  return batchItems.value.reduce((s: number, item: any) => s + calcBatchItemAmount(item), 0)
})
const reconcileData = ref<any>(null)
const reconcileAmount = ref(0)
const reconcileMethod = ref('银行转账')

const now = new Date()
const months = Array.from({ length: 6 }, (_, i) => {
  const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月`
})

const cardTitle = computed(() => {
  if (selectedFactory.value === 'all') return '🏢 全部工厂账单'
  const f = factories.value.find(f => String(f.id) === selectedFactory.value)
  return `🏢 ${f?.factoryName || f?.name || '未知'} - 账单明细`
})

const stats = computed(() => {
  const t = bills.value.reduce((s: number, b: any) => s + Number(b.totalAmount || 0), 0)
  const p = bills.value.reduce((s: number, b: any) => s + Number(b.paidAmount || 0), 0)
  return {
    totalAmount: t.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    totalPaid: p.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    totalUnpaid: (t - p).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
})

function getUnpaid(row: any): number {
  return Number((row.totalAmount || 0) - (row.paidAmount || 0))
}

/** 根据工厂ID获取工厂名称（用于无账单提示） */
function getFactoryNameById(id: string): string {
  const f = factories.value.find(f => String(f.id) === id)
  return f?.factoryName || f?.name || '未知工厂'
}

function getFactoryName(b: any): string {
  // 优先使用后端返回的 factoryName（已通过 customerId 校正）
  if (b.factoryName) return b.factoryName
  // 如果没有 factoryName，回退到根据 selectedFactory 查找
  if (selectedFactory.value !== 'all') {
    const f = factories.value.find(f => String(f.id) === selectedFactory.value)
    return f?.factoryName || f?.name || '-'
  }
  return '-'
}

function statusClass(status: any): string {
  const map: Record<string, string> = { 1: 'status-pending', 2: 'status-active', 3: 'status-active', 4: 'status-completed', '未对账': 'status-pending', '已对账': 'status-active', '已结清': 'status-completed' }
  return map[status] || 'status-pending'
}

function statusLabel(status: any): string {
  const map: Record<string, string> = { 1: '未对账', 2: '已对账', 3: '部分付款', 4: '已结清' }
  return map[status] || status || '未知'
}

function fmtMoney(v: any): string {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function selectFactory(id: string) {
  selectedFactory.value = id
  // 切换工厂时不自动切换月份，让用户看到该工厂所有账单
  await loadBills()
}

/** 加载工厂列表（独立函数，方便多处调用刷新） */
async function loadFactories() {
  try {
    const res = await factoryApi.getFactories()
    factories.value = res.data || []
  } catch (e: any) {
    factories.value = []
    ElMessage.error(e?.message || '加载工厂列表失败，请稍后重试')
  }
}

async function loadBills() {
  loading.value = true
  try {
    const params: any = { current: 1, size: 100 }
    if (selectedFactory.value !== 'all') params.factoryId = selectedFactory.value
    if (monthFilter.value) params.month = monthFilter.value
    if (statusFilter.value) {
      const sm: Record<string, number> = { '未对账': 1, '已对账': 2, '已结清': 4 }
      params.status = sm[statusFilter.value] || statusFilter.value
    }
    const res = await factoryApi.getBills(params)
    bills.value = res.data?.records || res.data?.list || []
  } catch {
    bills.value = []
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: any) {
  detailData.value = null
  billDetails.value = []
  showDetail.value = true
  showAddDetail.value = false
  resetAddForm()
  try {
    const res = await factoryApi.getBillDetail(row.id)
    detailData.value = res.data || row
    // 同时加载明细列表
    await loadBillDetails(row.id)
  } catch {
    // 接口失败时降级展示列表中已有数据
    detailData.value = row
    billDetails.value = []
  }
}

/** 加载账单明细 */
async function loadBillDetails(billId: number) {
  detailsLoading.value = true
  try {
    const res = await factoryApi.getBillDetails(billId)
    billDetails.value = res.data || []
  } catch {
    billDetails.value = []
  } finally {
    detailsLoading.value = false
  }
}

/** 明细合计金额 */
const detailTotal = computed(() => {
  return billDetails.value.reduce((s: number, d: any) => s + Number(d.amount || 0), 0)
})

/** 新增表单 — 根据计价模式实时计算预览金额 */
const addFormPreview = computed(() => {
  const f = addForm.value
  if (f.calcMode === 2) {
    // 按面积: 长 × 宽 × 平米单价
    const l = Number(f.lengthVal || 0)
    const w = Number(f.widthVal || 0)
    const p = Number(f.unitPrice || 0)
    const area = l * w
    return { area: area, amount: area * p }
  } else if (f.calcMode === 3) {
    // 固定价格
    return { area: 0, amount: Number(f.unitPrice || 0) }
  } else {
    // 按数量
    return { area: 0, amount: (Number(f.quantity || 0)) * (Number(f.unitPrice || 0)) }
  }
})

/** 计价模式标签 */
const calcModeLabels: Record<number, string> = { 1: '按数量', 2: '按面积(平方)', 3: '固定价格' }
/** 计价模式对应的单位提示 */
const calcModeUnitHints: Record<number, string> = { 1: '张/本/个/项', 2: '平方米', 3: '元(总价)' }
/** 获取计价模式标签 */
function calcModeLabel(mode: number): string {
  return calcModeLabels[mode] || '未知'
}

/** 判断是否为连续日期中的新日期行（用于视觉分组） */
function isNewRow(idx: number): boolean {
  if (idx === 0) return true
  const prev = billDetails.value[idx - 1]?.recordDate || ''
  const curr = billDetails.value[idx]?.recordDate || ''
  return prev !== curr
}

/** 重置新增表单 */
function resetAddForm() {
  addForm.value = {
    recordDate: new Date().toISOString().split('T')[0],
    itemName: '',
    spec: '',
    calcMode: 1,
    quantity: 1,
    unit: '',
    unitPrice: null,
    lengthVal: null,
    widthVal: null,
    remark: ''
  }
}

/** 提交新增明细 */
async function doAddDetail() {
  const f = addForm.value
  if (!f.itemName) { ElMessage.warning('请填写项目名称'); return }
  
  // 根据计价模式做不同的校验
  if (f.calcMode === 1) {
    // 按数量: 数量和单价必填
    if (f.unitPrice == null) { ElMessage.warning('请输入单价'); return }
  } else if (f.calcMode === 2) {
    // 按面积: 长、宽、平米单价必填
    if (!f.lengthVal || f.lengthVal <= 0) { ElMessage.warning('请输入长度(m)'); return }
    if (!f.widthVal || f.widthVal <= 0) { ElMessage.warning('请输入宽度(m)'); return }
    if (f.unitPrice == null) { ElMessage.warning('请输入每平方米单价'); return }
  } else if (f.calcMode === 3) {
    // 固定价格: 金额必填
    if (f.unitPrice == null || f.unitPrice <= 0) { ElMessage.warning('请输入金额'); return }
  }

  addDetailLoading.value = true
  try {
    await factoryApi.addBillDetail(detailData!.value!.id, f)
    ElMessage.success('登记成功')
    showAddDetail.value = false
    resetAddForm()
    // 重新加载明细和账单详情（因为后端会重算总额）
    await loadBillDetails(detailData!.value!.id)
    // 刷新详情数据以更新 totalAmount
    const res = await factoryApi.getBillDetail(detailData!.value!.id)
    detailData.value = res.data || detailData.value
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    addDetailLoading.value = false
  }
}

/** 确认删除明细 */
function confirmDeleteDetail(d: any) {
  if (!confirm(`确定要删除「${d.itemName}」这条记录吗？`)) return
  deleteLoading.value = true
  factoryApi.deleteBillDetail(detailData!.value!.id, d.id).then(async () => {
    ElMessage.success('已删除')
    await loadBillDetails(detailData!.value!.id)
    const res = await factoryApi.getBillDetail(detailData!.value!.id)
    detailData.value = res.data || detailData.value
  }).catch(e => ElMessage.error(e?.message || '删除失败')).finally(() => deleteLoading.value = false)
}

// ========== 账单级别操作（需超级管理员） ==========

const billDeleting = ref(false)

/**
 * 二次确认删除账单
 * 第一次确认：普通警告
 * 第二次确认：要求输入账单编号防止误删
 */
async function confirmDeleteBill(b: any) {
  // === 权限检查：只有超级管理员才能删除 ===
  // 如果 userInfo 未加载，先尝试拉取一次
  if (!authStore.userInfo) {
    try { await authStore.fetchUserInfo() } catch {}
  }
  
  const roles = authStore.userInfo?.roles || []
  if (!(roles.includes('SUPER_ADMIN') || roles.includes('ADMIN'))) {
    ElMessage.error('❌ 只有超级管理员/系统管理员才能删除账单')
    return
  }

  // === 第一次确认 ===
  const msg1 = `⚠️ 即将删除以下账单：\n\n` +
               `📋 编号：${b.billNo}\n` +
               `🏭 客户：${b.factoryName}\n` +
               `📅 月份：${b.month}\n` +
               `💰 金额：¥${fmtMoney(b.totalAmount)}\n\n` +
               `此操作不可恢复！确定要继续吗？`
  if (!confirm(msg1)) return

  // === 第二次确认（防误删）：必须输入账单编号 ===
  const inputMsg = `🔒 安全验证\n\n为了防止误操作，请输入账单编号「${b.billNo}」以确认删除：`
  const userInput = prompt(inputMsg, '')
  if (userInput === null) return // 用户点击了"取消"
  if (userInput.trim() !== b.billNo) {
    ElMessage.error('❌ 账单编号不匹配，删除已取消')
    return
  }

  // === 执行删除 ===
  billDeleting.value = true
  try {
    try {
      await factoryApi.deleteBill(b.id)
      ElMessage.success(`✅ 已删除账单 ${b.billNo}`)
    } catch (e: any) {
      console.warn('[delete] 接口返回异常但可能已删除:', e?.message)
      ElMessage.warning(`⚠️ 账单 ${b.billNo} 可能已删除，正在刷新列表...`)
    }
    // ★ 双重保障：先从本地数组移除（即时视觉反馈），再刷新后端数据
    bills.value = bills.value.filter((item: any) => item.id !== b.id)
    await loadBills()
  } catch (e) {
    console.error('[delete] 刷新列表失败:', e)
    // 即使后端刷新失败，本地也已移除该条
    bills.value = bills.value.filter((item: any) => item.id !== b.id)
  } finally {
    billDeleting.value = false
  }
}

/** 批量提交登记明细 */
async function doBatchAdd() {
  // 校验：至少一行，且每行项目名称必填
  if (batchItems.value.length === 0) {
    ElMessage.warning('请添加至少一项明细')
    return
  }
  
  for (let i = 0; i < batchItems.value.length; i++) {
    const item = batchItems.value[i]
    if (!item.itemName || !item.itemName.trim()) {
      ElMessage.warning(`第 ${i + 1} 行：请填写项目名称`)
      return
    }
    // 根据计价模式校验必填字段
    if (item.calcMode === 1 && item.unitPrice == null) {
      ElMessage.warning(`第 ${i + 1} 行：按数量计价时单价不能为空`)
      return
    }
    if (item.calcMode === 2) {
      if (!item.lengthVal || item.widthVal == null) {
        ElMessage.warning(`第 ${i + 1} 行：按面积计价时长和宽不能为空`)
        return
      }
      if (item.unitPrice == null) {
        ElMessage.warning(`第 ${i + 1} 行：按面积计价时平米单价不能为空`)
        return
      }
    }
    if (item.calcMode === 3 && (item.unitPrice == null || Number(item.unitPrice) <= 0)) {
      ElMessage.warning(`第 ${i + 1} 行：固定价格金额必须大于 0`)
      return
    }
  }

  // 确认弹窗
  if (!confirm(`确定要批量添加 ${batchItems.value.length} 条记录吗？\n合计金额：¥${batchTotal.value.toFixed(2)}`)) {
    return
  }

  batchLoading.value = true
  try {
    // 调用批量保存接口
    const res = await factoryApi.addBillDetailsBatch(detailData!.value!.id, batchItems.value)
    
    const savedIds = res.data || []
    ElMessage.success(`✅ 批量保存成功！共 ${savedIds.length} 条记录`)
    showBatchAdd.value = false
    
    // 重新加载明细和账单详情（后端会重算总额）
    await loadBillDetails(detailData!.value!.id)
    const billRes = await factoryApi.getBillDetail(detailData!.value!.id)
    detailData.value = billRes.data || detailData.value
  } catch (e: any) {
    ElMessage.error(e?.message || '批量保存失败')
  } finally {
    batchLoading.value = false
  }
}

/** 格式化日期时间 */
function formatDateTime(v: any): string {
  if (!v) return '-'
  if (v.includes && v.includes('T')) return v.replace('T', ' ').substring(0, 19)
  return String(v)
}

function reconcile(row: any) {
  reconcileData.value = row
  reconcileAmount.value = getUnpaid(row)
  showReconcile.value = true
}

async function doReconcile() {
  if (!reconcileData.value) return
  const amount = Number(reconcileAmount.value)
  if (!amount || amount <= 0) {
    ElMessage.warning('请输入有效的付款金额')
    return
  }
  const total = reconcileData.value.totalAmount || 0
  // 抹零提示：如果付款金额 < 应付总额（即抹了零头）
  let msg = `确认对账：${reconcileMethod.value} ¥${fmtMoney(amount)}`
  if (amount < total) {
    const diff = Number((total - amount).toFixed(2))
    msg += `\n⚠️ 抹零优惠：¥${fmtMoney(diff)}`
  } else if (amount > total) {
    msg += `\n⚠️ 多付金额：¥${fmtMoney(amount - total)}`
  }

  try {
    // 1. 更新账单状态为已对账
    await factoryApi.changeBillStatus(reconcileData.value.id, amount >= total ? 4 : 3)
    // 2. 更新实际付款金额（支持抹零）
    const newPaid = (Number(reconcileData.value.paidAmount || 0) + amount)
    await factoryApi.updatePaidAmount(reconcileData.value.id, newPaid)

    ElMessage.success(`✅ 对账成功！${reconcileMethod.value} ¥${fmtMoney(amount)}`)
    showReconcile.value = false
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.message || '对账失败')
  }
}

async function createBill() {
  if (!createForm.value.factoryId || !createForm.value.month) {
    ElMessage.warning('请选择工厂并填写账单月份')
    return
  }
  try {
    await factoryApi.createBill(createForm.value)
    ElMessage.success('账单生成成功')
    showCreate.value = false
    createForm.value = { factoryId: null, month: '', salesmanId: null, remark: '' }
    loadFactories()
    loadBills()
  } catch (e: any) {
    ElMessage.error(e?.message || '生成失败')
  }
}

/** 生成账单时切换工厂，自动加载该工厂的业务员选项 */
async function onCreateFactoryChange() {
  createForm.value.salesmanId = null
  const fid = createForm.value.factoryId
  if (fid) {
    try {
      const res = await factoryApi.getSalesmanOptions(fid)
      createSalesmen.value = res.data || []
    } catch {
      createSalesmen.value = []
    }
  } else {
    createSalesmen.value = []
  }
}

// ========== 业务员管理 ==========

/** 打开业务员管理弹窗 */
async function openSalesmanManager() {
  showSalesmanManager.value = true
  showSalesmanForm.value = false
  // 确保 factories 已加载（页面初始化时已加载，这里做二次保障）
  if (factories.value.length === 0) await loadFactories()
  await loadSalesmen()
}

/** 加载业务员列表 */
async function loadSalesmen() {
  salesmanLoading.value = true
  try {
    const res = await factoryApi.getSalesmen({})
    salesmenList.value = res.data || []
  } catch {
    salesmenList.value = []
  } finally {
    salesmanLoading.value = false
  }
}

/** 新增业务员（打开表单） */
function addNewSalesman() {
  salesmanForm.value = { id: null, name: '', phone: '', email: '', factoryId: null, status: 1, remark: '' }
  showSalesmanForm.value = true
}

/** 编辑业务员 */
function editSalesman(s: any) {
  salesmanForm.value = { id: s.id, name: s.name, phone: s.phone || '', email: s.email || '', factoryId: s.factoryId, status: s.status ?? 1, remark: s.remark || '' }
  showSalesmanForm.value = true
}

/** 取消编辑 */
function cancelSalesmanForm() {
  showSalesmanForm.value = false
  salesmanForm.value = { id: null, name: '', phone: '', email: '', factoryId: null, status: 1, remark: '' }
}

/** 保存业务员（新增/更新） */
async function saveSalesman() {
  if (!salesmanForm.value.name.trim()) { ElMessage.warning('请输入业务员姓名'); return }
  salesmanSaving.value = true
  try {
    if (salesmanForm.value.id) {
      await factoryApi.updateSalesman(salesmanForm.value.id, salesmanForm.value)
      ElMessage.success('✅ 业务员已更新')
    } else {
      await factoryApi.createSalesman(salesmanForm.value)
      ElMessage.success('✅ 业务员已添加')
    }
    cancelSalesmanForm()
    await loadSalesmen()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    salesmanSaving.value = false
  }
}

/** 删除业务员确认 */
function confirmDeleteSalesman(s: any) {
  if (!confirm(`确定要删除「${s.name}」吗？`)) return
  salesmanSaving.value = true
  factoryApi.deleteSalesman(s.id)
    .then(async () => {
      ElMessage.success('已删除')
      await loadSalesmen()
    })
    .catch((e: any) => ElMessage.error(e?.message || '删除失败'))
    .finally(() => salesmanSaving.value = false)
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(async () => {
  console.log('[FactoryBill] onMounted: 加载工厂和账单')
  await loadFactories()
  await loadBills()
})

onActivated(() => {
  console.log('[FactoryBill] onActivated: 重新加载工厂列表')
  loadFactories()
})
</script>

<style scoped>
.v2-badge {
  background: var(--success);
  color: #fff;
  font-size: 9px;
  padding: 2px 5px;
  border-radius: 3px;
  font-weight: normal;
}
.config-sidebar-item {
  padding: 12px 14px;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text2);
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: transparent;
  width: 100%;
  text-align: left;
}
.config-sidebar-item:hover {
  background: var(--bg);
  color: var(--primary);
}
.config-sidebar-item.active {
  background: linear-gradient(135deg, var(--primary), #66b1ff);
  color: #fff;
}

/* 账单详情 */
.detail-section {
  margin-bottom: 18px;
}
.detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--primary);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
  background: var(--bg);
  border-radius: 6px;
}
.detail-label {
  font-size: 11px;
  color: var(--text2);
  margin-bottom: 4px;
}
.detail-value {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}
.amount-card {
  padding: 14px;
  border-radius: 8px;
  text-align: center;
}
.amount-label {
  font-size: 11px;
  margin-bottom: 6px;
  opacity: 0.8;
}
.amount-value {
  font-size: 18px;
  font-weight: 700;
}
.amount-total {
  background: #fdf6ec;
  color: #e6a23c;
}
.amount-paid {
  background: #f0f9eb;
  color: #67c23a;
}
.amount-unpaid {
  background: #fef0f0;
  color: #f56c6c;
}

/* 明细表格 */
.detail-table {
  font-size: 12px !important;
  border-collapse: collapse;
}
.detail-table thead th {
  font-weight: 600;
  font-size: 11px;
  padding: 8px 6px;
}
.detail-table td {
  padding: 7px 6px;
}
.detail-table .row-new td:first-child::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--primary);
  border-radius: 2px 0 0 2px;
}
.delete-sm {
  width:24px;height:24px;padding:0;font-size:12px;color:#f56c6c;border:none;background:#fef0f0;border-radius:4px;cursor:pointer;transition:all .2s;}
.delete-sm:hover:not(:disabled){background:#f56c6c;color:#fff;}
.delete-sm:disabled{opacity:.4;cursor:not-allowed;}

/* 批量登记弹窗 — 独立全屏级大尺寸 + 毛玻璃效果 */
.batch-modal {
  z-index: 2100;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
}
.batch-dialog {
  max-width: 1200px;
  width: calc(100vw - 48px);
  height: calc(100vh - 80px);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}
.batch-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px !important;
}
.batch-table {
  font-size: 13px !important;
  table-layout: fixed;
  width: 100%;
}
.batch-table thead th {
  padding: 10px 8px;
  white-space: nowrap;
  background: var(--bg);
  position: sticky;
  top: 0;
  z-index: 1;
}
.batch-table td {
  padding: 9px 6px;
  vertical-align: middle;
}
.batch-table td input,
.batch-table td select {
  width: 100% !important;
}

/* 计价模式徽章 */
.calc-mode-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.calc-mode-badge.mode-1 {
  background: #ecf5ff;
  color: #409eff;
}
.calc-mode-badge.mode-2 {
  background: #fdf6ec;
  color: #e6a23c;
}
.calc-mode-badge.mode-3 {
  background: #f0f9eb;
  color: #67c23a;
}
</style>
