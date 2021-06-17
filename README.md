# QQHelper

*v1.2*
新增安卓wechat，当给好友发送text消息时，发送action为`com.cooltol.qqhelper.MM_MSG_SENT`的广播消息
extras有以下参数：

> talker: 对方微信id
content: txt消息

*v1.1*
安卓qq，当给好友发送text消息时，发送action为`com.cooltol.qqhelper.QQ_MSG_SENT`的广播消息
extras带有以下参数：

> selfuin：本人qq号
senderuin: 发送者qq号
frienduin： 对方qq号
msg： txt消息
time： 发送消息时时间戳
