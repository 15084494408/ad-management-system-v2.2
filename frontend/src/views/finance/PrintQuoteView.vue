<template>
  <div class="page-container">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="router.push('/dashboard')">首页</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item" @click="router.push('/finance/quote')">报价管理</span>
      <span class="breadcrumb-separator">/</span>
      <span class="breadcrumb-item active">印刷报价计算器</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🖨️ 印刷报价计算器</h1>
      <div class="page-actions">
        <button class="btn btn-default" @click="router.push('/finance/quote')">← 返回</button>
        <button class="btn btn-outline-primary" @click="copyResult">📋 复制报价结果</button>
        <button class="btn btn-danger" @click="resetCurrent">🔄 重置</button>
      </div>
    </div>

    <!-- ========== 模式切换 Tabs ========== -->
    <div class="mode-tabs">
      <div class="mode-tab" :class="{ active: activeMode === 'label' }" @click="activeMode = 'label'">
        <span class="tab-icon">🏷️</span>
        <span>标签印刷</span>
        <span class="tab-desc">简单报价 / 智能拼版</span>
      </div>
      <div class="mode-tab" :class="{ active: activeMode === 'print' }" @click="activeMode = 'print'">
        <span class="tab-icon">📄</span>
        <span>打印报价</span>
        <span class="tab-desc">黑白 / 彩色 份数计价</span>
      </div>
    </div>

    <!-- ================================================================== -->
    <!--                     模式一：标签印刷                                -->
    <!-- ================================================================== -->
    <template v-if="activeMode === 'label'">
      <!-- 子模式切换 -->
      <div class="sub-mode-bar">
        <div class="sub-toggle">
          <button class="sub-btn" :class="{ active: labelSubMode === 'direct' }" @click="labelSubMode = 'direct'">
            📐 直接输入
          </button>
          <button class="sub-btn" :class="{ active: labelSubMode === 'imposition' }" @click="labelSubMode = 'imposition'">
            🧩 智能拼版
          </button>
        </div>
        <span class="sub-hint">
          {{ labelSubMode === 'direct' ? '已知纸张数量，直接输入计算' : '输入标签尺寸和需求，自动计算拼版和用纸' }}
        </span>
      </div>

      <div class="grid-2col">
        <!-- 左列：参数输入 -->
        <div class="col-left">
          <!-- 纸张选择 -->
          <div class="card">
            <div class="card-title-bar"><span>📄 纸张选择</span></div>
            <div class="form-row">
              <div class="form-group" style="flex:2;">
                <label>纸张类型</label>
                <select v-model="label.paperType" class="form-control" @change="onPaperTypeChange">
                  <option value="">请选择</option>
                  <option v-for="p in paperTypes" :key="p.name" :value="p.name">{{ p.name }} ({{ p.price }}元/㎡)</option>
                </select>
              </div>
              <div class="form-group" style="flex:1;">
                <label>纸张单价 (元/㎡)</label>
                <input type="number" v-model.number="label.paperPrice" class="form-control" min="0" step="0.01" />
              </div>
            </div>
          </div>

          <!-- 标签参数 -->
          <div class="card">
            <div class="card-title-bar"><span>🏷️ 标签参数</span></div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>标签长度 (mm)</label>
                <input type="number" v-model.number="label.length" class="form-control" min="0" step="0.1" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>标签宽度 (mm)</label>
                <input type="number" v-model.number="label.width" class="form-control" min="0" step="0.1" />
              </div>
            </div>

            <!-- 拼版参数（仅拼版模式） -->
            <template v-if="labelSubMode === 'imposition'">
              <div class="form-row">
                <div class="form-group" style="flex:1;">
                  <label>印刷纸张规格</label>
                  <select v-model="label.paperSizeKey" class="form-control" @change="onPaperSizeChange">
                    <option v-for="(v, k) in PAPER_SIZES" :key="k" :value="k">{{ k }}</option>
                  </select>
                </div>
              </div>
              <div class="form-row" v-if="label.paperSizeKey === '自定义'">
                <div class="form-group" style="flex:1;">
                  <label>自定义宽 (mm)</label>
                  <input type="number" v-model.number="label.customPaperW" class="form-control" min="1" />
                </div>
                <div class="form-group" style="flex:1;">
                  <label>自定义高 (mm)</label>
                  <input type="number" v-model.number="label.customPaperH" class="form-control" min="1" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-group" style="flex:1;">
                  <label>标签间距 (mm)</label>
                  <input type="number" v-model.number="label.gap" class="form-control" min="0" step="0.1" />
                </div>
                <div class="form-group" style="flex:1;">
                  <label>页边距 (mm)</label>
                  <input type="number" v-model.number="label.margin" class="form-control" min="0" step="0.1" />
                </div>
                <div class="form-group" style="flex:1;">
                  <label>角线边距 (mm)</label>
                  <input type="number" v-model.number="label.corner" class="form-control" min="0" step="0.1" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-group" style="flex:1;">
                  <label>需求数量 (个)</label>
                  <input type="number" v-model.number="label.demand" class="form-control" min="1" />
                </div>
              </div>
            </template>

            <!-- 直接输入模式 -->
            <template v-else>
              <div class="form-row">
                <div class="form-group" style="flex:1;">
                  <label>纸张数量 (张)</label>
                  <input type="number" v-model.number="label.sheets" class="form-control" min="0" />
                </div>
                <div class="form-group" style="flex:1;">
                  <label>标签总个数</label>
                  <input type="number" v-model.number="label.totalLabels" class="form-control" min="0" />
                </div>
              </div>
            </template>
          </div>

          <!-- 数量 & 费用 -->
          <div class="card">
            <div class="card-title-bar"><span>💰 费用参数</span></div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>损耗张数 (张)</label>
                <input type="number" v-model.number="label.waste" class="form-control" min="0" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>印刷费 (元)</label>
                <input type="number" v-model.number="label.printFee" class="form-control" min="0" step="0.01" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>划线费 (元)</label>
                <input type="number" v-model.number="label.lineFee" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>压痕费 (元)</label>
                <input type="number" v-model.number="label.scoreFee" class="form-control" min="0" step="0.01" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>覆膜单价 (元/张)</label>
                <input type="number" v-model.number="label.laminate" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>报价比例</label>
                <input type="number" v-model.number="label.ratio" class="form-control" min="0.01" step="0.01" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>装订费 (元)</label>
                <input type="number" v-model.number="label.bindingFee" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>份数</label>
                <input type="number" v-model.number="label.copies" class="form-control" min="1" />
              </div>
            </div>
          </div>

          <!-- 计算按钮 -->
          <button class="btn btn-primary btn-calc" @click="calcLabel">
            💰 {{ labelSubMode === 'imposition' ? '计算拼版并报价' : '计算报价' }}
          </button>
        </div>

        <!-- 右列：结果 & 拼版预览 -->
        <div class="col-right">
          <!-- 拼版预览 (仅拼版模式) -->
          <div v-if="labelSubMode === 'imposition'" class="card preview-card">
            <div class="card-title-bar"><span>🖼️ 拼版预览</span></div>
            <div class="preview-canvas-wrap">
              <svg v-if="layoutResult" class="preview-svg" :viewBox="`0 0 340 ${layoutResult.paperH / layoutResult.paperW * 340}`">
                <!-- 纸张 -->
                <rect x="2" y="2" :width="layoutResult.paperW * scaleSvg" :height="layoutResult.paperH * scaleSvg"
                  fill="#fff" stroke="#333" stroke-width="1.5" />
                <!-- 页边距 -->
                <rect :x="(layoutResult.margin + 2) * scaleSvg" :y="(layoutResult.margin + 2) * scaleSvg"
                  :width="(layoutResult.paperW - layoutResult.margin * 2) * scaleSvg"
                  :height="(layoutResult.paperH - layoutResult.margin * 2) * scaleSvg"
                  fill="none" stroke="#409eff" stroke-width="0.8" stroke-dasharray="3,3" />
                <!-- 角线区域 -->
                <rect :x="layoutResult.margin + layoutResult.corner + 2 * scaleSvg"
                  :y="layoutResult.margin + layoutResult.corner + 2 * scaleSvg"
                  :width="layoutResult.totalW * scaleSvg" :height="layoutResult.totalH * scaleSvg"
                  fill="#e8ffe8" stroke="#67c23a" stroke-width="0.6" />
                <!-- 标签格子 -->
                <template v-for="row in Math.min(layoutResult.rows, 20)" :key="'r' + row">
                  <template v-for="col in Math.min(layoutResult.cols, 20)" :key="'c' + col">
                    <rect
                      :x="(layoutResult.margin + layoutResult.corner + col * (layoutResult.labelW + layoutResult.gap)) * scaleSvg + 2"
                      :y="(layoutResult.margin + layoutResult.corner + row * (layoutResult.labelH + layoutResult.gap)) * scaleSvg + 2"
                      :width="layoutResult.labelW * scaleSvg" :height="layoutResult.labelH * scaleSvg"
                      fill="#e8f4fd" stroke="#e6a23c" stroke-width="0.5" />
                  </template>
                </template>
              </svg>
              <div v-else class="preview-empty">点击「计算拼版」查看预览</div>
            </div>
          </div>

          <!-- 报价结果 -->
          <div class="card result-card">
            <div class="card-title-bar">
              <span>📊 报价结果</span>
              <span v-if="labelResult" class="result-total">¥{{ formatMoney(labelResult.total) }}</span>
            </div>
            <div v-if="labelResult" class="result-body">
              <div class="result-section">
                <div class="result-row"><span>实际尺寸 (+10mm)</span><span>{{ labelResult.lengthActual }} × {{ labelResult.widthActual }} mm</span></div>
                <div class="result-row"><span>单张面积</span><span>{{ labelResult.area }} ㎡</span></div>
                <div v-if="labelSubMode === 'imposition'" class="result-row highlight">
                  <span>拼版布局</span><span>{{ layoutResult?.cols }}列 × {{ layoutResult?.rows }}行 = {{ layoutResult?.perSheet }}个/张</span>
                </div>
                <div class="result-row"><span>基础用纸 / 损耗</span><span>{{ labelResult.baseSheets }} + {{ labelResult.waste }} = {{ labelResult.actualSheets }} 张</span></div>
                <div v-if="labelSubMode === 'direct'" class="result-row"><span>标签总数</span><span>{{ label.totalLabels }} 个</span></div>
                <div v-else class="result-row"><span>需求 / 实际产出</span><span>{{ label.demand }} / {{ labelResult.actualSheets * (layoutResult?.perSheet || 0) }} 个</span></div>
              </div>
              <div class="result-divider"></div>
              <div class="result-section">
                <div class="result-row"><span>纸张成本</span><span>¥{{ formatMoney(labelResult.paperCost) }}</span></div>
                <div class="result-row"><span>印刷费</span><span>¥{{ formatMoney(labelResult.printFee) }}</span></div>
                <div class="result-row"><span>划线 + 压痕</span><span>¥{{ formatMoney(labelResult.lineScore) }}</span></div>
                <div class="result-row"><span>覆膜费用</span><span>¥{{ formatMoney(labelResult.laminateTotal) }}</span></div>
                <div class="result-row"><span>装订费</span><span>¥{{ formatMoney(labelResult.bindingFee) }}</span></div>
              </div>
              <div class="result-divider"></div>
              <div class="result-section">
                <div class="result-row"><span>小计</span><span>¥{{ formatMoney(labelResult.subtotal) }}</span></div>
                <div class="result-row"><span>报价比例</span><span>{{ label.ratio }} 倍</span></div>
                <div v-if="label.copies > 1" class="result-row"><span>份数</span><span>{{ label.copies }} 份</span></div>
                <div class="result-row"><span>× 报价比例(×份数)</span><span>¥{{ formatMoney(labelResult.total) }}</span></div>
              </div>
              <div class="result-divider"></div>
              <div class="result-grand">
                <span>总报价</span>
                <strong>¥{{ formatMoney(labelResult.total) }}</strong>
              </div>
              <div class="result-unit">
                单{{ labelSubMode === 'imposition' ? '个标签' : '个' }}价格：
                <strong>¥{{ formatMoney(labelResult.unitPrice) }}</strong>
                /{{ labelSubMode === 'imposition' ? '个' : '个' }}
              </div>
            </div>
            <div v-else class="result-empty">请填写参数后点击计算</div>
          </div>
        </div>
      </div>
    </template>

    <!-- ================================================================== -->
    <!--                     模式二：打印报价                                -->
    <!-- ================================================================== -->
    <template v-if="activeMode === 'print'">
      <div class="grid-2col">
        <!-- 左列：参数输入 -->
        <div class="col-left">
          <!-- 全局设置 -->
          <div class="card">
            <div class="card-title-bar"><span>⚙️ 全局设置</span></div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>满版彩色阈值</label>
                <select v-model="print.colorThreshold" class="form-control">
                  <option v-for="t in [50,60,70,80,90]" :key="t" :value="t">{{ t }}%</option>
                </select>
              </div>
              <div class="form-group" style="flex:1;">
                <label>黑白单价 (元/张)</label>
                <input type="number" v-model.number="print.blackPrice" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>彩色单价 (元/张)</label>
                <input type="number" v-model.number="print.colorPrice" class="form-control" min="0" step="0.01" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" style="flex:1;">
                <label>满版彩色加价 (元/张)</label>
                <input type="number" v-model.number="print.fullColorExtra" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>装订费 (元)</label>
                <input type="number" v-model.number="print.bindingFee" class="form-control" min="0" step="0.01" />
              </div>
              <div class="form-group" style="flex:1;">
                <label>其他费用 (元)</label>
                <input type="number" v-model.number="print.otherFee" class="form-control" min="0" step="0.01" />
              </div>
            </div>
          </div>

          <!-- 价格梯度参考 -->
          <div class="card">
            <div class="card-title-bar"><span>📊 价格梯度参考</span></div>
            <div class="tier-grid">
              <div class="tier-section">
                <div class="tier-title">黑白价格梯度</div>
                <div class="tier-row" v-for="t in BLACK_PRICE_TIERS" :key="'bw' + t[0]">
                  <span>{{ t[0] }}-{{ t[1] === Infinity ? '∞' : t[1] }}张</span>
                  <strong>{{ t[2] }}元/张</strong>
                </div>
              </div>
              <div class="tier-section">
                <div class="tier-title">彩色价格梯度</div>
                <div class="tier-row" v-for="t in COLOR_PRICE_TIERS" :key="'cl' + t[0]">
                  <span>{{ t[0] }}-{{ t[1] === Infinity ? '∞' : t[1] }}张</span>
                  <strong>{{ t[2] }}元/张</strong>
                </div>
              </div>
            </div>
          </div>

          <!-- 隐藏的PDF文件选择器 -->
          <input type="file" ref="pdfFileInputRef" accept=".pdf" multiple hidden @change="onPdfFilesSelected" />

          <!-- 文件列表 -->
          <div class="card">
            <div class="card-title-bar">
              <span>📄 文件列表</span>
              <div style="display:flex;gap:8px;flex-wrap:wrap;">
                <button class="btn btn-sm btn-primary-outline" @click="triggerPdfUpload">+ 上传PDF文件</button>
                <button class="btn btn-sm btn-default" @click="addManualFile">+ 手动添加</button>
                <button v-if="printFiles.length > 0 && hasUnanalyzedFiles" class="btn btn-sm btn-custom-add"
                  @click="analyzeAllFiles" :disabled="isAnalyzing">
                  🔍 {{ isAnalyzing ? '分析中...' : '一键识别全部' }}
                </button>
                <button class="btn btn-sm btn-danger-outline" @click="clearPrintFiles">清空</button>
              </div>
            </div>
            <div v-if="printFiles.length === 0" class="files-empty">
              <div style="font-size:28px;margin-bottom:8px;">📁</div>
              点击「上传PDF文件」选择PDF，系统自动识别黑白/彩色页
            </div>
            <div v-for="(f, idx) in printFiles" :key="f.id" class="file-item" :class="{ 'analyzing-item': f.analyzing }">
              <div class="file-header">
                <span class="file-name">
                  <span v-if="f.analyzing" class="status-dot analyzing-dot"></span>
                  <span v-else-if="f.analyzed" class="status-dot analyzed-dot"></span>
                  <span v-else class="status-dot">📄</span>
                  {{ f.name || `文件${idx + 1}` }}
                  <span v-if="f.totalPages > 0" class="file-pages-badge">{{ f.totalPages }}页</span>
                </span>
                <div style="display:flex;gap:6px;align-items:center;">
                  <span v-if="f.analyzing" class="analyzing-text">{{ f.progressText || '分析中...' }}</span>
                  <span v-if="f.analyzed && !f.analyzing" class="analyzed-badge">
                    黑{{ f.blackPages }} / 彩{{ f.colorPages }} / 满版{{ f.fullColorPages }}
                  </span>
                  <button v-if="f.hasFile && !f.analyzing" class="btn-sm btn-primary-outline" style="font-size:11px;"
                    @click="analyzeSingleFile(idx)">🔍 重新识别</button>
                  <button class="delete-sm" @click="printFiles.splice(idx, 1); printResult = null">✕</button>
                </div>
              </div>
              <!-- 分析进度条 -->
              <div v-if="f.analyzing" class="analysis-progress-wrap">
                <div class="analysis-progress-bar" :style="{ width: f.progress + '%' }"></div>
              </div>
              <!-- 可编辑字段 -->
              <div class="file-fields">
                <div class="field-item">
                  <label>总页数</label>
                  <input type="number" v-model.number="f.totalPages" class="field-input" min="0" />
                </div>
                <div class="field-item">
                  <label>黑白页</label>
                  <input type="number" v-model.number="f.blackPages" class="field-input" min="0" />
                </div>
                <div class="field-item">
                  <label>彩色页</label>
                  <input type="number" v-model.number="f.colorPages" class="field-input" min="0" />
                </div>
                <div class="field-item">
                  <label>满版彩色</label>
                  <input type="number" v-model.number="f.fullColorPages" class="field-input" min="0" />
                </div>
                <div class="field-item">
                  <label>黑白份数</label>
                  <input type="number" v-model.number="f.bwCopies" class="field-input" min="0" />
                </div>
                <div class="field-item">
                  <label>彩色份数</label>
                  <input type="number" v-model.number="f.colorCopies" class="field-input" min="0" />
                </div>
              </div>
              <div class="file-hint">
                <template v-if="f.analyzing">⏳ 正在逐页分析色彩，请稍候...</template>
                <template v-else-if="f.analyzed">✅ 自动识别完成，数值可手动调整</template>
                <template v-else-if="f.hasFile">⏸ 等待识别（或手动填写页数）</template>
                <template v-else>黑白份数=全部页按黑白计费；彩色份数=彩色页彩印，黑白页黑白印</template>
              </div>
            </div>
          </div>

          <!-- 计算按钮 -->
          <button class="btn btn-primary btn-calc btn-calc-print" @click="calcPrint" :disabled="printFiles.length === 0">
            💰 计算打印报价
          </button>
          <div v-if="printFiles.length === 0" class="calc-hint">请先添加PDF文件后计算</div>
        </div>

        <!-- 右列：报价结果 -->
        <div class="col-right">
          <div class="card result-card">
            <div class="card-title-bar">
              <span>📊 打印报价结果</span>
              <span v-if="printResult" class="result-total">¥{{ formatMoney(printResult.total) }}</span>
            </div>
            <div v-if="printResult" class="result-body">
              <!-- 文件明细 -->
              <div class="result-section">
                <div class="result-section-title">文件明细</div>
                <div v-for="fd in printResult.fileDetails" :key="fd.name" class="file-detail-block">
                  <div class="fdb-name">{{ fd.name }}</div>
                  <div class="fdb-info">
                    总{{ fd.totalPages }}页 (黑白{{ fd.black }} / 彩色{{ fd.color }} / 满版{{ fd.fullColor }})
                  </div>
                  <div class="fdb-row">
                    <span>黑白印{{ fd.bwCopies }}份：</span>
                    <span>全部{{ fd.totalPages }}页×{{ fd.bwCopies }} = <strong>{{ fd.bwFromBlack }}</strong>张</span>
                  </div>
                  <div class="fdb-row">
                    <span>彩色印{{ fd.colorCopies }}份：</span>
                    <span>彩{{ fd.color }}×{{ fd.colorCopies }} = <strong>{{ fd.colorFromColor }}</strong>张彩 + 黑{{ fd.black }}×{{ fd.colorCopies }} = <strong>{{ fd.bwFromColor }}</strong>张黑</span>
                  </div>
                </div>
              </div>
              <div class="result-divider"></div>
              <div class="result-section">
                <div class="result-section-title">费用汇总</div>
                <div class="result-row"><span>黑白印刷</span><span>{{ printResult.totalBlack }}张 × {{ printResult.blackPrice }}元 = ¥{{ formatMoney(printResult.blackTotal) }}</span></div>
                <div class="result-row"><span>普通彩色</span><span>{{ printResult.totalNormalColor }}张 × {{ printResult.colorPrice }}元 = ¥{{ formatMoney(printResult.normalColorTotal) }}</span></div>
                <div class="result-row"><span>满版彩色</span><span>{{ printResult.totalFullColor }}张 × {{ printResult.fullColorPrice }}元 = ¥{{ formatMoney(printResult.fullColorTotal) }}</span></div>
                <div class="result-row"><span>装订费</span><span>¥{{ formatMoney(printResult.bindingFee) }}</span></div>
                <div class="result-row"><span>其他费用</span><span>¥{{ formatMoney(printResult.otherFee) }}</span></div>
              </div>
              <div class="result-divider"></div>
              <div class="result-grand">
                <span>总报价</span>
                <strong>¥{{ formatMoney(printResult.total) }}</strong>
              </div>
              <div class="result-unit">
                总份数：{{ printResult.totalCopies }}份 · 每份均价：<strong>¥{{ formatMoney(printResult.perCopy) }}</strong>/份
              </div>
            </div>
            <div v-else class="result-empty">请添加文件并填写参数后点击计算</div>
          </div>
        </div>
      </div>
    </template>

    <!-- ================================================================== -->
    <!--                     物料选择区域（共用）                            -->
    <!-- ================================================================== -->
    <div class="card material-card">
      <div class="card-title-bar">
        <span>📦 附加物料</span>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
          <input type="text" v-model="matKeyword" class="form-control" placeholder="搜索物料..." style="width:160px;padding:4px 8px;" @input="filterMats" />
          <select v-model="matCatFilter" class="form-control" style="width:120px;padding:4px 8px;" @change="filterMats">
            <option value="">全部分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <button class="btn btn-sm btn-custom-add" @click="addCustomMat">✏️ 手动添加</button>
          <span style="color:#909399;font-size:12px;">已选 {{ selectedMats.length }} 件 | 合计 ¥{{ formatMoney(matTotal) }}</span>
        </div>
      </div>
      <div class="mat-grid" v-if="!matsLoading">
        <div v-if="filteredMats.length > 0" class="mat-list">
          <div v-for="m in filteredMats" :key="m.id" class="mat-item" :class="{ selected: isMatSelected(m.id) }">
            <div class="mat-name">{{ m.name }}</div>
            <div class="mat-info">
              <span class="tag">{{ m.categoryName }}</span>
              <span class="mat-price">¥{{ formatMoney(m.price) }}</span>
            </div>
            <div v-if="isMatSelected(m.id)" class="mat-actions">
              <input type="number" v-model.number="getMat(m.id).quantity" class="mat-qty" min="1" style="width:50px;" @change="recalcMat(m.id)" />
              <input type="number" v-model.number="getMat(m.id).unitPrice" class="mat-price-input" min="0" step="0.01" style="width:60px;" @change="recalcMat(m.id)" />
              <button class="delete-sm" @click="removeMat(m.id)">✕</button>
            </div>
            <button v-else class="btn-sm btn-primary-outline" @click="addMat(m)">+ 选用</button>
          </div>
        </div>
        <div v-else class="materials-empty">暂无匹配物料</div>
      </div>
      <div v-else class="loading-placeholder"><p>加载物料中...</p></div>
    </div>

    <!-- 已选物料清单 -->
    <div v-if="selectedMats.length > 0" class="card">
      <div class="card-title-bar"><span>🛒 已选附加物料</span></div>
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:38px;text-align:center;">#</th>
            <th style="width:28%;">物料名称</th>
            <th style="width:14%;">规格</th>
            <th style="width:50px;text-align:center;">单位</th>
            <th style="width:60px;text-align:center;">数量</th>
            <th style="width:80px;text-align:right;">单价</th>
            <th style="width:80px;text-align:right;">小计</th>
            <th style="width:60px;text-align:center;">备注</th>
            <th style="width:50px;text-align:center;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, idx) in selectedMats" :key="s.materialId || idx" :class="{ 'custom-row': s.isCustom }">
            <td style="text-align:center;color:#909399;">{{ idx + 1 }}</td>
            <td>
              <input v-if="s.isCustom" type="text" v-model="s.materialName" class="inline-edit-input" style="width:100%;" placeholder="物料名称" />
              <strong v-else>{{ s.materialName }}</strong>
            </td>
            <td>
              <input v-if="s.isCustom" type="text" v-model="s.spec" class="inline-edit-input" style="width:100%;" placeholder="规格" />
              <span v-else style="font-size:12px;">{{ s.spec || '-' }}</span>
            </td>
            <td style="text-align:center;">{{ s.unit || '个' }}</td>
            <td style="text-align:center;"><input type="number" v-model.number="s.quantity" class="qty-inline" min="0.01" @change="recalcSelMat(idx)" /></td>
            <td style="text-align:right;"><input type="number" v-model.number="s.unitPrice" class="price-inline" min="0" step="0.01" @change="recalcSelMat(idx)" /></td>
            <td style="text-align:right;font-weight:600;color:#e6a23c;">¥{{ formatMoney(s.amount) }}</td>
            <td style="text-align:center;"><input type="text" v-model="s.quoteRemark" class="remark-inline" placeholder="-" /></td>
            <td style="text-align:center;"><button class="delete-sm" @click="selectedMats.splice(idx, 1)">✕</button></td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="total-row"><td colspan="6" style="text-align:right;font-weight:600;">物料合计：</td><td colspan="3" style="text-align:right;font-weight:700;color:#e6a23c;font-size:15px;">¥{{ formatMoney(matTotal) }}</td></tr>
        </tfoot>
      </table>
    </div>

    <!-- 综合汇总 -->
    <div v-if="showGrandSummary" class="card grand-summary-card">
      <div class="grand-summary-row">
        <div class="gs-item" v-if="labelResult">
          <span>标签印刷报价</span><strong>¥{{ formatMoney(labelResult.total) }}</strong>
        </div>
        <div class="gs-item" v-if="printResult">
          <span>打印报价</span><strong>¥{{ formatMoney(printResult.total) }}</strong>
        </div>
        <div class="gs-item" v-if="selectedMats.length > 0">
          <span>附加物料</span><strong>¥{{ formatMoney(matTotal) }}</strong>
        </div>
        <div class="gs-divider"></div>
        <div class="gs-item gs-final">
          <span>综合总计</span><strong>¥{{ formatMoney(grandTotalAmount) }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { materialApi } from '@/api/modules/material'

const router = useRouter()

// ==================== 纸张尺寸库 ====================
const PAPER_SIZES: Record<string, [number, number] | null> = {
  'A0 (841×1189)': [841, 1189],
  'A1 (594×841)': [594, 841],
  'A2 (420×594)': [420, 594],
  'A3 (297×420)': [297, 420],
  'A4 (210×297)': [210, 297],
  'A5 (148×210)': [148, 210],
  'SAR3 (340×480)': [340, 480],
  'SAR4 (240×340)': [240, 340],
  '320×450': [320, 450],
  'B4 (250×353)': [250, 353],
  'B5 (176×250)': [176, 250],
  '16开 (184×260)': [184, 260],
  '32开 (130×184)': [130, 184],
  '大度对开 (590×885)': [590, 885],
  '大度3开 (394×885)': [394, 885],
  '大度4开 (443×590)': [443, 590],
  '正度对开 (540×780)': [540, 780],
  '正度4开 (390×540)': [390, 540],
  '自定义': null,
}

// ==================== 价格梯度 ====================
const BLACK_PRICE_TIERS = [
  [0, 100, 0.5],
  [100, 500, 0.4],
  [500, 1000, 0.3],
  [1000, Infinity, 0.2],
]
const COLOR_PRICE_TIERS = [
  [0, 50, 1.0],
  [50, 200, 0.8],
  [200, 500, 0.7],
  [500, 1000, 0.6],
  [1000, Infinity, 0.5],
]

// ==================== 纸张类型 ====================
const paperTypes = ref<{ name: string; price: number }[]>([
  { name: '铜版纸-80g', price: 0.8 },
  { name: '铜版纸-105g', price: 1.0 },
  { name: '铜版纸-128g', price: 1.2 },
  { name: '牛皮纸-100g', price: 0.9 },
  { name: '哑粉纸-120g', price: 1.1 },
  { name: '特种纸-珠光', price: 2.5 },
  { name: '合成纸', price: 1.8 },
])

// ==================== 模式切换 ====================
const activeMode = ref<'label' | 'print'>('label')
const labelSubMode = ref<'direct' | 'imposition'>('imposition')

// ==================== 标签印刷参数 ====================
const label = reactive({
  paperType: '',
  paperPrice: 1.0,
  length: 100,
  width: 80,
  // 直接输入
  sheets: 1000,
  totalLabels: 1050,
  // 拼版
  paperSizeKey: '320×450',
  customPaperW: 320,
  customPaperH: 450,
  gap: 3,
  margin: 5,
  corner: 5,
  demand: 1000,
  // 通用
  waste: 50,
  printFee: 150,
  lineFee: 0,
  scoreFee: 0,
  laminate: 0,
  ratio: 1.3,
  bindingFee: 0,
  copies: 1,
})

// 拼版结果
const layoutResult = ref<any>(null)
const labelResult = ref<any>(null)

function onPaperTypeChange() {
  const pt = paperTypes.value.find(p => p.name === label.paperType)
  if (pt) label.paperPrice = pt.price
}

function onPaperSizeChange() {
  // 自定义模式时不需要额外处理
}

const scaleSvg = computed(() => {
  if (!layoutResult.value) return 1
  return 336 / (layoutResult.value.paperW || 320)
})

function calcLabel() {
  if (labelSubMode.value === 'direct') {
    calcLabelDirect()
  } else {
    calcLabelImposition()
  }
}

function calcLabelDirect() {
  const lengthActual = label.length + 10
  const widthActual = label.width + 10
  const area = (lengthActual * widthActual) / 1000000
  const actualSheets = label.sheets + label.waste
  const paperCost = area * actualSheets * label.paperPrice
  const laminateTotal = label.laminate * actualSheets
  const lineScore = label.lineFee + label.scoreFee
  const subtotal = paperCost + label.printFee + lineScore + laminateTotal + label.bindingFee
  const total = subtotal * label.ratio * label.copies
  const unitPrice = label.totalLabels > 0 ? total / label.totalLabels : 0

  labelResult.value = {
    lengthActual, widthActual, area,
    baseSheets: label.sheets, waste: label.waste, actualSheets,
    paperCost, printFee: label.printFee, lineScore, laminateTotal,
    bindingFee: label.bindingFee,
    subtotal, total, unitPrice,
  }
  layoutResult.value = null
}

function calcLabelImposition() {
  // 获取纸张尺寸
  const sizeVal = PAPER_SIZES[label.paperSizeKey]
  let paperW: number, paperH: number
  if (label.paperSizeKey === '自定义') {
    paperW = label.customPaperW
    paperH = label.customPaperH
  } else if (sizeVal) {
    [paperW, paperH] = sizeVal
  } else {
    ElMessage.error('请选择有效的纸张规格')
    return
  }

  const labelW = label.length
  const labelH = label.width
  const gap = label.gap
  const margin = label.margin
  const corner = label.corner

  const usableW = paperW - 2 * margin
  const usableH = paperH - 2 * margin

  let cols = Math.floor((usableW + gap) / (labelW + gap))
  let rows = Math.floor((usableH + gap) / (labelH + gap))

  if (cols <= 0 || rows <= 0) {
    ElMessage.error('标签尺寸过大，无法放入纸张中')
    return
  }

  let totalW = cols * labelW + (cols - 1) * gap
  let totalH = rows * labelH + (rows - 1) * gap

  if (totalW > usableW + 0.1) { cols--; totalW = cols * labelW + (cols - 1) * gap }
  if (totalH > usableH + 0.1) { rows--; totalH = rows * labelH + (rows - 1) * gap }

  const finalW = totalW + 2 * corner
  const finalH = totalH + 2 * corner

  if (finalW > usableW + 0.1 || finalH > usableH + 0.1) {
    ElMessage.error('加上角线后超出纸张范围')
    return
  }

  const perSheet = cols * rows
  const demand = label.demand
  const sheetsNeeded = Math.ceil(demand / perSheet)
  const area = (finalW * finalH) / 1000000

  // 保存拼版结果
  layoutResult.value = {
    labelW, labelH, gap, margin, corner,
    paperW, paperH, usableW, usableH,
    totalW, totalH, finalW, finalH,
    cols, rows, perSheet, demand, sheetsNeeded, area,
  }

  // 计算报价
  const actualSheets = sheetsNeeded + label.waste
  const paperCost = area * actualSheets * label.paperPrice
  const laminateTotal = label.laminate * actualSheets
  const lineScore = label.lineFee + label.scoreFee
  const subtotal = paperCost + label.printFee + lineScore + laminateTotal + label.bindingFee
  const total = subtotal * label.ratio * label.copies
  const unitPrice = demand > 0 ? total / demand : 0

  labelResult.value = {
    lengthActual: label.length + 10,
    widthActual: label.width + 10,
    area,
    baseSheets: sheetsNeeded,
    waste: label.waste,
    actualSheets,
    paperCost, printFee: label.printFee, lineScore, laminateTotal,
    bindingFee: label.bindingFee,
    subtotal, total, unitPrice,
  }
}

// ==================== PDF.js 动态加载 ====================
let pdfjsLib: any = null
const pdfFileInputRef = ref<HTMLInputElement | null>(null)
const isAnalyzing = ref(false)

async function getPdfJs(): Promise<any> {
  if (pdfjsLib) return pdfjsLib
  // 已经加载过
  if ((window as any).pdfjsLib) {
    pdfjsLib = (window as any).pdfjsLib
    if (!pdfjsLib.GlobalWorkerOptions.workerSrc) {
      pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js'
    }
    return pdfjsLib
  }
  // 动态加载 CDN 版本
  await new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('加载 PDF.js 失败，请检查网络连接'))
    document.head.appendChild(script)
  })
  pdfjsLib = (window as any).pdfjsLib
  pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js'
  return pdfjsLib
}

