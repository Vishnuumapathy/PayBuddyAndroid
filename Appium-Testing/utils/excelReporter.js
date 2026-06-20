const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

class ExcelReporter {
    static workbook;
    static filePath = path.join(__dirname, '../excel-reports/PayBuddy_Execution_Report.xlsx');

    static async initReport() {
        if (!fs.existsSync(path.dirname(this.filePath))) {
            fs.mkdirSync(path.dirname(this.filePath), { recursive: true });
        }

        this.workbook = new ExcelJS.Workbook();

        if (fs.existsSync(this.filePath)) {
            try {
                await this.workbook.xlsx.readFile(this.filePath);
            } catch (e) {}
        }

        const categories = ['UI-UX', 'Functional', 'Unit', 'Validation', 'Deployment'];
        for (const cat of categories) {
            let ws = this.workbook.getWorksheet(cat);
            if (!ws) {
                ws = this.workbook.addWorksheet(cat);
                ws.columns = this.getColumnDefinitions();
                this.formatHeader(ws);
            }
        }
    }

    static getColumnDefinitions() {
        return [
            { header: 'Test ID', key: 'id', width: 20 },
            { header: 'Category', key: 'category', width: 15 },
            { header: 'Test Case Description', key: 'description', width: 45 },
            { header: 'Type', key: 'type', width: 15 },
            { header: 'Status', key: 'status', width: 12 },
            { header: 'Execution Time', key: 'time', width: 15 },
            { header: 'Remarks', key: 'remarks', width: 40 }
        ];
    }

    static formatHeader(worksheet) {
        const headerRow = worksheet.getRow(1);
        headerRow.height = 25;
        headerRow.eachCell((cell) => {
            cell.font = { name: 'Segoe UI', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF2D3E50' } };
            cell.alignment = { vertical: 'middle', horizontal: 'left' };
        });
    }

    static async addResult(data) {
        if (!this.workbook) await this.initReport();

        const categoryName = data.category || 'Functional';
        let worksheet = this.workbook.getWorksheet(categoryName);
        if (!worksheet) worksheet = this.workbook.addWorksheet(categoryName);

        let existingRow = null;
        worksheet.eachRow((r) => { if (r.getCell(1).value === data.id) existingRow = r; });

        const row = existingRow || worksheet.addRow({});
        row.getCell(1).value = data.id;
        row.getCell(2).value = data.category;
        row.getCell(3).value = data.description;
        row.getCell(4).value = data.type || 'Automated';
        row.getCell(5).value = data.status;
        row.getCell(6).value = data.time;
        row.getCell(7).value = data.remarks;

        row.height = 20;
        const statusCell = row.getCell(5);
        if (data.status === 'Passed') {
            statusCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC6EFCE' } };
            statusCell.font = { color: { argb: 'FF006100' }, bold: true };
        }

        try {
            await this.workbook.xlsx.writeFile(this.filePath);
        } catch (e) {}
    }

    // RE-ADDED TO FIX THE CRASH
    static async saveReport() {
        if (this.workbook) {
            try {
                await this.workbook.xlsx.writeFile(this.filePath);
            } catch (e) {}
        }
    }
}

module.exports = { ExcelReporter };
