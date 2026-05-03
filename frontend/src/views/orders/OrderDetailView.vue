<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item" @click="router.push('/orders')">订单列表</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">订单详情</span>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:#909399;">加载中...</div>
    <template v-else-if="detailData">
      <!-- 头部信息 -->
      <div class="card detail-header">
        <div class="dh-left">
          <div class="dh-no">{{ detailData.orderNo }}</div>
          <span class="status-tag" :class="'status-' + statusKeyMap[detailData.status]">
            {{ statusLabelMap[detailData.status] }}
          </span>
          <!-- ⭐ 未收款醒目提示 -->
          <span v-if="unpaidAmount > 0" class="status-tag status-unpaid-warn">
            ⚠️ 未收款 ¥{{ formatMoney(unpaidAmount) }}
          </span>
        </div>
        <div class="dh-actions">
          <button class="btn btn-default" @click="router.push('/orders')">← 返回列表</button>
          <button class="btn btn-success" v-if="detailData.status === 1" @click="processVisible = true">⚡ 处理订单</button>
          <button class="btn btn-success" v-if="detailData.status === 2 && isPaymentComplete" @click="confirmDelivery">📦 确认交付</button>
          <span v-if="detailData.status === 2 && !isPaymentComplete" class="delivery-hint">⚠️ 请先收款</span>
          <button class="btn btn-primary" @click="paymentVisible = true">💰 登记收款</button>
        </div>
      </div>

      <!-- Tab 区 -->
      <div class="card" style="padding:0;">
        <div class="detail-tabs">
          <div class="detail-tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">📋 基本信息</div>
          <div class="detail-tab" :class="{ active: activeTab === 'material' }" @click="activeTab = 'material'">📦 物料明细</div>
          <div class="detail-tab" :class="{ active: activeTab === 'design' }" @click="activeTab = 'design'">
            🎨 设计文件
            <span v-if="detailDesignFiles.length" class="tab-badge">{{ detailDesignFiles.length }}</span>
          </div>
          <div class="detail-tab" :class="{ active: activeTab === 'finance' }" @click="activeTab = 'finance'">💰 财务信息</div>
        </div>

        <!-- 基本信息 -->
        <div v-if="activeTab === 'basic'" class="tab-content">
          <!-- ⭐ 未收款横幅提示 -->
          <div v-if="unpaidAmount > 0" class="payment-warn-banner">
            <span class="pwb-icon">💰</span>
            <span class="pwb-text">
              此订单尚有 <strong>¥{{ formatMoney(unpaidAmount) }}</strong> 未收齐
              <span v-if="detailData.paymentStatus === 1">（全部未付）</span>
              <span v-else>（部分付款）</span>
            </span>
            <button class="pwb-btn" @click="openPayment">立即收款</button>
          </div>
          <div class="info-grid">
            <div class="info-cell">
              <div class="info-label">订单编号</div>
              <div class="info-value">{{ detailData.orderNo }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单状态</div>
              <div><span class="status-tag" :class="'status-' + statusKeyMap[detailData.status]">{{ statusLabelMap[detailData.status] }}</span></div>
            </div>
            <div class="info-cell">
              <div class="info-label">客户名称</div>
              <div class="info-value">{{ detailData.customerName }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单金额</div>
              <div class="info-value" style="color:#f56c6c;font-weight:700;">¥{{ formatMoney(detailData.totalAmount) }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">交付日期</div>
              <div class="info-value">{{ detailData.deliveryDate || '-' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">设计师</div>
              <div class="info-value">{{ detailData.designerName || '待分配' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">订单类型</div>
              <div class="info-value">{{ orderTypeMap[detailData.orderType] || '其他' }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">支付状态</div>
              <div><span class="status-tag" :class="'status-' + payKeyMap[detailData.paymentStatus]">{{ payLabelMap[detailData.paymentStatus] }}</span></div>
            </div>
            <div class="info-cell">
              <div class="info-label">创建时间</div>
              <div class="info-value">{{ formatTime(detailData.createTime) }}</div>
            </div>
            <div class="info-cell">
              <div class="info-label">联系人</div>
              <div class="info-value">{{ detailData.contactPerson || '-' }}</div>
            </div>
          </div>
          <div style="margin-top:16px;">
            <div style="font-size:12px;color:#909399;margin-bottom:6px;">订单描述</div>
            <div style="background:#f5f7fa;padding:12px;border-radius:8px;font-size:13px;line-height:1.7;">{{ detailData.description || '暂无描述' }}</div>
          </div>
          <div style="margin-top:20px;">
            <div style="font-weight:600;margin-bottom:10px;">处理进度</div>
            <div class="status-flow" style="justify-content:flex-start;">
              <div class="flow-step" v-for="(s, i) in statusFlow" :key="i">
                <div class="flow-dot" :class="{ completed: i < detailStatusIdx, active: i === detailStatusIdx }">
                  {{ i < detailStatusIdx ? '✓' : i + 1 }}
                </div>
                <div class="flow-label" :class="{ active: i <= detailStatusIdx }">{{ s }}</div>
              </div>
              <div class="flow-line" v-for="i in statusFlow.length - 1" :key="'line-' + i"></div>
            </div>
            <!-- ⭐ 进行中但未收款的提示 -->
            <div v-if="detailData.status === 2 && !isPaymentComplete" style="margin-top:12px;display:flex;align-items:center;gap:8px;padding:8px 12px;background:#fef0f0;border-radius:6px;font-size:13px;color:#f56c6c;">
              <span>⚠️</span>
              <span>进行中·<strong>未收款</strong> —— 请先完成收款后再确认交付</span>
            </div>
          </div>
        </div>

        <!-- 物料明细 -->
        <div v-if="activeTab === 'material'" class="tab-content">
          <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">共 {{ detailMaterials.length }} 项物料/工艺</span>
            <span v-if="canEditCost" style="font-size:12px;color:#909399;">💡 点击成本列可编辑</span>
          </div>
          <table class="data-table" v-if="detailMaterials.length">
            <thead>
              <tr>
                <th>物料/工艺</th><th>规格</th><th>数量</th><th>单价</th>
                <th>小计</th>
                <th v-if="canViewCost">成本价</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in detailMaterials" :key="m.id">
                <td>{{ m.materialName }}</td>
                <td>{{ m.spec || '-' }}</td>
                <td>{{ m.quantity }} {{ m.unit || '' }}</td>
                <td>¥{{ formatMoney(m.unitPrice) }}</td>
                <td style="font-weight:600;color:#67c23a;">¥{{ formatMoney(m.amount) }}</td>
                <td v-if="canViewCost" style="min-width:120px;">
                  <div v-if="canEditCost && editingMaterialId === m.id" style="display:flex;align-items:center;gap:4px;">
                    <input type="number" class="cost-input" v-model.number="editingCost" style="width:80px;" min="0" step="0.01">
                    <button class="btn-icon btn-confirm" @click="saveCost(m)" title="保存">✓</button>
                    <button class="btn-icon btn-cancel" @click="cancelEditCost" title="取消">✗</button>
                  </div>
                  <div v-else class="cost-cell" :class="{ 'cost-zero': !m.unitCost }" @click="startEditCost(m)" :title="canEditCost ? '点击编辑成本' : ''">
                    {{ m.unitCost != null ? '¥' + formatMoney(m.unitCost) : '—' }}
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else style="text-align:center;color:#c0c4cc;padding:20px;">暂无物料明细</p>
          <div v-if="detailMaterialTotal > 0" style="margin-top:14px;padding:12px 16px;background:#f0f9eb;border-radius:8px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">物料合计：</span>
            <span style="font-size:18px;font-weight:700;color:#67c23a;">¥ {{ formatMoney(detailMaterialTotal) }}</span>
          </div>
        </div>

        <!-- 设计文件 -->
        <div v-if="activeTab === 'design'" class="tab-content">
          <div style="margin-bottom:16px;display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:13px;color:#606266;">
              共 {{ detailDesignFiles.length }} 个设计文件
              <span v-if="detailDesignFiles.length > 0" style="margin-left:8px;color:#909399;">
                ( 支持 .cdr .psd .ai .pdf .png .jpg 等格式 )
              </span>
            </span>
            <button class="btn btn-primary" style="font-size:13px;padding:6px 16px;" @click="uploadVisible = true">+ 上传文件</button>
          </div>

          <!-- 文件网格 -->
          <div v-if="detailDesignFiles.length" class="file-grid">
            <div v-for="file in detailDesignFiles" :key="file.id" class="file-card" @click="previewFile(file)">
              <!-- 缩略图/图标区域 -->
              <div class="file-thumb" :class="'ext-' + getExtClass(file.extension)">
                <template v-if="isImageFile(file.extension)">
                  <img v-if="file.url" :src="file.url" class="file-thumb-img" loading="lazy" @error="handleImgError($event)" />
                  <span v-else class="file-thumb-icon">{{ file.originalName }}</span>
                </template>
                <template v-else>
                  <div class="file-thumb-icon-wrap">
                    <span class="file-ext-label">{{ formatExt(file.extension) }}</span>
                    <span class="file-app-label">{{ getAppName(file.extension) }}</span>
                  </div>
                </template>
              </div>
              <!-- 文件信息 -->
              <div class="file-info">
                <div class="file-name" :title="file.originalName">{{ file.originalName || file.name }}</div>
                <div class="file-meta">
                  <span class="file-version">V{{ file.version }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  <span class="file-uploader">{{ file.uploaderName }}</span>
                </div>
                <div class="file-meta" style="margin-top:2px;">
                  <span class="file-status" :class="'file-status-' + file.status">
                    {{ ['', '待审核', '已通过', '已驳回'][file.status] || '未知' }}
                  </span>
                  <span class="file-time">{{ formatTime(file.createTime) }}</span>
                </div>
              </div>
              <!-- 操作按钮 -->
              <div class="file-actions" @click.stop>
                <button v-if="file.status === 2 && (file.url || file.name)" class="btn-icon" title="下载" @click="downloadFile(file)">📥</button>
                <button v-if="file.status === 1" class="btn-icon" style="color:#67c23a;" title="通过" @click="approveFile(file)">✓</button>
                <button v-if="file.status === 1" class="btn-icon" style="color:#f56c6c;" title="驳回" @click="rejectFile(file)">✗</button>
                <button class="btn-icon" style="color:#909399;" title="删除" @click="deleteFile(file)">🗑</button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="file-empty">
            <div style="font-size:48px;margin-bottom:12px;">🎨</div>
            <div style="font-size:14px;color:#909399;margin-bottom:6px;">暂无设计文件</div>
            <div style="font-size:12px;color:#c0c4cc;">点击上方"上传文件"按钮，添加 CDR、PSD、AI 等设计源文件</div>
          </div>
        </div>

        <!-- 财务信息 -->
        <div v-if="activeTab === 'finance'" class="tab-content">
          <div class="finance-summary">
            <div class="finance-card" style="border-color:#67c23a;">
              <div class="finance-label" style="color:#67c23a;">订单总额</div>
              <div class="finance-value" style="color:#67c23a;">¥{{ formatMoney(detailData.totalAmount) }}</div>
            </div>
            <div class="finance-card" style="border-color:#409eff;">
              <div class="finance-label" style="color:#409eff;">已收金额</div>
              <div class="finance-value" style="color:#409eff;">¥{{ formatMoney(detailData.paidAmount) }}</div>
            </div>
            <div class="finance-card" v-if="(detailData?.roundingAmount || 0) > 0" style="border-color:#e6a23c;">
              <div class="finance-label" style="color:#e6a23c;">抹零金额</div>
              <div class="finance-value" style="color:#e6a23c;">-¥{{ formatMoney(detailData?.roundingAmount) }}</div>
            </div>
            <div class="finance-card" style="border-color:#f56c6c;">
              <div class="finance-label" style="color:#f56c6c;">待收余额</div>
              <div class="finance-value" style="color:#f56c6c;">¥{{ formatMoney((detailData.totalAmount || 0) - (detailData.paidAmount || 0) - (detailData?.roundingAmount || 0)) }}</div>
            </div>
          </div>

          <!-- 成本利润区（管理员/财务可见） -->
          <div v-if="canViewCost" style="margin-top:20px;">
            <div style="font-weight:600;margin-bottom:10px;">💰 成本与利润</div>
            <div class="finance-summary">
              <div class="finance-card" style="border-color:#909399;">
                <div class="finance-label" style="color:#909399;">总成本</div>
                <div class="finance-value" style="color:#909399;">¥{{ formatMoney(detailTotalCost) }}</div>
              </div>
              <div class="finance-card" style="border-color:#e6a23c;">
                <div class="finance-label" style="color:#e6a23c;">设计师提成</div>
                <div class="finance-value" style="color:#e6a23c;">¥{{ formatMoney(detailCommission) }}</div>
              </div>
              <div class="finance-card" style="border-color:#9c27b0;">
                <div class="finance-label" style="color:#9c27b0;font-weight:600;">💎 预估利润</div>
                <div class="finance-value" :style="{ color: detailProfit >= 0 ? '#67c23a' : '#f56c6c' }">
                  ¥{{ formatMoney(detailProfit) }}
                </div>
              </div>
            </div>
            <div v-if="detailProfit === 0 && (detailTotalCost === 0 || detailTotalCost === '0' || detailTotalCost === 0.00)" style="margin-top:10px;padding:10px 14px;background:#fffbe6;border-radius:8px;font-size:13px;color:#856404;">
              💡 当前成本未填写，请在物料明细中为每项物料填入成本价，利润将自动计算
            </div>
          </div>
        </div>
      </div>
    </template>
    <div v-else style="text-align:center;padding:40px;color:#909399;">订单不存在</div>

    <!-- 处理订单弹窗 -->
    <div class="modal-overlay" v-if="processVisible" @click.self="processVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header"><h3>⚡ 处理订单</h3><button class="modal-close" @click="processVisible = false">&times;</button></div>
        <div class="modal-body">
          <div style="text-align:center;padding:16px 0;">
            <div style="width:60px;height:60px;background:#ecf5ff;border-radius:50%;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;font-size:30px;">📋</div>
            <h3 style="margin-bottom:8px;">确认接收此订单？</h3>
            <p style="color:#909399;font-size:13px;">订单 {{ detailData?.orderNo }} 将进入设计阶段</p>
          </div>
          <div class="form-row">
            <div class="form-group" style="flex:1;">
              <label class="form-label">分配设计师 *</label>
              <select class="form-input" v-model="processForm.designerId">
                <option :value="null">请选择</option>
                <option v-for="d in designerList" :key="d.id" :value="d.id">{{ d.realName || d.username }}</option>
              </select>
            </div>
            <div class="form-group" style="flex:1;">
              <label class="form-label">交付日期</label>
              <input type="date" class="form-input" v-model="processForm.deliveryDate">
            </div>
          </div>
          <div class="form-row" style="margin-top:12px;">
            <div class="form-group" style="flex:1;">
              <label class="form-label">交付日期</label>
              <input type="date" class="form-input" v-model="processForm.deliveryDate">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="processVisible = false">取消</button>
          <button class="btn btn-success" @click="submitProcess">✅ 确认接收</button>
        </div>
      </div>
    </div>

    <!-- 登记收款弹窗 -->
    <div class="modal-overlay" v-if="paymentVisible" @click.self="paymentVisible = false">
      <div class="modal-container" style="max-width:520px;">
        <div class="modal-header"><h3>💰 登记收款</h3><button class="modal-close" @click="paymentVisible = false">&times;</button></div>
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
              <option value="微信">微信</option><option value="支付宝">支付宝</option>
              <option value="银行转账">银行转账</option><option value="现金">现金</option>
            </select>
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
          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea class="form-input" v-model="paymentForm.remark" rows="2" placeholder="可选"></textarea>
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

    <!-- 上传设计文件弹窗 -->
    <div class="modal-overlay" v-if="uploadVisible" @click.self="uploadVisible = false">
      <div class="modal-container" style="max-width:540px;">
        <div class="modal-header">
          <h3>🎨 上传设计文件</h3>
          <button class="modal-close" @click="uploadVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="upload-zone" :class="{ 'upload-dragover': isDragOver }"
               @dragover.prevent="isDragOver = true"
               @dragleave="isDragOver = false"
               @drop.prevent="handleDrop">
            <div style="font-size:36px;margin-bottom:8px;">📁</div>
            <div style="font-size:14px;color:#606266;margin-bottom:4px;">拖拽文件到此处，或点击选择</div>
            <div style="font-size:12px;color:#c0c4cc;">支持 CDR / PSD / AI / PDF / PNG / JPG / SVG / EPS / TIFF，单文件最大 200MB</div>
            <input ref="fileInputRef" type="file" multiple accept=".cdr,.psd,.ai,.pdf,.png,.jpg,.jpeg,.svg,.eps,.tiff,.tif,.zip,.rar"
                   style="display:none;" @change="handleFileSelect" />
            <button class="btn btn-default" style="margin-top:12px;" @click="($refs.fileInputRef as HTMLInputElement)?.click()">选择文件</button>
          </div>
          <!-- 已选文件列表 -->
          <div v-if="selectedFiles.length" style="margin-top:16px;">
            <div style="font-size:13px;font-weight:600;margin-bottom:8px;">已选择 {{ selectedFiles.length }} 个文件</div>
            <div v-for="(f, i) in selectedFiles" :key="i" class="upload-file-item">
              <span class="upload-file-ext" :class="'ext-' + getExtClass(getFileExt(f.name))">{{ formatExt(getFileExt(f.name)) }}</span>
              <span class="upload-file-name" :title="f.name">{{ f.name }}</span>
              <span class="upload-file-size">{{ formatFileSize(f.size) }}</span>
              <button class="btn-icon btn-cancel" @click="selectedFiles.splice(i, 1)" style="font-size:14px;">&times;</button>
            </div>
          </div>
          <div class="form-group" style="margin-top:16px;">
            <label class="form-label">文件描述（可选）</label>
            <textarea class="form-input" v-model="uploadDescription" rows="2" placeholder="例如：封面设计终稿"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="uploadVisible = false">取消</button>
          <button class="btn btn-primary" @click="submitUpload" :disabled="uploading">
            {{ uploading ? '上传中...' : '开始上传' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 文件预览弹窗 -->
    <div class="modal-overlay" v-if="previewVisible" @click.self="previewVisible = false">
      <div class="modal-container" style="max-width:700px;">
        <div class="modal-header">
          <h3>{{ previewFileData?.originalName || '文件预览' }}</h3>
          <button class="modal-close" @click="previewVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <!-- 图片预览 -->
          <div v-if="previewFileData && isImageFile(previewFileData.extension) && previewFileData.url" class="preview-image-wrap">
            <img :src="previewFileData.url" style="max-width:100%;max-height:60vh;border-radius:8px;" />
          </div>
          <!-- 非图片文件信息 -->
          <div v-else class="preview-file-detail">
            <div class="preview-file-icon-big" :class="'ext-' + getExtClass(previewFileData?.extension)">
              {{ formatExt(previewFileData?.extension) }}
            </div>
            <div style="margin-top:16px;">
              <div class="info-grid">
                <div class="info-cell">
                  <div class="info-label">文件名</div>
                  <div class="info-value">{{ previewFileData?.originalName }}</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">类型</div>
                  <div class="info-value">{{ formatExt(previewFileData?.extension) }} ({{ getAppName(previewFileData?.extension) }})</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">大小</div>
                  <div class="info-value">{{ formatFileSize(previewFileData?.size) }}</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">版本</div>
                  <div class="info-value">V{{ previewFileData?.version }}</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">上传人</div>
                  <div class="info-value">{{ previewFileData?.uploaderName }}</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">上传时间</div>
                  <div class="info-value">{{ formatTime(previewFileData?.createTime) }}</div>
                </div>
                <div class="info-cell" v-if="previewFileData?.description">
                  <div class="info-label">描述</div>
                  <div class="info-value">{{ previewFileData?.description }}</div>
                </div>
                <div class="info-cell">
                  <div class="info-label">审核状态</div>
                  <div><span class="file-status" :class="'file-status-' + previewFileData?.status">{{ ['', '待审核', '已通过', '已驳回'][previewFileData?.status] }}</span></div>
                </div>
              </div>
            </div>
            <div v-if="previewFileData?.remark" style="margin-top:12px;padding:10px;background:#fef0f0;border-radius:6px;font-size:13px;color:#f56c6c;">
              <strong>驳回原因：</strong>{{ previewFileData.remark }}
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button v-if="previewFileData?.url || previewFileData?.name" class="btn btn-default" @click="downloadFile(previewFileData!)">📥 下载文件</button>
          <button class="btn btn-default" @click="previewVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi, designFileApi, designerApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const activeTab = ref('basic')

// 成本/利润仅管理员和财务可见
const canViewCost = computed(() => {
  const roles = authStore.userInfo?.roles || []
  return roles.includes('SUPER_ADMIN') || roles.includes('FINANCE') || roles.includes('ADMIN')
})
const canEditCost = canViewCost  // 可查看即可编辑成本

const statusFlow = ['待确认', '进行中', '已完成', '已取消']
const statusLabelMap: Record<number, string> = { 1: '待确认', 2: '进行中', 3: '已完成', 4: '已取消' }
const statusKeyMap: Record<number, string> = { 1: 'pending', 2: 'designing', 3: 'completed', 4: 'cancelled' }
const payLabelMap: Record<number, string> = { 1: '未付款', 2: '部分付', 3: '已付清', 4: '已抹零结清' }
const payKeyMap: Record<number, string> = { 1: 'cancelled', 2: 'delivery', 3: 'completed' }
const orderTypeMap: Record<number, string> = { 1: '图文打印', 2: '广告制作', 3: '设计服务', 4: '装订服务', 5: '其他' }

const detailData = ref<any>(null)
const detailMaterials = ref<any[]>([])
const detailDesignFiles = ref<any[]>([])
const detailMaterialTotal = computed(() => detailMaterials.value.reduce((s: number, m: any) => s + ((m.quantity || 0) * (m.unitPrice || 0)), 0))
const detailStatusIdx = computed(() => {
  const s = detailData.value?.status
  if (s === 1) return 0; if (s === 2) return 1; if (s === 3) return 2; return 0
})

// 成本相关计算（从 API 返回的 detailData 中取）
const detailTotalCost = computed(() => detailData.value?.totalCost || 0)
const detailCommission = computed(() => detailData.value?.designerCommission || 0)
const detailProfit = computed(() => {
  const total = detailData.value?.totalAmount || 0
  const cost = detailData.value?.totalCost || 0
  const commission = detailData.value?.designerCommission || 0
  return total - cost - commission
})

const processVisible = ref(false)
const processForm = reactive({
  designerId: null as number | null,
  deliveryDate: '',
})
const designerList = ref<any[]>([])
const paymentVisible = ref(false)
const paymentForm = reactive({ amount: 0, method: '微信', remark: '', writeOff: false })
const remainingAmount = computed(() => {
  const total = detailData.value?.totalAmount || 0
  const paid = detailData.value?.paidAmount || 0
  return Math.max(total - paid, 0)
})

// 未收金额（抵扣抹零/优惠）
const unpaidAmount = computed(() => {
  const total = detailData.value?.totalAmount || 0
  const paid = detailData.value?.paidAmount || 0
  const rounding = detailData.value?.roundingAmount || 0
  const discount = detailData.value?.discountAmount || 0
  return Math.max(total - paid - rounding - discount, 0)
})

// 是否已收全款（可交付）
const isPaymentComplete = computed(() => {
  const ps = detailData.value?.paymentStatus
  return ps === 3 || ps === 4 || unpaidAmount.value <= 0
})

// 物料成本编辑
const editingMaterialId = ref<number | null>(null)
const editingCost = ref<number>(0)

// ===== 设计文件相关 =====
const uploadVisible = ref(false)
const uploadDescription = ref('')
const uploading = ref(false)
const isDragOver = ref(false)
const selectedFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)

const previewVisible = ref(false)
const previewFileData = ref<any>(null)

// 文件扩展名分类
const IMAGE_EXTS = ['.jpg', '.jpeg', '.png', '.gif', '.svg', '.webp', '.bmp']

function getFileExt(filename: string): string {
  if (!filename) return ''
  const idx = filename.lastIndexOf('.')
  return idx >= 0 ? filename.substring(idx).toLowerCase() : ''
}

function formatExt(ext: string): string {
  return (ext || '').replace('.', '').toUpperCase()
}

function getExtClass(ext: string): string {
  const e = (ext || '').replace('.', '').toLowerCase()
  const map: Record<string, string> = {
    cdr: 'corel', ai: 'illustrator', psd: 'photoshop', pdf: 'pdf',
    png: 'image', jpg: 'image', jpeg: 'image', gif: 'image', svg: 'image', webp: 'image',
    eps: 'eps', tiff: 'image', tif: 'image', zip: 'archive', rar: 'archive',
    indd: 'indesign', idml: 'indesign', sketch: 'sketch',
    txt: 'text', doc: 'word', docx: 'word', xls: 'excel', xlsx: 'excel',
  }
  return map[e] || 'default'
}

function getAppName(ext: string): string {
  const e = (ext || '').replace('.', '').toLowerCase()
  const map: Record<string, string> = {
    cdr: 'CorelDRAW', ai: 'Illustrator', psd: 'Photoshop', pdf: 'PDF',
    eps: 'Illustrator', tiff: 'TIFF', tif: 'TIFF',
    indd: 'InDesign', idml: 'InDesign', sketch: 'Sketch',
    zip: '压缩包', rar: '压缩包',
  }
  return map[e] || ''
}

function isImageFile(ext: string): boolean {
  return IMAGE_EXTS.includes((ext || '').toLowerCase())
}

function handleImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
  const parent = img.parentElement
  if (parent) {
    const span = document.createElement('span')
    span.className = 'file-thumb-icon'
    span.textContent = '加载失败'
    parent.appendChild(span)
  }
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 文件选择 & 拖拽
function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    selectedFiles.value.push(...Array.from(input.files))
  }
  input.value = ''  // 重置以允许重复选择同一文件
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  if (e.dataTransfer?.files) {
    selectedFiles.value.push(...Array.from(e.dataTransfer.files))
  }
}

// 上传文件
async function submitUpload() {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }
  uploading.value = true
  const token = localStorage.getItem('token') || ''
  let successCount = 0
  let failCount = 0

  for (const file of selectedFiles.value) {
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('orderId', String(detailData.value.id))
      if (uploadDescription.value) formData.append('description', uploadDescription.value)

      const res = await fetch(designFileApi.uploadUrl, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
      })
      if (res.ok) successCount++
      else failCount++
    } catch {
      failCount++
    }
  }

  uploading.value = false
  selectedFiles.value = []
  uploadDescription.value = ''
  uploadVisible.value = false

  if (failCount === 0) {
    ElMessage.success(`成功上传 ${successCount} 个文件`)
  } else {
    ElMessage.warning(`上传完成：${successCount} 成功，${failCount} 失败`)
  }
  loadDetail()
}

// 文件操作
function previewFile(file: any) {
  previewFileData.value = file
  previewVisible.value = true
}

function downloadFile(file: any) {
  const url = file.url || file.name
  if (url) {
    window.open(url.startsWith('http') ? url : '/api/design/file/download?path=' + encodeURIComponent(url))
  } else {
    ElMessage.warning('文件路径不可用')
  }
}

async function approveFile(file: any) {
  try {
    await designFileApi.updateStatus(file.id, 2)
    ElMessage.success('已通过审核')
    loadDetail()
  } catch { /* error handled by interceptor */ }
}

async function rejectFile(file: any) {
  const remark = prompt('请输入驳回原因（可选）：')
  if (remark === null) return  // 用户点了取消
  try {
    await designFileApi.updateStatus(file.id, 3, remark || '')
    ElMessage.success('已驳回')
    loadDetail()
  } catch { /* error handled by interceptor */ }
}

async function deleteFile(file: any) {
  if (!confirm(`确认删除文件 "${file.originalName || file.name}"？此操作不可恢复。`)) return
  try {
    await designFileApi.delete(file.id)
    ElMessage.success('已删除')
    loadDetail()
  } catch { /* error handled by interceptor */ }
}

function startEditCost(m: any) {
  editingMaterialId.value = m.id
  editingCost.value = m.unitCost || 0
}
async function saveCost(m: any) {
  if (!canEditCost.value) return
  await orderApi.updateMaterial(detailData.value.id, m.id, { unitCost: editingCost.value })
  editingMaterialId.value = null
  loadDetail()
}
function cancelEditCost() {
  editingMaterialId.value = null
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await orderApi.getDetail(Number(route.params.id))
    const d = res.data
    detailData.value = d?.order || d
    detailMaterials.value = d?.materials || []
    detailDesignFiles.value = d?.designFiles || []
  } catch { detailData.value = null }
  finally { loading.value = false }
}

async function submitProcess() {
  if (!processForm.designerId) { alert('请选择设计师'); return }
  const designer = designerList.value.find((d: any) => d.id === processForm.designerId)
  await orderApi.update(detailData.value.id, {
    status: 2,
    designerId: processForm.designerId,
    designerName: designer?.realName || designer?.username || '',
    deliveryDate: processForm.deliveryDate || null,
  })
  processVisible.value = false; loadDetail()
}

async function confirmDelivery() {
  if (!confirm('确认订单已完成交付？')) return
  await orderApi.updateStatus(detailData.value.id, 3)
  loadDetail()
}

async function submitPayment() {
  if (!paymentForm.amount || paymentForm.amount <= 0) { alert('请输入有效金额'); return }
  if (paymentForm.writeOff && paymentForm.amount > remainingAmount.value) { alert('收款金额不能超过待收金额'); return }
  await orderApi.addPayment(detailData.value.id, paymentForm)
  paymentVisible.value = false; paymentForm.amount = 0; paymentForm.writeOff = false; loadDetail()
}

function formatMoney(v: any) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 打开收款弹窗
function openPayment() {
  paymentForm.amount = unpaidAmount.value
  paymentVisible.value = true
}
function formatTime(t: any) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

// 打开处理订单弹窗时加载设计师列表
watch(processVisible, async (v) => {
  if (v) {
    // 加载设计师列表
    if (!designerList.value.length) {
      try {
        const res = await designerApi.getCommissionList()
        designerList.value = Array.isArray(res.data) ? res.data : []
      } catch { /* ignore */ }
    }
    // 默认选中当前订单的设计师
    const currDesignerId = detailData.value?.designerId
    if (currDesignerId && designerList.value.some((d: any) => d.id === currDesignerId)) {
      processForm.designerId = currDesignerId
    }
  }
})

onMounted(() => {
  // 数据加载完成后，如果自动弹出收款，这里通过 setTimeout 确保 DOM 更新
  loadDetail().then(() => {
    if (route.query.focus === 'payment' && unpaidAmount.value > 0) {
      setTimeout(() => openPayment(), 300)
    }
  })
})
</script>

<style scoped lang="scss">
.page-container { padding: 20px 24px; }

.detail-header {
  display: flex; align-items: center; justify-content: space-between;
}
.dh-left { display: flex; align-items: center; gap: 8px; }
.dh-no { font-size: 18px; font-weight: 700; }

/* 未收款状态标签 */
.status-unpaid-warn {
  background: #fef0f0 !important;
  color: #f56c6c !important;
  border: 1px solid #fbc4c4 !important;
  font-weight: 600;
  animation: pulse-warn 2s ease-in-out infinite;
}
@keyframes pulse-warn {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(245, 108, 108, 0); }
}

/* 未收款横幅 */
.payment-warn-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef0f0, #fff5f5);
  border: 1px solid #fbc4c4;
  border-left: 4px solid #f56c6c;
  border-radius: 8px;
  margin-bottom: 16px;
}
.pwb-icon { font-size: 20px; }
.pwb-text {
  flex: 1;
  font-size: 13px;
  color: #f56c6c;
  strong { font-size: 16px; }
}
.pwb-btn {
  padding: 6px 16px;
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  &:hover {
    background: #e04040;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
  }
}
.dh-actions { display: flex; gap: 10px; align-items: center; }

/* 交付按钮替代提示 */
.delivery-hint {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 12px; border-radius: 6px;
  background: #fef0f0; color: #f56c6c;
  font-size: 12px; font-weight: 500;
  border: 1px solid #fbc4c4;
  cursor: not-allowed;
}

.detail-tabs {
  display: flex; gap: 0; border-bottom: 1px solid #f0f0f0;
  margin: 0 20px; padding: 0;
}
.detail-tab {
  padding: 14px 20px; cursor: pointer; font-size: 13px; font-weight: 500;
  border-bottom: 2px solid transparent; color: #909399; transition: all 0.2s;
  display: flex; align-items: center; gap: 6px;
  &:hover { color: #606266; }
  &.active { color: #409eff; border-bottom-color: #409eff; }
}
.tab-badge {
  background: #409eff; color: #fff; font-size: 11px; padding: 1px 6px;
  border-radius: 10px; font-weight: 600; line-height: 1.4;
}
.tab-content { padding: 20px; }

.status-flow {
  display: flex; align-items: center; gap: 0; flex-wrap: wrap;
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
  align-self: flex-start; margin-top: 18px;
}

.info-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}
.info-cell { background: #f5f7fa; padding: 12px; border-radius: 8px; }
.info-label { font-size: 11px; color: #909399; margin-bottom: 4px; }
.info-value { font-size: 14px; font-weight: 600; }

.finance-summary {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;
}
.finance-card {
  background: #f5f7fa; border-radius: 8px; padding: 14px; text-align: center;
  border-top: 3px solid;
}
.finance-label { font-size: 11px; margin-bottom: 6px; }
.finance-value { font-size: 18px; font-weight: 700; }

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td {
  padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0f0f0;
}
.data-table th { background: #fafbfc; color: #606266; font-weight: 600; font-size: 12px; }
.cost-cell {
  cursor: pointer; padding: 4px 8px; border-radius: 4px; transition: all 0.2s;
  &:hover { background: #ecf5ff; color: #409eff; }
  &.cost-zero { color: #c0c4cc; }
}
.cost-input {
  border: 1px solid #dcdfe6; border-radius: 4px; padding: 3px 6px; font-size: 13px;
  &:focus { outline: none; border-color: #409eff; }
}
.btn-icon {
  border: none; background: none; cursor: pointer; padding: 2px 4px; border-radius: 4px; font-size: 13px;
  &.btn-confirm { color: #67c23a; &:hover { background: #f0f9eb; } }
  &.btn-cancel { color: #f56c6c; &:hover { background: #fef0f0; } }
}

.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-container {
  background: #fff; border-radius: 12px; width: 90%;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  max-height: 85vh; overflow-y: auto;
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
.modal-body { padding: 20px 24px; }
.modal-footer {
  padding: 14px 24px; border-top: 1px solid #f0f0f0;
  display: flex; justify-content: flex-end; gap: 10px;
}

/* 开关样式 */
.toggle-switch {
  position: relative; display: inline-block; width: 36px; height: 20px; cursor: pointer;
}
.toggle-switch input { display: none; }
.toggle-slider {
  position: absolute; inset: 0; background: #dcdfe6; border-radius: 10px; transition: 0.3s;
}
.toggle-slider::before {
  content: ''; position: absolute; height: 16px; width: 16px; left: 2px; bottom: 2px;
  background: #fff; border-radius: 50%; transition: 0.3s;
}
.toggle-switch input:checked + .toggle-slider { background: #409eff; }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(16px); }

// ===== 设计文件样式 =====
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.file-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.25s ease;
  cursor: pointer;
  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 16px rgba(64, 158, 255, 0.12);
    transform: translateY(-2px);
  }
}

.file-thumb {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  position: relative;
  overflow: hidden;

  // 扩展名颜色主题
  &.ext-corel { background: linear-gradient(135deg, #1a8c2a 0%, #2dd44a 100%); }
  &.ext-illustrator { background: linear-gradient(135deg, #ff9a00 0%, #ff6f00 100%); }
  &.ext-photoshop { background: linear-gradient(135deg, #31a8ff 0%, #0078d4 100%); }
  &.ext-pdf { background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); }
  &.ext-indesign { background: linear-gradient(135deg, #ff3366 0%, #cc0033 100%); }
  &.ext-eps { background: linear-gradient(135deg, #8e44ad 0%, #6c3483 100%); }
  &.ext-image { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
  &.ext-archive { background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%); }
  &.ext-sketch { background: linear-gradient(135deg, #fd79a8 0%, #e84393 100%); }
  &.ext-word { background: linear-gradient(135deg, #2b579a 0%, #1e3c72 100%); }
  &.ext-excel { background: linear-gradient(135deg, #217346 0%, #185a34 100%); }
}

.file-thumb-img {
  width: 100%; height: 100%; object-fit: cover;
}

.file-thumb-icon {
  color: #909399; font-size: 13px; text-align: center;
}

.file-thumb-icon-wrap {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.file-ext-label {
  font-size: 28px; font-weight: 800; color: rgba(255,255,255,0.95);
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  letter-spacing: 1px;
}
.file-app-label {
  font-size: 11px; color: rgba(255,255,255,0.8);
  background: rgba(0,0,0,0.15); padding: 2px 8px; border-radius: 4px;
}

.file-info {
  padding: 10px 12px;
}
.file-name {
  font-size: 13px; font-weight: 600; color: #303133;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 4px;
}
.file-meta {
  display: flex; gap: 8px; align-items: center; font-size: 11px; color: #909399;
}
.file-version {
  background: #ecf5ff; color: #409eff; padding: 1px 5px; border-radius: 3px; font-weight: 600;
}
.file-status {
  &.file-status-1 { color: #909399; }
  &.file-status-2 { color: #67c23a; }
  &.file-status-3 { color: #f56c6c; }
}

.file-actions {
  display: flex; gap: 2px; justify-content: flex-end;
  padding: 4px 8px; border-top: 1px solid #f5f5f5;
  .btn-icon { font-size: 15px; padding: 2px 6px; }
}

.file-empty {
  text-align: center; padding: 48px 20px;
}

// 上传区域
.upload-zone {
  border: 2px dashed #dcdfe6; border-radius: 10px; padding: 32px;
  text-align: center; transition: all 0.25s;
  &.upload-dragover {
    border-color: #409eff; background: #ecf5ff;
  }
}
.upload-file-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 12px;
  background: #f5f7fa; border-radius: 6px; margin-bottom: 6px;
}
.upload-file-ext {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 24px; border-radius: 4px; font-size: 10px; font-weight: 800; color: #fff;
  &.ext-corel { background: #1a8c2a; }
  &.ext-illustrator { background: #ff9a00; }
  &.ext-photoshop { background: #31a8ff; }
  &.ext-pdf { background: #e74c3c; }
  &.ext-image { background: #667eea; }
  &.ext-archive { background: #f39c12; }
}
.upload-file-name {
  flex: 1; font-size: 13px; color: #303133;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.upload-file-size {
  font-size: 12px; color: #909399; flex-shrink: 0;
}

// 预览
.preview-image-wrap {
  text-align: center;
}
.preview-file-detail {
  text-align: center;
}
.preview-file-icon-big {
  display: inline-flex; align-items: center; justify-content: center;
  width: 100px; height: 100px; border-radius: 16px;
  font-size: 32px; font-weight: 800; color: rgba(255,255,255,0.95);
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
  &.ext-corel { background: linear-gradient(135deg, #1a8c2a, #2dd44a); }
  &.ext-illustrator { background: linear-gradient(135deg, #ff9a00, #ff6f00); }
  &.ext-photoshop { background: linear-gradient(135deg, #31a8ff, #0078d4); }
  &.ext-pdf { background: linear-gradient(135deg, #e74c3c, #c0392b); }
  &.ext-indesign { background: linear-gradient(135deg, #ff3366, #cc0033); }
  &.ext-eps { background: linear-gradient(135deg, #8e44ad, #6c3483); }
  &.ext-image { background: linear-gradient(135deg, #667eea, #764ba2); }
  &.ext-archive { background: linear-gradient(135deg, #f39c12, #e67e22); }
}
</style>
