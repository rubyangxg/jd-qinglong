package com.meread.selenium.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by yangxg on 2021/9/9
 *
 * @author yangxg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeStatus {
    private String nodeId;
    private String uri;
    private int maxSessions;
    private boolean isFullSession;
    private String availability;
    private List<SlotStatus> slotStatus;
}
