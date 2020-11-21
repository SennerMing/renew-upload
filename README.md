### 大文件上传,断点续传,秒传,fastdfs

该项目是对gitee名为令狐大侠老哥的[renew_upload](https://gitee.com/zwlan/renewFastdfs)项目进行改造的，感谢这位好大哥提供的解决方案。

为什么要做这个东西呢？因为没有钱嘛，没钱一定要做嘛！

<img src="https://github.com/SennerMing/renew-upload/blob/master/images/spirit.jpg" alt="精神领袖" style="zoom:33%;" />

我们公司的后端存储工具为FastDFS，关于这个存储工具小明百度过，比较适合小文件就类似于抖音小视频的那种，好像有一种说法是5M到500M之间，没仔细阅读过官方文档不是很懂，总之就是不适合大文件的上传。之前我们公司的一些个老哥，写过一个基于此的文件上传接口，在实际应用当中，有些不听话的用户就反馈啦：哎呀，我上传1个G的文件，怎么传着传着莫名其妙的就失败了，而且我再上传，这个小东西都不做上传记录的，还要重新开始，真的是太难用、太浪费时间了！

客户反馈关键词：上传1个G、历史记录，拿着这两个关键词的小明就开始胡思乱想了🤔，上传大文件的时候，不管是网络哇还是前后台处理请求的原因，总是有可能中途断掉，那能不能通过工具将用户上传的大文件切成小块再进行上传哇，这样的话，既能降低莫名其妙上传失败的几率，又能记录用户上传的进度这样的话，就算是上传中途失败了，用户再重新开始上传的时候，也不用从0开始了，还能实现秒传你说喜不喜人。小明想的正美着呢，领导拍了拍小明聪明的大腿说道，对对对，明啊，你这思路不错，那就顺手把实现的前后台技术方案也调研一下吧，这周做个Demo出来。哔哔哔哩？

小明百度的第一条，就是令狐大侠的技术方案，粗略的看了下简介，各方面感觉整挺好，工程抢过来不必自己做,十亿先拿掉五亿,接下来发包，两转三转,四五六七八转，用docker装个redis，改下配置文件就算齐活了。项目跑起来，小明试着上传了几个大文件，感觉中途中断的几率还是蛮高的，管它呢，反正可以交差了。

可是等到了实际要上线的时间，前端的好大哥又说了：明啊，你这个WebUploader就这个前端大文件上传工具啊，它没发集成到我的VUE项目里去哇，小明气的当时差点就要叫出来了，心想：MD，老子刚刚才看到将WebUploader集成到VUE的文章，不就是不愿意费那一点点的事嘛真的是，转脸微笑着对着前端老大哥说：那您说咋整？“再整一个用VUE框架的插件呗”，呵呵呵，好吧好吧都是老大，刚来几天手头活也不是那么多，立马翻出来个Vue-Simple-Uploader给他，其实交互的原理和百度的WebUploader差不多，Vue-Simple-uploader说是比WebUploader多了支持多线程....然鹅坏就坏在这个支持多线程。

===========2020年11月22日00:46:08，夜深了，🍊晚安不要噩梦===========

