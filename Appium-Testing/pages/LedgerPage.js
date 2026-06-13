const BasePage = require('./BasePage');

class LedgerPage extends BasePage {
    get ledgerSummary() { return $('//*[@resource-id="com.paybuddy:id/tv_ledger_summary"]'); }
    get transactionList() { return $('//*[@resource-id="com.paybuddy:id/rv_ledger_transactions"]'); }
    get exportBtn() { return $('//*[@resource-id="com.paybuddy:id/btn_export_ledger"]'); }
}

module.exports = new LedgerPage();
