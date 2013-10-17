package com.weixin;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.weixin.domain.Articles;
import com.weixin.domain.Item;
import com.weixin.domain.Music;
import com.weixin.domain.ReplyMusicMessage;
import com.weixin.domain.ReplyTextMessage;
import com.weixin.domain.ReplyTuwenMessage;
import com.weixin.domain.RequestTextMessage;
import com.weixin.util.JdbcUtil;

/**
 * Servlet implementation class Weixin
 */
public class Weixin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// 自定义的TOken
	public static final String Token = "liutimeweixintoken";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Weixin() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		String signature = request.getParameter("signature");
		// 时间戳
		String timestamp = request.getParameter("timestamp");
		// 随机数
		String nonce = request.getParameter("nonce");
		// 随机字符串
		String echostr = request.getParameter("echostr");
		System.out.println(signature + " " + timestamp + " " + nonce + " "
				+ echostr);
		echostr = checkAuthentication(signature, timestamp, nonce, echostr);
		// 验证通过返回随即字串
		response.getWriter().write(echostr);
		response.getWriter().flush();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// 当你用微信给平台发送信息时就会到这里
		// 回复音乐和图文消息，我都写死了，自己可以根据自己的需要加相应的处理
		JdbcUtil ju = new JdbcUtil();
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter pw = response.getWriter();
		String wxMsgXml = IOUtils.toString(request.getInputStream(), "utf-8");
		RequestTextMessage textMsg = null;
		try {
			textMsg = getRequestTextMessage(wxMsgXml);
			// check the user, 
			// if the user is first time use our wechat, not in the DB,
			// then add the user into our db
			// and return welcome to our system.
			if(!checkUser(textMsg,ju)){ 
				ju.saveUser(textMsg.getFromUserName());
				textMsg.setContent("welcome to login wechat sys first time");
				}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		StringBuffer replyMsg = new StringBuffer();
		String receive = textMsg.getContent().trim();
		String returnXml = null;

		if (textMsg != null && !receive.equals("")) {
			if (receive.equals("？") || receive.equals("?")) {

				replyMsg.append("欢迎使用微信平台！");
				replyMsg.append("\r\n1、当前时间");
				replyMsg.append("\r\n2、听音乐");
				replyMsg.append("\r\n3、看图文");
				replyMsg.append("\r\n其他、回音壁请直接输入文字信息");

				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			} else if (receive.equals("2")) {

				// 回复音乐信息
				returnXml = getReplyMusicMessage(textMsg.getFromUserName(),
						textMsg.getToUserName());

			} else if (receive.equals("3")) {

				// 回复图文
				returnXml = getReplyTuwenMessage(textMsg.getFromUserName(),
						textMsg.getToUserName());

			} else if (receive.equals("1")) {

				// 回复时间
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");

				replyMsg.append("当前时间\r\n" + df.format(new Date()));
				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			} else {

				replyMsg.append("收到： " + textMsg.getContent());
				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			}
		} else {

			replyMsg.append("别闹了，这谁看得懂啊！");
			returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
					.getFromUserName(), textMsg.getToUserName());

		}
		pw.println(returnXml);
	}
	
	private boolean checkUser(RequestTextMessage rtm, JdbcUtil ju){
		
		String userId = rtm.getFromUserName();
		return ju.getUser(userId);
	}

	private String checkAuthentication(String signature, String timestamp,
			String nonce, String echostr) {
		String result = "";
		// 将获取到的参数放入数组
		String[] ArrTmp = { Token, timestamp, nonce };
		// 按微信提供的方法，对数据内容进行排序
		Arrays.sort(ArrTmp);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < ArrTmp.length; i++) {
			sb.append(ArrTmp[i]);
		}
		// 对排序后的字符串进行SHA-1加密
		String pwd = Encrypt(sb.toString());
		if (pwd.equals(signature)) {
			try {
				System.out.println("微信平台签名消息验证成功！");
				result = echostr;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("微信平台签名消息验证失败！");
		}
		return result;
	}

	/**
	 * 用SHA-1算法加密字符串并返回16进制串
	 * 
	 * @param strSrc
	 * @return
	 */
	private String Encrypt(String strSrc) {
		MessageDigest md = null;
		String strDes = null;
		byte[] bt = strSrc.getBytes();
		try {
			md = MessageDigest.getInstance("SHA-1");
			md.update(bt);
			strDes = bytes2Hex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			System.out.println("错误");
			return null;
		}
		return strDes;
	}

	private String bytes2Hex(byte[] bts) {
		String des = "";
		String tmp = null;
		for (int i = 0; i < bts.length; i++) {
			tmp = (Integer.toHexString(bts[i] & 0xFF));
			if (tmp.length() == 1) {
				des += "0";
			}
			des += tmp;
		}
		return des;
	}

	// 获取推送文本消息
	private RequestTextMessage getRequestTextMessage(String xml) {

		XStream xstream = new XStream(new DomDriver());

		xstream.alias("xml", RequestTextMessage.class);
		
		xstream.aliasField("ToUserName", RequestTextMessage.class,"toUserName");
		xstream.aliasField("FromUserName", RequestTextMessage.class,"fromUserName");
		xstream.aliasField("CreateTime", RequestTextMessage.class,"createTime");
		xstream.aliasField("MsgType", RequestTextMessage.class, "messageType");
		xstream.aliasField("Content", RequestTextMessage.class, "content");
		xstream.aliasField("MsgId", RequestTextMessage.class, "msgId");

		RequestTextMessage requestTextMessage = (RequestTextMessage) xstream.fromXML(xml);
		return requestTextMessage;
	}

	// 回复文本消息
	private String getReplyTextMessage(String content, String fromUserName,
			String toUserName) {

		ReplyTextMessage we = new ReplyTextMessage();
		we.setMessageType("text");
		we.setFuncFlag("0");
		we.setCreateTime(new Long(new Date().getTime()).toString());
		we.setContent(content);
		we.setToUserName(fromUserName);
		we.setFromUserName(toUserName);
		XStream xstream = new XStream(new DomDriver());
		xstream.alias("xml", ReplyTextMessage.class);
		xstream.aliasField("ToUserName", ReplyTextMessage.class, "toUserName");
		xstream.aliasField("FromUserName", ReplyTextMessage.class,"fromUserName");
		xstream.aliasField("CreateTime", ReplyTextMessage.class, "createTime");
		xstream.aliasField("MsgType", ReplyTextMessage.class, "messageType");
		xstream.aliasField("Content", ReplyTextMessage.class, "content");
		xstream.aliasField("FuncFlag", ReplyTextMessage.class, "funcFlag");
		String xml = xstream.toXML(we);
		return xml;
	}

	// 回复音乐消息
	private String getReplyMusicMessage(String fromUserName, String toUserName) {

		ReplyMusicMessage we = new ReplyMusicMessage();
		Music music = new Music();

		we.setMessageType("music");
		we.setCreateTime(new Long(new Date().getTime()).toString());
		we.setToUserName(fromUserName);
		we.setFromUserName(toUserName);
		we.setFuncFlag("0");

		music.setTitle("回家|X-man");
		music.setDescription("萨克斯 回家  勾起你想家的愁思...");

		String url = "http://bcs.duapp.com/yishi-music/%E5%9B%9E%E5%AE%B6.mp3?sign=MBO:97068c69ccb2ab230a497c59d528dcce:LdYZ%2FLXohKa6YCy9gbxL%2B1mZ4Co%3D";
		String url2 = "http://bcs.duapp.com/yishi-music/X-man.mp3?sign=MBO:97068c69ccb2ab230a497c59d528dcce:cYV%2B%2Fq2Tlv2de6gqecZynCyIm3k%3D";
		music.setMusicUrl(url);
		music.setHqMusicUrl(url2);

		we.setMusic(music);

		XStream xstream = new XStream(new DomDriver());
		xstream.alias("xml", ReplyMusicMessage.class);
		xstream.aliasField("ToUserName", ReplyMusicMessage.class, "toUserName");
		xstream.aliasField("FromUserName", ReplyMusicMessage.class,
				"fromUserName");
		xstream.aliasField("CreateTime", ReplyMusicMessage.class, "createTime");
		xstream.aliasField("MsgType", ReplyMusicMessage.class, "messageType");
		xstream.aliasField("FuncFlag", ReplyMusicMessage.class, "funcFlag");
		xstream.aliasField("Music", ReplyMusicMessage.class, "Music");

		xstream.aliasField("Title", Music.class, "title");
		xstream.aliasField("Description", Music.class, "description");
		xstream.aliasField("MusicUrl", Music.class, "musicUrl");
		xstream.aliasField("HQMusicUrl", Music.class, "hqMusicUrl");

		String xml = xstream.toXML(we);
		return xml;
	}

	// 回复图文消息
	private String getReplyTuwenMessage(String fromUserName, String toUserName) {

		ReplyTuwenMessage we = new ReplyTuwenMessage();

		Articles articles = new Articles();

		Item item = new Item();

		we.setMessageType("news");
		we.setCreateTime(new Long(new Date().getTime()).toString());
		we.setToUserName(fromUserName);
		we.setFromUserName(toUserName);
		we.setFuncFlag("0");
		we.setArticleCount(1);

		item.setTitle("俊介");
		item
				.setDescription("俊介（SHUNSUKE）是Twitter上现在最流行的偶像犬，是哈多利系博美犬（即俗称英系博美），因为在网上卖萌而走红网络。");
		item
				.setPicUrl("http://bcs.duapp.com/yishi-music/111.jpg?sign=MBO:97068c69ccb2ab230a497c59d528dcce:hmzcBYxgI4yUaTd9GvahO1GvE%2BA%3D");
		item.setUrl("http://baike.baidu.com/view/6300265.htm");

		articles.setItem(item);
		we.setArticles(articles);

		XStream xstream = new XStream(new DomDriver());
		xstream.alias("xml", ReplyTuwenMessage.class);
		xstream.aliasField("ToUserName", ReplyTuwenMessage.class, "toUserName");
		xstream.aliasField("FromUserName", ReplyTuwenMessage.class,
				"fromUserName");
		xstream.aliasField("CreateTime", ReplyTuwenMessage.class, "createTime");
		xstream.aliasField("MsgType", ReplyTuwenMessage.class, "messageType");
		xstream.aliasField("Articles", ReplyTuwenMessage.class, "Articles");

		xstream.aliasField("ArticleCount", ReplyTuwenMessage.class,
				"articleCount");
		xstream.aliasField("FuncFlag", ReplyTuwenMessage.class, "funcFlag");

		xstream.aliasField("item", Articles.class, "item");

		xstream.aliasField("Title", Item.class, "title");
		xstream.aliasField("Description", Item.class, "description");
		xstream.aliasField("PicUrl", Item.class, "picUrl");
		xstream.aliasField("Url", Item.class, "url");

		String xml = xstream.toXML(we);
		return xml;
	}

}
