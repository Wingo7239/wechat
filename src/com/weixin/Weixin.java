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
	// �Զ����TOken
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
		// ʱ���
		String timestamp = request.getParameter("timestamp");
		// �����
		String nonce = request.getParameter("nonce");
		// ����ַ���
		String echostr = request.getParameter("echostr");
		System.out.println(signature + " " + timestamp + " " + nonce + " "
				+ echostr);
		echostr = checkAuthentication(signature, timestamp, nonce, echostr);
		// ��֤ͨ�������漴�ִ�
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
		// ������΢�Ÿ�ƽ̨������Ϣʱ�ͻᵽ����
		// �ظ����ֺ�ͼ����Ϣ���Ҷ�д���ˣ��Լ����Ը����Լ�����Ҫ����Ӧ�Ĵ���
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
			if (receive.equals("��") || receive.equals("?")) {

				replyMsg.append("��ӭʹ��΢��ƽ̨��");
				replyMsg.append("\r\n1����ǰʱ��");
				replyMsg.append("\r\n2��������");
				replyMsg.append("\r\n3����ͼ��");
				replyMsg.append("\r\n��������������ֱ������������Ϣ");

				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			} else if (receive.equals("2")) {

				// �ظ�������Ϣ
				returnXml = getReplyMusicMessage(textMsg.getFromUserName(),
						textMsg.getToUserName());

			} else if (receive.equals("3")) {

				// �ظ�ͼ��
				returnXml = getReplyTuwenMessage(textMsg.getFromUserName(),
						textMsg.getToUserName());

			} else if (receive.equals("1")) {

				// �ظ�ʱ��
				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");

				replyMsg.append("��ǰʱ��\r\n" + df.format(new Date()));
				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			} else {

				replyMsg.append("�յ��� " + textMsg.getContent());
				returnXml = getReplyTextMessage(replyMsg.toString(), textMsg
						.getFromUserName(), textMsg.getToUserName());

			}
		} else {

			replyMsg.append("�����ˣ���˭���ö�����");
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
		// ����ȡ���Ĳ�����������
		String[] ArrTmp = { Token, timestamp, nonce };
		// ��΢���ṩ�ķ��������������ݽ�������
		Arrays.sort(ArrTmp);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < ArrTmp.length; i++) {
			sb.append(ArrTmp[i]);
		}
		// ���������ַ�������SHA-1����
		String pwd = Encrypt(sb.toString());
		if (pwd.equals(signature)) {
			try {
				System.out.println("΢��ƽ̨ǩ����Ϣ��֤�ɹ���");
				result = echostr;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("΢��ƽ̨ǩ����Ϣ��֤ʧ�ܣ�");
		}
		return result;
	}

	/**
	 * ��SHA-1�㷨�����ַ���������16���ƴ�
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
			System.out.println("����");
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

	// ��ȡ�����ı���Ϣ
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

	// �ظ��ı���Ϣ
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

	// �ظ�������Ϣ
	private String getReplyMusicMessage(String fromUserName, String toUserName) {

		ReplyMusicMessage we = new ReplyMusicMessage();
		Music music = new Music();

		we.setMessageType("music");
		we.setCreateTime(new Long(new Date().getTime()).toString());
		we.setToUserName(fromUserName);
		we.setFromUserName(toUserName);
		we.setFuncFlag("0");

		music.setTitle("�ؼ�|X-man");
		music.setDescription("����˹ �ؼ�  ��������ҵĳ�˼...");

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

	// �ظ�ͼ����Ϣ
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

		item.setTitle("����");
		item
				.setDescription("���飨SHUNSUKE����Twitter�����������е�ż��Ȯ���ǹ�����ϵ����Ȯ�����׳�Ӣϵ����������Ϊ���������ȶ��ߺ����硣");
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
