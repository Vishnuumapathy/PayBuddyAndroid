const BasePage = require('./BasePage');

class DashboardPage extends BasePage {
    // Verified Resource IDs & Content Descriptions
    get welcomeText() { return $('//*[contains(@text, "Good")] | //*[@resource-id="com.paybuddy:id/tv_welcome"]'); }
    get statsGrid() { return $('//*[@resource-id="com.paybuddy:id/grid_stats"] | //*[contains(@text, "Today")]'); }
    get newSaleBtn() { return $('//android.widget.TextView[@text="New Sale"] | //*[@resource-id="com.paybuddy:id/btn_new_sale"] | //android.widget.Button[contains(@text, "New Sale")]'); }
    get recordPaymentBtn() { return $('//android.widget.TextView[@text="Payment"] | //*[@resource-id="com.paybuddy:id/btn_record_payment"]'); }
    get bottomNav() { return $('//*[@resource-id="com.paybuddy:id/bottom_nav"]'); }
    get ledgerList() { return $('//*[@resource-id="com.paybuddy:id/rv_ledger"]'); }

    // Navigation IDs with Text Fallbacks for Bottom Nav
    get navDashboard() { return $('//*[@resource-id="com.paybuddy:id/dashboardFragment"] | //*[contains(@content-desc, "Dashboard")] | //*[@text="Dashboard"]'); }
    get navCustomers() { return $('//*[@resource-id="com.paybuddy:id/customerFragment"] | //*[contains(@content-desc, "Customers")] | //*[@text="Customers"]'); }
    get navSales() { return $('//*[@resource-id="com.paybuddy:id/salesFragment"] | //*[contains(@content-desc, "Sales")] | //*[@text="Sales"]'); }
    get navLedger() { return $('//*[@resource-id="com.paybuddy:id/paymentHistoryFragment"] | //*[contains(@content-desc, "Payments")] | //*[contains(@content-desc, "Ledger")] | //*[@text="Payments"] | //*[@text="Ledger"]'); }
    get navReminders() { return $('//*[@resource-id="com.paybuddy:id/reminderFragment"] | //*[contains(@content-desc, "Reminders")] | //*[@text="Reminders"]'); }
    get navSettings() { return $('~Settings'); }

    async isAt() {
        return await this.isDisplayed(await this.welcomeText);
    }
}

module.exports = new DashboardPage();
