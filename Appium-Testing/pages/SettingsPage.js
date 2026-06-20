const BasePage = require('./BasePage');

class SettingsPage extends BasePage {
    // More specific selectors using content descriptions added in Compose
    get logoutBtn() { return $('~logout_item | //*[@text="Logout" and not(contains(@class, "Button"))] | //*[@resource-id="com.paybuddy:id/tv_logout"]'); }
    get confirmLogoutBtn() { return $('~confirm_logout_button | //android.widget.Button[.//android.widget.TextView[@text="Logout"]] | //android.widget.Button[@text="Logout"] | //*[@resource-id="android:id/button1"]'); }

    // Sub-settings navigators
    get businessProfile() { return $('//*[contains(@text, "Business Profile")]'); }
    get securityNotifications() { return $('//*[contains(@text, "Security & Notifications")]'); }
    get archivedRecords() { return $('//*[contains(@text, "Archived Records")]'); }
    get resetAppData() { return $('//*[contains(@text, "Reset App Data")]'); }

    async logout() {
        const logoutButton = await this.logoutBtn;
        await this.click(logoutButton);
        await driver.pause(1000); // Give dialog time to animate

        // Handle confirmation dialog if it appears
        const confirmBtn = await this.confirmLogoutBtn;
        if (await this.isDisplayed(confirmBtn)) {
            await this.click(confirmBtn);
        } else {
            Logger.warn('Confirm logout button not displayed, trying fallback...');
            // Try one more fallback if not found by selector
            const fallbackConfirm = await $('//*[@text="Logout"]');
            if (await this.isDisplayed(fallbackConfirm)) {
                await this.click(fallbackConfirm);
            }
        }
    }
}

module.exports = new SettingsPage();
