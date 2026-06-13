const LoginPage = require('../pages/LoginPage');
const DashboardPage = require('../pages/DashboardPage');
const CustomerPage = require('../pages/CustomerPage');
const SalesPage = require('../pages/SalesPage');
const LedgerPage = require('../pages/LedgerPage');
const SettingsPage = require('../pages/SettingsPage');
const RemindersPage = require('../pages/RemindersPage');
const { expect } = require('chai');
const { Logger } = require('../utils/logger');

describe('PayBuddy System Navigation Deep-Dive', () => {
    const validEmail = 'androidtest@gmail.com';
    const validPass = 'testandroid';
    let testCustomer = 'NavTest ' + Math.floor(Math.random() * 1000);

    before(async () => {
        Logger.info('Starting System Navigation Deep-Dive...');
        await LoginPage.waitForActivity('LoginActivity', 20000);
        await LoginPage.login(validEmail, validPass);
        await DashboardPage.waitForActivity('DashboardActivity', 30000);
    });

    describe('Main Navigation (Bottom Bar)', () => {
        it('NAV-001: Access Dashboard', async () => {
            await DashboardPage.click(await DashboardPage.navDashboard);
            await driver.pause(2000);
            expect(await DashboardPage.isAt()).to.be.true;
        });

        it('NAV-002: Access Sales History', async () => {
            await DashboardPage.click(await DashboardPage.navSales);
            await driver.pause(2000);
            const title = $('//*[@text="Sales History"] | //*[@text="Sales"]');
            expect(await title.isDisplayed()).to.be.true;
        });

        it('NAV-003: Access Customers List', async () => {
            await DashboardPage.click(await DashboardPage.navCustomers);
            await driver.pause(2000);
            const title = $('//*[@text="Customers"]');
            expect(await title.isDisplayed()).to.be.true;
        });

        it('NAV-004: Access Reminders', async () => {
            await DashboardPage.click(await DashboardPage.navReminders);
            await driver.pause(2000);
            const title = $('//*[contains(@text, "Reminders")]');
            expect(await title.isDisplayed()).to.be.true;
        });

        it('NAV-005: Access Payment History', async () => {
            await DashboardPage.click(await DashboardPage.navLedger);
            await driver.pause(2000);
            const title = $('//*[@text="Payment History"] | //*[@text="Payments"] | //*[@text="Ledger"]');
            expect(await title.isDisplayed()).to.be.true;
        });
    });

    describe('Secondary Screens & Workflows', () => {
        it('WORK-001: Access Add Customer Screen', async () => {
            await DashboardPage.click(await DashboardPage.navCustomers);
            await driver.pause(2000);
            await CustomerPage.click(await CustomerPage.addCustomerBtn);
            await driver.pause(2000);
            const title = $('//*[@text="Add New Customer"] | //*[contains(@text, "Add Customer")]');
            expect(await title.isDisplayed()).to.be.true;
            await driver.back();
            await driver.pause(1000);
        });

        it('WORK-002: Access New Sale Screen', async () => {
            await DashboardPage.click(await DashboardPage.navSales);
            await driver.pause(2000);
            await SalesPage.click(await SalesPage.createSaleBtn);
            await driver.pause(2000);
            const title = $('//*[@text="New Sale"] | //*[@text="Create Sale"]');
            expect(await title.isDisplayed()).to.be.true;
            await driver.back();
            await driver.pause(1000);
        });

        it('WORK-003: Access Settings & Sub-screens', async () => {
            // Click Settings icon
            await DashboardPage.click(await DashboardPage.navSettings);
            await driver.pause(2000);

            // Verify we are in Settings
            expect(await $('//*[@text="Settings"]').isDisplayed()).to.be.true;

            // Business Profile
            await SettingsPage.click(await SettingsPage.businessProfile);
            await driver.pause(2000);
            expect(await $('//*[contains(@text, "Profile")]').isDisplayed()).to.be.true;
            await driver.back();
            await driver.pause(1000);

            // Security
            await SettingsPage.click(await SettingsPage.securityNotifications);
            await driver.pause(2000);
            expect(await $('//*[contains(@text, "Security")]').isDisplayed()).to.be.true;
            await driver.back();
            await driver.pause(1000);

            // Archives
            await SettingsPage.click(await SettingsPage.archivedRecords);
            await driver.pause(2000);
            expect(await $('//*[contains(@text, "Archived")]').isDisplayed()).to.be.true;
            await driver.back();
            await driver.pause(1000);
        });
    });

    describe('Entity-Specific Navigation (Deep Links)', () => {
        it('DEEP-001: Access Customer Profile & Sub-tabs', async () => {
            await DashboardPage.click(await DashboardPage.navCustomers);
            await driver.pause(2000);

            // Ensure at least one customer exists or find one
            let customerCard = $('//*[contains(@text, "NavTest")] | //*[contains(@text, "Automation")] | //android.view.View[@clickable="true"][.//android.widget.TextView]');

            if (!(await customerCard.isDisplayed())) {
                Logger.info('No customer card found, creating one...');
                await CustomerPage.addCustomer(testCustomer, '9876543210');
                await driver.pause(2000);
                customerCard = $(`//*[contains(@text, "${testCustomer}")]`);
            }

            await CustomerPage.click(customerCard);
            await driver.pause(2000);

            // Now in Customer Profile - The title should be the customer name
            Logger.info('In Customer Profile');

            // Check sub-actions in Profile (e.g. Ledger, All Sales)
            const ledgerBtn = $('//*[contains(@text, "Ledger")]');
            if (await ledgerBtn.isDisplayed()) {
                await ledgerBtn.click();
                await driver.pause(2000);
                await driver.back();
                await driver.pause(1000);
            }

            const salesBtn = $('//*[contains(@text, "All Sales")] | //*[contains(@text, "Sales")]');
            if (await salesBtn.isDisplayed()) {
                await salesBtn.click();
                await driver.pause(2000);
                await driver.back();
                await driver.pause(1000);
            }
        });
    });

    after(async () => {
        await DashboardPage.click(await DashboardPage.navSettings);
        await SettingsPage.logout();
    });
});
