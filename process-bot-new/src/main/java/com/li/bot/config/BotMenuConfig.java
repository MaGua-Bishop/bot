package com.li.bot.config;

import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class BotMenuConfig {

    private List<BotCommand> menu = new ArrayList<>();


    public List<BotCommand> getMenu() {
        return menu;
    }





}