// ==================== 打印报价参数 ====================
const print = reactive({
  colorThreshold: 70,
  blackPrice: 0.5,
  colorPrice: 1.0,
  fullColorExtra: 0.1,
  bindingFee: 0,
  otherFee: 0,
})

let fileIdCounter = 0
const printFiles = ref<any[]>([])
const printResult = ref<any>(null)

// --- 文件上传 ---
function triggerPdfUpload() {
  const input = pdfFileInputRef.value
  if (input) input.click()
}

async function onPdfFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return

  const newFileIds: number[] = []
  for (const file of Array.from(input.files)) {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      ElMessage.warning(`跳过非PDF文件: ${file.name}`)
      continue
    }
    const id = ++fileIdCounter
    printFiles.value.push({
      id,
      name: file.name,
      file: file as any,
      totalPages: 0,
      blackPages: 0,
      colorPages: 0,
      fullColorPages: 0,
      bwCopies: 4,
      colorCopies: 1,
      analyzing: false,
      analyzed: false,
      hasFile: true,
      progress: 0,
      progressText: '',
    })
    newFileIds.push(id)
  }
  // 重置 input 以允许重复选择同一文件
  input.value = ''

  // 自动识别所有新上传的文件
  if (newFileIds.length > 0) {
    await analyzeFilesByIds(newFileIds)
  }
}

