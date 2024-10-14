package com.li.bot.sessions;

import com.li.bot.entity.database.Convoys;

/**
 * @Author: li
 * @CreateTime: 2024-10-01
 */
public class UpdateConvoysSession {
    private UpdateConvoysSessionState state;
    private Convoys convoys;

    private Integer type  ;

    public UpdateConvoysSession(Convoys convoys) {
        this.convoys = convoys;
    }

    public UpdateConvoysSessionState getState() {
        return state;
    }

    public void setState(UpdateConvoysSessionState state) {
        this.state = state;
    }


    public Convoys getBusiness() {
        return convoys;
    }

    public Convoys getConvoys() {
        return convoys;
    }

    public void setConvoys(Convoys convoys) {
        this.convoys = convoys;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
