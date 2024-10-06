package com.li.bot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.li.bot.entity.database.User;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
public interface IUserService extends IService<User> {

    void insertUser(User user);

}