function addManualFile() {
  printFiles.value.push({
    id: ++fileIdCounter,
    name: '',
    file: null,
    totalPages: 0,
    blackPages: 0,
    colorPages: 0,
    fullColorPages: 0,
    bwCopies: 4,
    colorCopies: 1,
    analyzing: false,
    analyzed: false,
    hasFile: false,
    progress: 0,
    progressText: '',
  })
}

function clearPrintFiles() {
  printFiles.value = []
  printResult.value = null
}

const hasUnanalyzedFiles = computed(() =>
  printFiles.value.some(f => f.hasFile && !f.analyzed && !f.analyzing)
)

// --- PDF 色彩识别 ---
async function analyzeFilesByIds(ids: number[]) {
  isAnalyzing.value = true
  try {
    for (const id of ids) {
      const idx = printFiles.value.findIndex(f => f.id === id)
      if (idx >= 0) {
        await analyzeSingleFile(idx)
      }
    }
  } finally {
    isAnalyzing.value = false
  }
}

async function analyzeAllFiles() {
  isAnalyzing.value = true
  try {
    for (let i = 0; i < printFiles.value.length; i++) {
      const f = printFiles.value[i]
      if (f.hasFile && !f.analyzed && !f.analyzing) {
        await analyzeSingleFile(i)
      }
    }
  } finally {
    isAnalyzing.value = false
  }
}

