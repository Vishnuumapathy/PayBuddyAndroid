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
        // Start fresh for every run
        this.workbook = new ExcelJS.Workbook();
        console.log('Excel Reporter Initialized');
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

    static async addResult(data) {
        if (!this.workbook) {
            await this.initReport();
        }

        const categoryName = data.category || 'General';
        // Excel tab names cannot exceed 31 chars and cannot contain certain chars
        const safeTabName = categoryName.substring(0, 31).replace(/[\[\]\*\?\/\\]/g, '');

        let worksheet = this.workbook.getWorksheet(safeTabName);

        if (!worksheet) {
            worksheet = this.workbook.addWorksheet(safeTabName);
            worksheet.columns = this.getColumnDefinitions();

            // Format header row
            const headerRow = worksheet.getRow(1);
            headerRow.height = 25;
            headerRow.eachCell((cell) => {
                cell.font = { name: 'Segoe UI', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
                cell.fill = {
                    type: 'pattern',
                    pattern: 'solid',
                    fgColor: { argb: 'FF2D3E50' } // Dark header background
                };
                cell.alignment = { vertical: 'middle', horizontal: 'left' };
                // Add borders to header
                cell.border = {
                    top: { style: 'thin', color: { argb: 'FF000000' } },
                    left: { style: 'thin', color: { argb: 'FF000000' } },
                    bottom: { style: 'thin', color: { argb: 'FF000000' } },
                    right: { style: 'thin', color: { argb: 'FF000000' } }
                };
            });
        }

        const row = worksheet.addRow({
            id: data.id,
            category: data.category,
            description: data.description,
            type: data.type || 'Automated',
            status: data.status,
            time: data.time,
            remarks: data.remarks
        });

        row.height = 20;
        row.eachCell((cell) => {
            cell.font = { name: 'Segoe UI', size: 10 };
            cell.alignment = { vertical: 'middle' };
        });

        // Status coloring
        const statusCell = row.getCell('status');
        if (data.status === 'Passed') {
            statusCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFC6EFCE' } };
            statusCell.font = { color: { argb: 'FF006100' }, name: 'Segoe UI', size: 10, bold: true };
        } else {
            statusCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFC7CE' } };
            statusCell.font = { color: { argb: 'FF9C0006' }, name: 'Segoe UI', size: 10, bold: true };
        }

        // Auto-save periodically or every row (safer for long runs)
        try {
            await this.workbook.xlsx.writeFile(this.filePath);
        } catch (e) {
            // EBUSY is common if the user has the file open
            if (e.code !== 'EBUSY') {
                console.error('Save error: ' + e.message);
            }
        }
    }

    static async saveReport() {
        if (!this.workbook) return;
        try {
            await this.workbook.xlsx.writeFile(this.filePath);
            console.log('=========================================');
            console.log('EXCEL REPORT UPDATED: ' + this.filePath);
            console.log('=========================================');
        } catch (e) {
            if (e.code === 'EBUSY') {
                const altPath = this.filePath.replace('.xlsx', `_Live_Backup_${Date.now()}.xlsx`);
                await this.workbook.xlsx.writeFile(altPath);
                console.error('Original file was locked. Saved backup to: ' + altPath);
            }
        }
    }
}

module.exports = { ExcelReporter };
