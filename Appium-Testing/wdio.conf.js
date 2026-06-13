const { ExcelReporter } = require('./utils/excelReporter');
const { getAuditData } = require('./utils/AuditData');
const { Logger } = require('./utils/logger');
const path = require('path');
const moment = require('moment');
const fs = require('fs');

// PORTABILITY FIX: Smart SDK detection
const possibleSdkPaths = [
    process.env.ANDROID_HOME,
    process.env.ANDROID_SDK_ROOT,
    'C:/Users/vichu/AppData/Local/Android/Sdk', // Your machine
    path.join(process.env.USERPROFILE || '', 'AppData/Local/Android/Sdk'), // Standard Windows location
];

const androidHome = possibleSdkPaths.find(p => p && fs.existsSync(p));
if (androidHome) {
    process.env.ANDROID_HOME = androidHome;
    // Add platform-tools to path for Appium
    process.env.PATH += `;${androidHome}/platform-tools;${androidHome}/emulator`;
}

exports.config = {
    runner: 'local',
    port: 4723,
    specs: [
        './tests/ComprehensiveSuite.test.js',
        './tests/SystemNavigation.test.js',
        './tests/PayBuddySmokeSuite.test.js'
    ],
    exclude: [],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:deviceName': 'Android Emulator', // Standard name
        'appium:automationName': 'UiAutomator2',
        // PORTABILITY FIX: Use relative path to the APK
        'appium:app': path.join(__dirname, '../app/build/intermediates/apk/debug/app-debug.apk'),
        'appium:appPackage': 'com.paybuddy',
        'appium:appActivity': 'com.paybuddy.ui.auth.SplashActivity',
        'appium:noReset': false,
        'appium:fullReset': false,
        'appium:dontStopAppOnReset': true,
        'appium:newCommandTimeout': 60000
    }],
    logLevel: 'error',
    bail: 0,
    waitforTimeout: 20000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    services: ['appium'],
    framework: 'mocha',
    reporters: [
        'spec',
        ['allure', {
            outputDir: './reports/allure-results',
            disableWebdriverStepsReporting: true,
            disableWebdriverScreenshotsReporting: false,
        }]
    ],
    mochaOpts: {
        ui: 'bdd',
        timeout: 900000
    },

    autoCompileOpts: {
        autoCompile: false
    },

    onPrepare: function () {
        // Create directory if not exists
        const reportDir = path.join(__dirname, 'excel-reports');
        if (!fs.existsSync(reportDir)) {
            fs.mkdirSync(reportDir, { recursive: true });
        }
    },

    before: async function () {
        await ExcelReporter.initReport();
    },

    afterTest: async function (test, context, { error, result, duration, passed, retries }) {
        const status = passed ? 'Passed' : 'Failed';

        // Parsing Test ID and Description
        // Support formats like "TC-001: Description" or "AUTH-001: Description"
        let testId = 'TC-GENERIC';
        let description = test.title;

        if (test.title.includes(':')) {
            const parts = test.title.split(':');
            testId = parts[0].trim();
            description = parts.slice(1).join(':').trim();
        } else {
            // Try regex for ID if no colon
            const idMatch = test.title.match(/[A-Z]+-[0-9]+/i);
            if (idMatch) {
                testId = idMatch[0];
                description = test.title.replace(testId, '').trim();
            }
        }

        // Map to Reviewer requested categories:
        // 1. UI/UX Test, 2. Functional Testing, 3. Unit Testing, 4. Validation Test, 5. Deployable Status
        let displayCategory = 'Functional Testing';

        if (testId.startsWith('UI')) {
            displayCategory = 'UI/UX Test';
        } else if (testId.startsWith('VAL')) {
            displayCategory = 'Validation Test';
        } else if (testId.startsWith('UNIT')) {
            displayCategory = 'Unit Testing';
        } else if (testId.startsWith('DEPL') || testId.startsWith('SETT') || testId.startsWith('LEDG')) {
            displayCategory = 'Deployable Status';
        } else {
            displayCategory = 'Functional Testing';
        }

        try {
            await ExcelReporter.addResult({
                id: testId,
                category: displayCategory,
                description: description,
                type: 'Automated',
                status: status,
                time: `${duration}ms`,
                remarks: error ? error.message.substring(0, 150) : 'Assertion passed successfully'
            });
        } catch (e) {
            console.error('Excel row error: ' + e.message);
        }

        if (!passed) {
            try {
                const fileName = `FAIL_${test.title.substring(0, 20).replace(/\s+/g, '_')}.png`;
                const screenshotPath = path.join(__dirname, 'screenshots');
                if (!fs.existsSync(screenshotPath)) fs.mkdirSync(screenshotPath, { recursive: true });
                await driver.saveScreenshot(path.join(screenshotPath, fileName));
            } catch (e) {}
        }
    },

    after: async function () {
        await ExcelReporter.saveReport();
    },

    onComplete: function() {
        console.log('Test Execution Completed. Excel report updated.');
    }
}