async function analyzeSingleFile(idx: number) {
  const f = printFiles.value[idx]
  if (!f || !f.hasFile || !f.file) return

  f.analyzing = true
  f.analyzed = false
  f.progress = 0
  f.progressText = '准备中...'
  f.blackPages = 0
  f.colorPages = 0
  f.fullColorPages = 0

  try {
    const lib = await getPdfJs()
    const arrayBuffer = await (f.file as File).arrayBuffer()
    const pdf = await lib.getDocument({ data: new Uint8Array(arrayBuffer) }).promise
    f.totalPages = pdf.numPages

    let blackPages = 0
    let colorPages = 0
    let fullColorPages = 0

    for (let pageIdx = 1; pageIdx <= pdf.numPages; pageIdx++) {
      f.progressText = `分析第 ${pageIdx} / ${pdf.numPages} 页`
      f.progress = Math.round((pageIdx / pdf.numPages) * 100)

      const page = await pdf.getPage(pageIdx)
      // 渲染分辨率与 Python PyMuPDF dpi=72 对齐（scale=1.0 ≈ 72dpi）
      const scale = 1.0
      const viewport = page.getViewport({ scale })
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.floor(viewport.width))
      canvas.height = Math.max(1, Math.floor(viewport.height))
      const ctx = canvas.getContext('2d')!

      try {
        // Python PyMuPDF 默认白色背景无透明像素，canvas 也需要先填白
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
        await page.render({ canvasContext: ctx, viewport }).promise

        // 缩放到150px检测（Python用200px，这里用更小尺寸来增加 colorRatio
        // 弥补 PDF.js 渲染引擎与 PyMuPDF 的差异，防止漏检边界页）
        const THUMB_SIZE = 150
        const thumbW = canvas.width > THUMB_SIZE ? THUMB_SIZE : canvas.width
        const thumbH = Math.max(1, Math.floor(THUMB_SIZE * canvas.height / canvas.width))
        const thumb = document.createElement('canvas')
        thumb.width = thumbW
        thumb.height = thumbH
        const thumbCtx = thumb.getContext('2d')!
        thumbCtx.fillStyle = '#ffffff'
        thumbCtx.fillRect(0, 0, thumbW, thumbH)
        thumbCtx.imageSmoothingEnabled = true
        thumbCtx.imageSmoothingQuality = 'high'
        thumbCtx.drawImage(canvas, 0, 0, thumbW, thumbH)

        const thumbImageData = thumbCtx.getImageData(0, 0, thumbW, thumbH)
        const data = thumbImageData.data
        let colorPixelCount = 0
        const totalPixels = thumbW * thumbH

        for (let i = 0; i < data.length; i += 4) {
          const r = data[i]
          const g = data[i + 1]
          const b = data[i + 2]
          // 严格判定：只有 r==g==b 才算黑白（与Python逻辑一致）
          if (!(r === g && g === b)) {
            colorPixelCount++
          }
        }

        const colorRatio = colorPixelCount / totalPixels
        // 彩色判定阈值：5% 以上像素有颜色
        if (colorRatio > 0.05) {
          colorPages++
          // 满版彩色判定
          if (colorRatio >= print.colorThreshold / 100) {
            fullColorPages++
          }
        } else {
          blackPages++
        }
      } finally {
        page.cleanup()
        canvas.width = 0
        canvas.height = 0
      }
    }

    // 防御：满版彩色不超过彩色页
    if (fullColorPages > colorPages) fullColorPages = colorPages

    f.blackPages = blackPages
    f.colorPages = colorPages
    f.fullColorPages = fullColorPages
    f.analyzed = true
    f.progress = 100
    f.progressText = '分析完成'

    pdf.destroy()

    ElMessage.success(`${f.name}：共${pdf.numPages}页，黑白${blackPages}页，彩色${colorPages}页（满版${fullColorPages}页）`)
  } catch (e: any) {
    ElMessage.error(`分析 ${f.name} 失败: ${e.message || '未知错误'}`)
    f.progressText = '分析失败'
  } finally {
    f.analyzing = false
  }
}

