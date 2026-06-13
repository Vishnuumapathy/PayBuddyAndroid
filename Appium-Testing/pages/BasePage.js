const { Logger } = require('../utils/logger');

class BasePage {
    async click(element) {
        const selector = element.selector || 'unknown';
        try {
            await element.waitForDisplayed({ timeout: 20000 });
            await element.click();
            Logger.info(`Clicked element: ${selector}`);
        } catch (error) {
            Logger.error(`Failed to click element: ${selector}. Trying tap by coordinates...`);
            try {
                const location = await element.getLocation();
                const size = await element.getSize();
                const x = location.x + size.width / 2;
                const y = location.y + size.height / 2;
                await driver.touchAction({ action: 'tap', x, y });
            } catch (e) {
                Logger.error(`Coordinate tap also failed: ${e.message}`);
            }
        }
    }

    async type(element, text) {
        try {
            await element.waitForDisplayed({ timeout: 20000 });
            await element.setValue(text);
            if (await driver.isKeyboardShown()) await driver.hideKeyboard();
            Logger.info(`Typed "${text}" into element: ${element.selector}`);
        } catch (error) {
            Logger.error(`Failed to type into element: ${element.selector}. Error: ${error.message}`);
        }
    }

    async isDisplayed(element) {
        try {
            await element.waitForDisplayed({ timeout: 5000 });
            return await element.isDisplayed();
        } catch (e) {
            return false;
        }
    }

    async waitForActivity(activityName, timeout = 20000) {
        try {
            await driver.waitUntil(async () => {
                const currentActivity = await driver.getCurrentActivity();
                return currentActivity.includes(activityName);
            }, {
                timeout,
                timeoutMsg: `Timed out waiting for activity ${activityName}`
            });
            Logger.info(`Successfully navigated to: ${activityName}`);
        } catch (e) {
            Logger.warn(`Activity wait failed for ${activityName}: ${e.message}`);
        }
    }
}

module.exports = BasePage;
