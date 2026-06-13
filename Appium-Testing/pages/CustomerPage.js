const BasePage = require('./BasePage');

class CustomerPage extends BasePage {
    get addCustomerBtn() { return $('//*[@content-desc="Add Customer"] | //android.view.View[@content-desc="Add Customer"] | //android.widget.Button[@text="Add Customer"] | //*[@resource-id="com.paybuddy:id/fab_add_customer"]'); }
    get nameInput() { return $('//android.widget.EditText[contains(@text, "Full Name")] | //android.widget.EditText[contains(@hint, "Full Name")] | //android.widget.EditText[.//android.widget.TextView[@text="Full Name"]] | //*[@resource-id="com.paybuddy:id/et_customer_name"]'); }
    get phoneInput() { return $('//android.widget.EditText[contains(@text, "Phone Number")] | //android.widget.EditText[contains(@hint, "Phone Number")] | //android.widget.EditText[.//android.widget.TextView[@text="Phone Number"]] | //*[@resource-id="com.paybuddy:id/et_phone_number"]'); }
    get saveBtn() { return $('//android.widget.Button[contains(@text, "Save")] | //*[contains(@text, "Save Customer")] | //android.view.View[@clickable="true"][.//android.widget.TextView[@text="Save Customer"]] | //*[@resource-id="com.paybuddy:id/btn_save_customer"]'); }
    get customerList() { return $('//*[@resource-id="com.paybuddy:id/rv_customers"] | //android.view.View[contains(@content-desc, "Customer List")] | //*[contains(@text, "No customers yet")]'); }

    async addCustomer(name, phone) {
        if (await this.isDisplayed(await this.addCustomerBtn)) {
            await this.click(await this.addCustomerBtn);
        }
        await this.type(await this.nameInput, name);
        await this.type(await this.phoneInput, phone);
        await this.click(await this.saveBtn);
    }
}

module.exports = new CustomerPage();