function calcPrint() {
  if (printFiles.value.length === 0) {
    ElMessage.warning('请先添加文件')
    return
  }

  let totalBlackFinal = 0
  let totalNormalColorFinal = 0
  let totalFullColorFinal = 0
  let totalCopies = 0
  const fileDetails: any[] = []

  for (const f of printFiles.value) {
    let blackPages = f.blackPages || 0
    let colorPages = f.colorPages || 0
    let fullColorPages = f.fullColorPages || 0
    const bwCopies = f.bwCopies || 0
    const colorCopies = f.colorCopies || 0

    if (fullColorPages > colorPages) fullColorPages = colorPages
    const normalColorPages = colorPages - fullColorPages
    const totalPages = blackPages + colorPages

    const bwFromBlack = totalPages * bwCopies
    const bwFromColor = blackPages * colorCopies
    const colorFromColor = colorPages * colorCopies

    totalCopies += bwCopies + colorCopies
    totalBlackFinal += bwFromBlack + bwFromColor
    totalNormalColorFinal += normalColorPages * colorCopies
    totalFullColorFinal += fullColorPages * colorCopies

    fileDetails.push({
      name: f.name || `文件${f.id}`,
      totalPages, black: blackPages, color: colorPages,
      fullColor: fullColorPages, normalColor: normalColorPages,
      bwCopies, colorCopies,
      bwFromBlack, bwFromColor, colorFromColor,
    })
  }

  const blackTotal = totalBlackFinal * print.blackPrice
  const normalColorTotal = totalNormalColorFinal * print.colorPrice
  const fullColorPricePer = print.colorPrice + print.fullColorExtra
  const fullColorTotal = totalFullColorFinal * fullColorPricePer
  const total = blackTotal + normalColorTotal + fullColorTotal + print.bindingFee + print.otherFee
  const perCopy = totalCopies > 0 ? total / totalCopies : 0

  printResult.value = {
    fileDetails,
    totalBlack: totalBlackFinal,
    totalNormalColor: totalNormalColorFinal,
    totalFullColor: totalFullColorFinal,
    totalCopies,
    blackPrice: print.blackPrice,
    colorPrice: print.colorPrice,
    fullColorPrice: fullColorPricePer,
    blackTotal,
    normalColorTotal,
    fullColorTotal,
    bindingFee: print.bindingFee,
    otherFee: print.otherFee,
    total,
    perCopy,
  }
}

