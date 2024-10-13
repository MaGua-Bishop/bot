package com.li.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInfoListVo;
import com.li.bot.entity.database.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Mapper
public interface ConvoysMapper extends BaseMapper<Convoys> {

//    @Select("select * from tg_convoys")
//    IPage<Convoys> selectConvoysList(Page<Convoys> page);


    IPage<ConvoysInfoListVo> selectConvoysList(Page<Convoys> page);


}
