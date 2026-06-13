# PayBuddy Appium Automation Framework

This is a production-grade automation framework for the PayBuddy Android Application. It uses Node.js, WebdriverIO, Appium, and Mocha.

## Framework Features
- **Page Object Model (POM)**: Organized and maintainable code structure.
- **Financial Integrity Tracking**: Custom logic to ensure ledger and balance accuracy.
- **Excel Reporting**: Automatic generation of `PayBuddy_Execution_Report.xlsx`.
- **Allure Reporting**: Rich HTML reports with screenshots and steps.
- **Winston Logging**: Detailed execution logs stored in `/logs`.
- **Screenshot Capture**: Automatic failure screenshots.

## Project Structure
```
Appium-Testing/
├── tests/           # 120+ Test Cases
├── pages/           # Page Object Classes
├── utils/           # Excel, Logger, and Financial Helpers
├── reports/         # Allure Results
├── excel-reports/   # Generated XLSX Reports
├── screenshots/     # Failure Screenshots
├── logs/            # Execution Logs
├── config/          # Configurations
├── package.json     # Dependencies
└── wdio.conf.js     # Main WebdriverIO Config
```

## Prerequisites
1. **Node.js**: Install latest LTS.
2. **Java JDK**: Install JDK 11+ and set `JAVA_HOME`.
3. **Android SDK**: Install Command Line Tools and set `ANDROID_HOME`.
4. **Appium**: `npm install -g appium`
5. **UiAutomator2 Driver**: `appium driver install uiautomator2`

## Setup Instructions
1. Navigate to directory: `cd Appium-Testing`
2. Install dependencies: `npm install`
3. Connect Android device or start Emulator.
4. Verify ADB connection: `adb devices`

## Running Tests
- **Run Full Suite**: `npm test`
- **Generate Allure Report**: `npm run report`
- **Clean Reports**: `npm run clean`

## Financial Integrity Rules
- The framework uses dedicated **TEST accounts**.
- It verifies that `Total Paid + Remaining Balance = Sale Amount`.
- It ensures ledger entries are created atomically for every transaction.
