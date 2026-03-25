package com.kidslearn.schedule.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReminderService {

    /**
     * 每天 17:00 (UTC+8) 执行学习提醒
     */
    @Scheduled(cron = "0 0 9 * * ?") // 9:00 UTC = 17:00 UTC+8
    public void sendDailyReminders() {
        log.info("DAILY_REMINDER_JOB | start");
        // TODO: 查询今天还未学习的孩子，发送推送
        log.info("DAILY_REMINDER_JOB | end");
    }

    /**
     * 每周日 20:00 (UTC+8) 生成周报
     */
    @Scheduled(cron = "0 0 12 ? * SUN") // 12:00 UTC = 20:00 UTC+8
    public void sendWeeklyReports() {
        log.info("WEEKLY_REPORT_JOB | start");
        // TODO: 统计本周学习数据，推送给家长
        log.info("WEEKLY_REPORT_JOB | end");
    }
}
