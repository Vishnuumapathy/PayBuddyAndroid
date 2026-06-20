const BasePage = require('./BasePage');
const { Logger } = require('../utils/logger');

class DashboardPage extends BasePage {
    get welcomeText() { return $('~welcome_text | //*[contains(@text, "Good")] | //*[@resource-id="com.paybuddy:id/tv_welcome"]'); }
    get statsGrid() { return $('~stats_grid | //*[@resource-id="com.paybuddy:id/grid_stats"] | //*[contains(@text, "Today")]'); }
    get navCustomers() { return $('//*[@resource-id="com.paybuddy:id/customerFragment"] | //*[contains(@content-desc, "Customers")] | //*[@text="Customers"]'); }
    get navSales() { return $('//*[@resource-id="com.paybuddy:id/salesFragment"] | //*[contains(@content-desc, "Sales")] | //*[@text="Sales"]'); }
    get navLedger() { return $('//*[@resource-id="com.paybuddy:id/paymentHistoryFragment"] | //*[contains(@content-desc, "Payments")] | //*[contains(@content-desc, "Ledger")] | //*[@text="Payments"] | //*[@text="Ledger"]'); }
    get navSettings() { return $('~settings_button | ~Settings'); }

    async isAt() {
        await this.handlePopups();
        return await this.isDisplayed(await this.welcomeText);
    }

    async handlePopups() {
        Logger.info('Scanning for blocking popups...');

        // Wait and click "Allow" on the Reminder Popup
        const allowBtn = await $('//*[@resource-id="android:id/button1"] | //*[@text="Allow"]');
        for(let i = 0; i < 3; i++) {
            if (await allowBtn.isDisplayed()) {
                Logger.info('Clicking "Allow" on Reminder Dialog...');
                await allowBtn.click();
                await driver.pause(1500);
            } else {
                await driver.pause(1000); // Wait for it to animate in
            }
        }

        // Handle the Android System Permission if it appears
        const systemAllow = await $('//*[@resource-id="com.android.permissioncontroller:id/permission_allow_button"]');
        if (await systemAllow.isDisplayed()) {
            Logger.info('Clicking "Allow" on System Permission...');
            await systemAllow.click();
            await driver.pause(1000);
        }
    }
}

module.exports = new DashboardPage();