// ==================== 物料 ====================
const allMats = ref<any[]>([])
const filteredMats = ref<any[]>([])
const matKeyword = ref('')
const matCatFilter = ref('')
const categories = ref<any[]>([])
const selectedMats = ref<any[]>([])
const matsLoading = ref(true)

async function loadMatsAndCats() {
  matsLoading.value = true
  try {
    const [matRes, catRes] = await Promise.all([
      materialApi.listAll(),
      materialApi.getCategories(),
    ])
    allMats.value = matRes.data || matRes || []
    categories.value = catRes.data || catRes || []
    filterMats()
  } catch {
    ElMessage.error('加载物料失败')
  } finally {
    matsLoading.value = false
  }
}

function filterMats() {
  let list = allMats.value
  if (matKeyword.value.trim()) {
    const kw = matKeyword.value.toLowerCase()
    list = list.filter((m: any) => (m.name || '').toLowerCase().includes(kw) || (m.code || '').toLowerCase().includes(kw))
  }
  if (matCatFilter.value) {
    list = list.filter((m: any) => Number(m.categoryId) === Number(matCatFilter.value))
  }
  // 耗材和包装材料优先
  const priorityCats = categories.value.filter((c: any) => ['耗材', '包装材料', '辅料'].includes(c.name)).map((c: any) => c.id)
  const priority = list.filter((m: any) => priorityCats.includes(Number(m.categoryId)))
  const normal = list.filter((m: any) => !priorityCats.includes(Number(m.categoryId)))
  filteredMats.value = [...priority, ...normal]
}

function isMatSelected(id: number) { return selectedMats.value.some(s => s.materialId === id && !s.isCustom) }
function getMat(id: number) { return selectedMats.value.find(s => s.materialId === id && !s.isCustom) }

function addMat(m: any) {
  if (isMatSelected(m.id)) return
  selectedMats.value.push({
    materialId: m.id,
    materialName: m.name,
    spec: m.spec || '',
    unit: m.unit || '个',
    quantity: 1,
    unitPrice: m.price ? Number(m.price) : 0,
    amount: m.price ? Number(m.price) : 0,
    isCustom: false,
    quoteRemark: '',
  })
}

function removeMat(id: number) {
  selectedMats.value = selectedMats.value.filter(s => !(s.materialId === id && !s.isCustom))
}

function addCustomMat() {
  selectedMats.value.push({
    materialId: -(Date.now()),
    materialName: '',
    spec: '',
    unit: '项',
    quantity: 1,
    unitPrice: 0,
    amount: 0,
    isCustom: true,
    quoteRemark: '',
  })
}

function recalcMat(id: number) {
  const item = getMat(id)
  if (item) item.amount = Number((Math.max(1, item.quantity) * Math.max(0, item.unitPrice)).toFixed(2))
}

function recalcSelMat(idx: number) {
  const item = selectedMats.value[idx]
  if (item) item.amount = Number((Math.max(0, item.quantity) * Math.max(0, item.unitPrice)).toFixed(2))
}

const matTotal = computed(() => selectedMats.value.reduce((s, i) => s + (i.amount || 0), 0))

// ==================== 综合 ====================
const showGrandSummary = computed(() => labelResult.value || printResult.value || selectedMats.value.length > 0)
const grandTotalAmount = computed(() => {
  let total = matTotal.value
  if (labelResult.value) total += labelResult.value.total
  if (printResult.value) total += printResult.value.total
  return total
})

