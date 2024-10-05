package com.li.bot.config;

import com.li.bot.service.impl.BotServiceImpl;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;

@Data
@Configuration
@ConfigurationProperties(prefix = "bot")
public class BotConfig {

    //bot_Name
    private String botname;
    //bot_Token
    private String token;

    private String host;

    private Integer port;

    private String url ;



    public DefaultBotOptions getDefaultBotOptions(){
        //设置Http代理
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setProxyHost(host);
        botOptions.setProxyPort(port);

        //选择代理类型:[HTTP|SOCKS4|SOCKS5](默认:NO_PROXY)
        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);

        return botOptions;
    }

}
