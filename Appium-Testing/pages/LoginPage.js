const BasePage = require('./BasePage');

class LoginPage extends BasePage {
    // Verified Resource IDs with Fallbacks
    get emailInput() { return $('//*[@resource-id="com.paybuddy:id/et_email"] | //android.widget.EditText[contains(@text, "Email")]'); }
    get passwordInput() { return $('//*[@resource-id="com.paybuddy:id/et_password"] | //android.widget.EditText[contains(@text, "Password")]'); }
    get loginButton() { return $('//*[@resource-id="com.paybuddy:id/btn_email_login"] | //*[@resource-id="com.paybuddy:id/btn_login"] | //android.widget.Button[contains(@text, "Login")]'); }
    get createAccountLink() { return $('//*[@resource-id="com.paybuddy:id/tv_create_account"]'); }

    async login(email, password) {
        await this.type(await this.emailInput, email);
        await this.type(await this.passwordInput, password);
        await this.click(await this.loginButton);

        // Wait for transition and handle popups immediately on the next screen
        await driver.pause(2000);
        const DashboardPage = require('./DashboardPage');
        await DashboardPage.handlePopups();
    }

    async handleCommonDialogs() {
        const dialogAllowBtn = await $('//*[@resource-id="android:id/button1"] | //*[@text="Allow"]');
        const permissionAllowBtn = await $('//*[@resource-id="com.android.permissioncontroller:id/permission_allow_button"]');

        if (await this.isDisplayed(dialogAllowBtn)) {
            await this.click(dialogAllowBtn);
        }

        if (await this.isDisplayed(permissionAllowBtn)) {
            await this.click(permissionAllowBtn);
        }
    }

    async register(email, password) {
        await this.type(await this.emailInput, email);
        await this.type(await this.passwordInput, password);
        await this.click(await this.createAccountLink);
    }

    async isAt() {
        return await this.isDisplayed(await this.emailInput);
    }
}

module.exports = new LoginPage();
