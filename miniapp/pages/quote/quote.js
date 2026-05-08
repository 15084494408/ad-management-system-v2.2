"use strict";
const BLACK_TIERS = [[0, 100, 0.5], [100, 500, 0.4], [500, 1000, 0.3], [1000, Infinity, 0.2]];
const COLOR_TIERS = [[0, 50, 1.0], [50, 200, 0.8], [200, 500, 0.7], [500, 1000, 0.6], [1000, Infinity, 0.5]];
function getPriceByTier(count, tiers) {
    // ★ 修复 P1-金额：count <= 0 时返回第一档起步价，防止匹配到最后一档
    if (count <= 0)
        return tiers[0][2];
    for (const [min, max, price] of tiers) {
        if (count > min && count <= max)
            return price;
    }
    return tiers[tiers.length - 1][2];
}
Page({
    data: {
        mode: 'label',
        label: { paperType: '铜版纸-128g', paperPrice: '1.2', length: '', width: '', sheets: '', totalLabels: '', printPrice: '0.1', linePrice: '', copies: '1', ratio: '100' },
        print: { fileName: '', totalPages: '', blackPages: '', colorPages: '', bwCopies: '1', colorCopies: '1' },
        labelResult: null,
        printResult: null
    },
    onModeChange(e) { this.setData({ mode: e.detail.value }); },
    onLabelChange(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [`label.${field}`]: e.detail.value });
    },
    onPrintChange(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [`print.${field}`]: e.detail.value });
    },
    calcLabel() {
        const l = this.data.label;
        const sheets = parseFloat(l.sheets) || 0;
        const totalLabels = parseInt(l.totalLabels) || 0;
        const paperPrice = parseFloat(l.paperPrice) || 0;
        const printPrice = parseFloat(l.printPrice) || 0;
        const linePrice = parseFloat(l.linePrice) || 0;
        const copies = parseInt(l.copies) || 1;
        const ratio = (parseFloat(l.ratio) || 100) / 100;
        const paperCost = (sheets * paperPrice).toFixed(2);
        const printCost = (sheets * printPrice * copies).toFixed(2);
        const lineCost = (sheets * linePrice).toFixed(2);
        const total = ((parseFloat(paperCost) + parseFloat(printCost) + parseFloat(lineCost)) * ratio).toFixed(2);
        this.setData({
            labelResult: {
                paperCost: paperCost + ' 元',
                printCost: printCost + ' 元',
                lineCost: lineCost + ' 元',
                total: total + ' 元'
            }
        });
    },
    calcPrint() {
        const p = this.data.print;
        const blackPages = parseInt(p.blackPages) || 0;
        const colorPages = parseInt(p.colorPages) || 0;
        const bwCopies = parseInt(p.bwCopies) || 1;
        const colorCopies = parseInt(p.colorCopies) || 1;
        const totalBw = blackPages * bwCopies;
        const totalColor = colorPages * colorCopies;
        const bwUnitPrice = getPriceByTier(totalBw, BLACK_TIERS);
        const colorUnitPrice = getPriceByTier(totalColor, COLOR_TIERS);
        const bwCost = (totalBw * bwUnitPrice).toFixed(2);
        const colorCost = (totalColor * colorUnitPrice).toFixed(2);
        const total = (parseFloat(bwCost) + parseFloat(colorCost)).toFixed(2);
        this.setData({
            printResult: {
                bwCost: `${bwCost} 元 (${totalBw}张 x ${bwUnitPrice})`,
                colorCost: `${colorCost} 元 (${totalColor}张 x ${colorUnitPrice})`,
                total: total + ' 元'
            }
        });
    }
});
