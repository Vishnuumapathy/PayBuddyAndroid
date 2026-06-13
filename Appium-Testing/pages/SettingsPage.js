const BasePage = require('./BasePage');

class SettingsPage extends BasePage {
    get logoutBtn() { return $('//*[contains(@text, "Logout")] | //*[@resource-id="com.paybuddy:id/tv_logout"]'); }
    get confirmLogoutBtn() { return $('//android.widget.Button[contains(@text, "Logout")] | //*[@resource-id="android:id/button1"]'); }

    // Sub-settings navigators
    get businessProfile() { return $('//*[contains(@text, "Business Profile")]'); }
    get securityNotifications() { return $('//*[contains(@text, "Security & Notifications")]'); }
    get archivedRecords() { return $('//*[contains(@text, "Archived Records")]'); }
    get resetAppData() { return $('//*[contains(@text, "Reset App Data")]'); }

    async logout() {
        await this.click(await this.logoutBtn);
        // Handle confirmation dialog if it appears
        if (await this.isDisplayed(await this.confirmLogoutBtn)) {
            await this.click(await this.confirmLogoutBtn);
        }
    }
}

module.exports = new SettingsPage();
