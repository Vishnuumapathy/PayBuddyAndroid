const BasePage = require('./BasePage');

class RemindersPage extends BasePage {
    get remindersList() { return $('//*[@resource-id="com.paybuddy:id/rv_reminders"] | //android.view.View[contains(@content-desc, "Reminder List")]'); }
    get addReminderBtn() { return $('//*[@resource-id="com.paybuddy:id/fab_add_reminder"] | //android.widget.Button[contains(@text, "Reminder")]'); }
}

module.exports = new RemindersPage();
