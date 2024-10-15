package com.li.bot.task;

import com.li.bot.service.impl.ChannelMembersServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChannelCacheTask {

    @Autowired
    private ChannelMembersServiceImpl channelMembersService;


    @Scheduled(cron ="0 0 0 * * ?")
    public void channelCache() {
        channelMembersService.clearChannelMembers();
        log.info("执行频道用户缓存清除");
    }

}