// ==================== 工具 ====================
function formatMoney(val: any) {
  return (Number(val) || 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function copyResult() {
  let text = ''
  if (activeMode.value === 'label' && labelResult.value) {
    const r = labelResult.value
    text = `【标签印刷报价】\n`
    text += `实际尺寸: ${r.lengthActual}×${r.widthActual}mm\n`
    text += `实际用纸: ${r.actualSheets}张\n`
    if (labelSubMode.value === 'imposition' && layoutResult.value) {
      text += `拼版: ${layoutResult.value.cols}×${layoutResult.value.rows}=${layoutResult.value.perSheet}个/张\n`
    }
    text += `纸张成本: ¥${r.paperCost.toFixed(2)}\n`
    text += `印刷费: ¥${r.printFee.toFixed(2)}\n`
    text += `划线+压痕: ¥${r.lineScore.toFixed(2)}\n`
    text += `覆膜: ¥${r.laminateTotal.toFixed(2)}\n`
    text += `装订费: ¥${r.bindingFee.toFixed(2)}\n`
    if (label.copies > 1) text += `份数: ${label.copies}\n`
    text += `总报价: ¥${r.total.toFixed(2)}\n`
    text += `单价: ¥${r.unitPrice.toFixed(4)}/个\n`
  } else if (activeMode.value === 'print' && printResult.value) {
    const r = printResult.value
    text = `【打印报价】\n`
    text += `文件数: ${r.fileDetails.length}\n`
    for (const fd of r.fileDetails) {
      text += `  ${fd.name}: 总${fd.totalPages}页(黑白${fd.black}/彩色${fd.color}/满版${fd.fullColor}) 黑白${fd.bwCopies}份 彩色${fd.colorCopies}份\n`
    }
    text += `黑白: ${r.totalBlack}张 × ${r.blackPrice} = ¥${r.blackTotal.toFixed(2)}\n`
    text += `普通彩色: ${r.totalNormalColor}张 × ${r.colorPrice} = ¥${r.normalColorTotal.toFixed(2)}\n`
    text += `满版彩色: ${r.totalFullColor}张 × ${r.fullColorPrice} = ¥${r.fullColorTotal.toFixed(2)}\n`
    text += `装订费: ¥${r.bindingFee.toFixed(2)}\n`
    text += `其他: ¥${r.otherFee.toFixed(2)}\n`
    text += `总报价: ¥${r.total.toFixed(2)}\n`
    text += `每份: ¥${r.perCopy.toFixed(4)}/份\n`
  }
  if (selectedMats.value.length > 0) {
    text += `\n【附加物料】\n`
    for (const s of selectedMats.value) {
      text += `  ${s.materialName} × ${s.quantity} = ¥${(s.amount || 0).toFixed(2)}\n`
    }
    text += `物料合计: ¥${matTotal.value.toFixed(2)}\n`
  }
  if (showGrandSummary.value) {
    text += `\n【综合总计】¥${grandTotalAmount.value.toFixed(2)}\n`
  }

  if (!text) {
    ElMessage.warning('暂无可复制的报价结果')
    return
  }
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

function resetCurrent() {
  if (activeMode.value === 'label') {
    labelResult.value = null
    layoutResult.value = null
    Object.assign(label, {
      paperType: '', paperPrice: 1.0, length: 100, width: 80,
      sheets: 1000, totalLabels: 1050,
      paperSizeKey: '320×450', customPaperW: 320, customPaperH: 450,
      gap: 3, margin: 5, corner: 5, demand: 1000,
      waste: 50, printFee: 150, lineFee: 0, scoreFee: 0, laminate: 0, ratio: 1.3,
      bindingFee: 0, copies: 1,
    })
  } else {
    printResult.value = null
    printFiles.value = []
    Object.assign(print, {
      colorThreshold: 70, blackPrice: 0.5, colorPrice: 1.0,
      fullColorExtra: 0.1, bindingFee: 0, otherFee: 0,
    })
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadMatsAndCats()
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.page-container { padding: 20px; max-width: 1400px; margin: 0 auto; }

/* ===== 面包屑 ===== */
.breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 13px; margin-bottom: 16px; color: var(--text2, #606266); }
.breadcrumb-item { cursor: pointer; transition: color .2s; }
.breadcrumb-item:hover:not(.active) { color: var(--primary, #409eff); }
.breadcrumb-separator { color: #c0c4cc; }
.breadcrumb-item.active { color: var(--text1, #303133); font-weight: 600; }

/* ===== 页头 ===== */
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; flex-wrap: wrap; gap: 8px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--text1, #303133); margin: 0; }
.page-actions { display: flex; gap: 8px; }

/* ===== 按钮 ===== */
.btn { padding: 8px 16px; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; border: none; transition: all .2s; display: inline-flex; align-items: center; gap: 4px; }
.btn-primary { background: var(--primary, #409eff); color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: .9; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(64,158,255,.35); }
.btn-primary:disabled { opacity: .55; cursor: not-allowed; }
.btn-default { background: #f5f7fa; color: var(--text2, #606266); border: 1px solid #dcdfe6; }
.btn-default:hover { background: #ebeef5; }
.btn-danger { background: linear-gradient(135deg, #f56c6c, #fa897b); color: #fff; }
.btn-danger:hover { opacity: .9; }
.btn-outline-primary { background: #ecf5ff; color: var(--primary, #409eff); border: 1px solid #b3d8ff; }
.btn-outline-primary:hover { background: var(--primary, #409eff); color: #fff; }
.btn-sm { padding: 3px 8px; font-size: 12px; border-radius: 4px; cursor: pointer; border: 1px solid transparent; transition: all .2s; }
.btn-primary-outline { color: var(--primary, #409eff); background: #ecf5ff; border-color: #b3d8ff; }
.btn-primary-outline:hover { background: var(--primary, #409eff); color: #fff; }
.btn-danger-outline { color: #f56c6c; background: #fef0f0; border-color: #fab6b6; }
.btn-danger-outline:hover { background: #f56c6c; color: #fff; }
.btn-custom-add { background: linear-gradient(135deg, #67c23a, #85ce61); color: #fff; border: none; font-size: 12px; padding: 4px 12px; white-space: nowrap; }
.btn-custom-add:hover { opacity: .9; }
.btn-calc { width: 100%; padding: 12px; font-size: 15px; font-weight: 600; margin-top: 12px; border-radius: 8px; }
.btn-calc-print { font-size: 17px; padding: 14px; margin-top: 16px; box-shadow: 0 4px 16px rgba(64,158,255,.3); letter-spacing: 1px; }
.calc-hint { text-align: center; font-size: 12px; color: #909399; margin-top: 6px; }

/* ===== 模式切换 ===== */
.mode-tabs { display: flex; gap: 12px; margin-bottom: 20px; }
.mode-tab {
  flex: 1; padding: 16px 20px; background: #f5f7fa; border: 2px solid #dcdfe6;
  border-radius: 12px; cursor: pointer; transition: all .25s; display: flex; flex-direction: column; gap: 4px;
}
.mode-tab:hover { border-color: var(--primary, #409eff); background: #ecf5ff; }
.mode-tab.active { border-color: var(--primary, #409eff); background: linear-gradient(135deg, rgba(64,158,255,.08), rgba(64,158,255,.02)); box-shadow: 0 2px 12px rgba(64,158,255,.15); }
.tab-icon { font-size: 22px; }
.tab-desc { font-size: 12px; color: #909399; }

/* ===== 子模式 ===== */
.sub-mode-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.sub-toggle { display: flex; background: #f0f2f5; border-radius: 8px; overflow: hidden; }
.sub-btn { padding: 8px 20px; border: none; background: transparent; font-size: 13px; cursor: pointer; transition: all .2s; color: var(--text2, #606266); }
.sub-btn.active { background: var(--primary, #409eff); color: #fff; font-weight: 600; }
.sub-hint { font-size: 12px; color: #909399; }

/* ===== 卡片 ===== */
.card { background: #fff; border-radius: 12px; border: 1px solid #ebeef5; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.04); }
.card-title-bar { display: flex; justify-content: space-between; align-items: center; font-size: 15px; font-weight: 600; color: var(--text1, #303133); padding-bottom: 12px; margin-bottom: 14px; border-bottom: 1.5px solid #f0f0f0; flex-wrap: wrap; gap: 8px; }

/* ===== 表单 ===== */
.form-row { display: flex; gap: 12px; margin-bottom: 12px; }
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 12px; font-weight: 500; color: var(--text2, #606266); white-space: nowrap; }
.form-control { border: 1px solid #dcdfe6; border-radius: 6px; padding: 7px 10px; font-size: 13px; outline: none; transition: all .2s; box-sizing: border-box; width: 100%; background: #fff; color: var(--text1, #303133); }
.form-control:focus { border-color: var(--primary, #409eff); box-shadow: 0 0 0 2px rgba(64,158,255,.12); }

/* ===== 两列布局 ===== */
.grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.col-left, .col-right { display: flex; flex-direction: column; }
@media (max-width: 1024px) { .grid-2col { grid-template-columns: 1fr; } }

/* ===== 拼版预览 ===== */
.preview-canvas-wrap { background: #fafafa; border: 1px solid #ebeef5; border-radius: 8px; min-height: 200px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.preview-svg { width: 100%; max-height: 400px; }
.preview-empty { color: #c0c4cc; font-size: 13px; padding: 40px 0; }

/* ===== 报价结果 ===== */
.result-card { position: sticky; top: 20px; }
.result-total { font-size: 18px; font-weight: 700; color: var(--primary, #409eff); }
.result-body { font-size: 13px; }
.result-section-title { font-weight: 600; color: var(--text1, #303133); margin-bottom: 8px; font-size: 14px; }
.result-row { display: flex; justify-content: space-between; padding: 4px 0; color: var(--text2, #606266); }
.result-row.highlight { background: rgba(64,158,255,.06); margin: 2px -8px; padding: 4px 8px; border-radius: 4px; }
.result-row span:last-child { font-weight: 500; color: var(--text1, #303133); }
.result-divider { height: 1px; background: #ebeef5; margin: 10px 0; }
.result-grand { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; font-size: 16px; color: var(--text1, #303133); }
.result-grand strong { font-size: 24px; color: var(--primary, #409eff); font-weight: 800; }
.result-unit { text-align: center; font-size: 12px; color: #909399; margin-top: 4px; }
.result-unit strong { color: var(--text1, #303133); }
.result-empty { text-align: center; padding: 40px 0; color: #c0c4cc; font-size: 13px; }

/* ===== 价格梯度 ===== */
.tier-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.tier-section { background: #f8fafc; border-radius: 8px; padding: 10px 14px; }
.tier-title { font-size: 12px; font-weight: 600; color: var(--text2, #606266); margin-bottom: 6px; }
.tier-row { display: flex; justify-content: space-between; font-size: 11px; padding: 2px 0; color: #909399; }
.tier-row strong { color: #e6a23c; }

/* ===== 文件列表 ===== */
.files-empty { text-align: center; padding: 24px 0; color: #c0c4cc; font-size: 13px; }
.file-item { background: #fafbfc; border: 1px solid #ebeef5; border-radius: 8px; padding: 12px; margin-bottom: 10px; transition: all .2s; }
.file-item.analyzing-item { border-color: var(--primary, #409eff); background: rgba(64,158,255,.03); }
.file-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; flex-wrap: wrap; gap: 6px; }
.file-name { font-weight: 600; color: var(--text1, #303133); font-size: 13px; display: flex; align-items: center; gap: 6px; }
.file-pages-badge { font-size: 11px; color: #909399; background: #f0f2f5; padding: 1px 6px; border-radius: 10px; font-weight: 400; }
.status-dot { font-size: 14px; }
.analyzing-dot { animation: pulse 1s ease-in-out infinite; display: inline-block; }
.analyzed-dot { display: inline-block; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.analyzing-text { font-size: 12px; color: var(--primary, #409eff); font-weight: 500; }
.analyzed-badge { font-size: 11px; color: #67c23a; background: #f0f9eb; padding: 2px 8px; border-radius: 10px; font-weight: 500; }
.analysis-progress-wrap { height: 4px; background: #ebeef5; border-radius: 2px; margin-bottom: 8px; overflow: hidden; }
.analysis-progress-bar { height: 100%; background: linear-gradient(90deg, var(--primary, #409eff), #67c23a); border-radius: 2px; transition: width .3s ease; }
.file-fields { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 6px; }
.field-item { display: flex; align-items: center; gap: 4px; }
.field-item label { font-size: 11px; color: #909399; white-space: nowrap; }
.field-input { width: 60px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 3px 6px; font-size: 12px; outline: none; text-align: center; }
.field-input:focus { border-color: var(--primary, #409eff); }
.file-hint { font-size: 11px; color: #c0c4cc; }
.file-detail-block { background: #f8fafc; border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; }
.fdb-name { font-weight: 600; color: var(--text1, #303133); font-size: 13px; margin-bottom: 4px; }
.fdb-info { font-size: 12px; color: #909399; margin-bottom: 6px; }
.fdb-row { font-size: 11px; color: var(--text2, #606266); padding: 2px 0; display: flex; gap: 6px; }
.fdb-row strong { color: var(--primary, #409eff); }

/* ===== 小按钮 ===== */
.delete-sm { width: 24px; height: 24px; padding: 0; font-size: 12px; color: #f56c6c; border: none; background: #fef0f0; border-radius: 4px; cursor: pointer; transition: all .2s; }
.delete-sm:hover { background: #f56c6c; color: #fff; }

/* ===== 物料区域 ===== */
.material-card .mat-list { max-height: 320px; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 8px; }
.mat-list::-webkit-scrollbar { width: 6px; }
.mat-list::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 3px; }
.mat-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; transition: all .2s; background: #fafbfc; }
.mat-item:hover { border-color: var(--primary, #409eff); }
.mat-item.selected { border-color: var(--primary, #409eff); background: rgba(64,158,255,.04); }
.mat-name { font-weight: 600; font-size: 13px; color: var(--text1, #303133); margin-bottom: 4px; }
.mat-info { display: flex; justify-content: space-between; align-items: center; }
.mat-price { font-size: 13px; font-weight: 600; color: #e6a23c; }
.mat-actions { display: flex; align-items: center; gap: 6px; margin-top: 8px; }
.mat-qty { border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; text-align: center; }
.mat-qty:focus { border-color: var(--primary, #409eff); }
.mat-price-input { border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; text-align: right; color: #e6a23c; }
.mat-price-input:focus { border-color: #e6a23c; }
.tag { display: inline-block; padding: 2px 8px; font-size: 11px; background: #ecf5ff; color: #409eff; border-radius: 4px; }

/* ===== 数据表格 ===== */
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table thead th { background: #f5f7fa; color: var(--text2, #606266); font-weight: 600; padding: 9px 8px; text-align: left; border-bottom: 1px solid #ebeef5; font-size: 12px; }
.data-table tbody td { padding: 7px 8px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; }
.data-table tbody tr:hover { background: #f8faff; }
.total-row td { background: #fdf6ec !important; font-size: 14px; padding: 10px 8px; border-top: 2px solid #e6a23c; }
.custom-row { background: linear-gradient(135deg, #f0f9eb, #fff) !important; }
.inline-edit-input { border: 1px solid #dcdfe6; border-radius: 4px; padding: 3px 6px; font-size: 12px; outline: none; transition: all .2s; background: #fff; color: var(--text1, #303133); box-sizing: border-box; }
.inline-edit-input:focus { border-color: #67c23a; }
.qty-inline { width: 54px; text-align: center; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; }
.qty-inline:focus { border-color: var(--primary, #409eff); }
.price-inline { width: 64px; text-align: right; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 12px; outline: none; color: #e6a23c; font-weight: 500; }
.price-inline:focus { border-color: #e6a23c; }
.remark-inline { width: 90px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 2px 4px; font-size: 11px; outline: none; text-align: center; }
.materials-empty { text-align: center; padding: 24px; color: #909399; grid-column: 1/-1; }
.loading-placeholder { text-align: center; padding: 40px 0; color: #909399; }

/* ===== 综合汇总 ===== */
.grand-summary-card { background: linear-gradient(135deg, #f8fafc, #f1f5f9); border: 2px solid var(--primary, #409eff); margin-top: 16px; }
.grand-summary-row { display: flex; align-items: center; gap: 24px; padding: 8px 0; flex-wrap: wrap; }
.gs-item { display: flex; flex-direction: column; gap: 2px; font-size: 12px; color: var(--text2, #606266); }
.gs-item strong { font-size: 18px; color: var(--text1, #303133); }
.gs-item.gs-final strong { font-size: 24px; color: var(--primary, #409eff); }
.gs-item.gs-final { font-size: 14px; color: var(--text1, #303133); font-weight: 600; }
.gs-divider { width: 2px; height: 40px; background: #dcdfe6; border-radius: 1px; }
</style>
