package com.github.tvbox.osc.bean;

import java.util.ArrayList;

public class LiveChannelGroup {
    /**
     * groupIndex : 分组索引号
     * groupName : 分组名称
     * password : 分组密码
     */
    private int groupIndex;
    private String groupName;
    private String groupPassword;
    private ArrayList<LiveChannelItem> liveChannelItems;

    public int getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ArrayList<LiveChannelItem> getLiveChannels() {
        if (liveChannelItems == null) {
            liveChannelItems = new ArrayList<>();
        }
        return liveChannelItems;
    }

    public void setLiveChannels(ArrayList<LiveChannelItem> liveChannelItems) {
        this.liveChannelItems = liveChannelItems;
    }

    /**
     * 分组密码。任何来源（尤其程序里手工构造的频道组）都可能没设过密码，
     * 这里统一兜底成空串，避免各处 getGroupPassword().isEmpty() 抛 NPE。
     */
    public String getGroupPassword() {
        return groupPassword == null ? "" : groupPassword;
    }

    public void setGroupPassword(String groupPassword) {
        this.groupPassword = groupPassword;
    }
}
