const BasePage = require('./BasePage');

class OnboardingPage extends BasePage {
    get vendorNameInput() { return $('//*[@resource-id="vendor_name_input"] | //android.widget.EditText[@text="Vendor Name"] | //android.widget.EditText[@hint="Vendor Name"]'); }
    get shopNameInput() { return $('//*[@resource-id="shop_name_input"] | //android.widget.EditText[@text="Shop Name"] | //android.widget.EditText[@hint="Shop Name"]'); }
    get phoneInput() { return $('//*[@resource-id="phone_input"] | //android.widget.EditText[@text="Phone"] | //android.widget.EditText[@hint="Phone"]'); }
    get emailInput() { return $('//*[@resource-id="email_input"] | //android.widget.EditText[@text="Email"] | //android.widget.EditText[@hint="Email"]'); }
    get upiIdInput() { return $('//*[@resource-id="upi_id_input"] | //android.widget.EditText[contains(@text, "UPI")] | //android.widget.EditText[contains(@hint, "UPI")]'); }
    get saveButton() { return $('//*[@resource-id="save_button"] | //android.widget.Button[contains(@text, "Save")] | //android.widget.Button[contains(@text, "Finish")]'); }

    async completeOnboarding(name, shop, phone, email) {
        await this.type(await this.vendorNameInput, name);
        await this.type(await this.shopNameInput, shop);
        await this.type(await this.phoneInput, phone);

        // Email might be pre-filled
        const emailEl = await this.emailInput;
        const currentEmail = await emailEl.getText();

        if (!currentEmail || currentEmail === 'Email' || currentEmail === '') {
            await this.type(emailEl, email);
        } else {
            // If already has text (e.g. from Firebase Auth), we can skip typing or clear and type
            // For onboarding, if it's correct, we just leave it.
        }

        await this.click(await this.saveButton);
    }
}

module.exports = new OnboardingPage();
