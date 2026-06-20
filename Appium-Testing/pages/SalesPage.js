const BasePage = require('./BasePage');

class SalesPage extends BasePage {
    get createSaleBtn() { return $('//*[@content-desc="Add Sale"] | //android.view.View[@content-desc="Add Sale"] | //*[contains(@text, "Create Your First Sale")] | //*[@resource-id="com.paybuddy:id/btn_new_sale"]'); }
    get customerSpinner() { return $('//android.widget.EditText[contains(@text, "Select Customer")] | //android.widget.Spinner | //*[@resource-id="com.paybuddy:id/spinner_customer"]'); }
    get itemNameInput() { return $('~item_name_input | //android.widget.EditText[.//android.widget.TextView[@text="Item Name"]] | //android.widget.EditText[contains(@text, "Item Name")]'); }
    get qtyInput() { return $('~qty_input | //android.widget.EditText[.//android.widget.TextView[@text="Qty"]] | //android.widget.EditText[contains(@text, "Qty")]'); }
    get priceInput() { return $('~unit_price_input | //android.widget.EditText[.//android.widget.TextView[@text="Unit Price"]] | //android.widget.EditText[contains(@text, "Unit Price")]'); }
    get amountInput() { return $('//android.widget.EditText[contains(@text, "Total Amount")] | //*[@resource-id="com.paybuddy:id/et_sale_amount"]'); }
    get submitSaleBtn() { return $('~submit_sale_button | //android.widget.Button[contains(@text, "Create Sale")] | //*[contains(@text, "Create Sale")] | //*[@resource-id="com.paybuddy:id/btn_submit_sale"]'); }

    async createSale(customerName, itemName, qty, price) {
        if (await this.isDisplayed(await this.customerSpinner)) {
            await this.click(await this.customerSpinner);
            const customerOption = await $(`//android.widget.TextView[@text="${customerName}"]`);
            await this.click(customerOption);
        }
        await this.type(await this.itemNameInput, itemName);
        await this.type(await this.qtyInput, qty);
        await this.type(await this.priceInput, price);

        if (await driver.isKeyboardShown()) await driver.hideKeyboard();
        await this.click(await this.submitSaleBtn);
    }
}

module.exports = new SalesPage();
