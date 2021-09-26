package com.meread.selenium;

import com.zhuangxv.bot.annotation.FriendMessageHandler;
import com.zhuangxv.bot.core.Friend;
import com.zhuangxv.bot.message.MessageChain;
import org.springframework.stereotype.Service;

/**
 * @author yangxg
 * @date 2021/9/26
 */
@Service
public class BotService {

    @FriendMessageHandler
    public void receive(Friend friend, MessageChain chain, String content, int msgId) {
        System.out.println("receive");
        System.out.println(friend);
        System.out.println(chain);
        System.out.println(content);
        System.out.println(msgId);
    }
}
