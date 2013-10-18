package com.neetnoffice.xmppclient;

import java.util.HashMap;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.neetnoffice.xmppclient.AbstractXMPPService.NeetChatManagerListener;

public class XMPPClient {
	public static final String GTALK_HOST = "talk.google.com";
	public static final int GTALK_PORT = 5222;
	public static final String FACEBOOK_HOST = "chat.facebook.com";
	public static final int FACEBOOK_PORT = 5222;

	private XMPPConnection connection;
	private NeetChatManagerListener neetChatManagerListener;
	public String host;
	public int port;
	String username;
	String password;
	private Context context;
	private Class<? extends AbstractXMPPService> clazz;

	public static XMPPClient getXMPPClient(String host, int port) {
		if (AbstractXMPPService.clients == null) {
			AbstractXMPPService.clients = new HashMap<String, XMPPClient>();
		}

		StringBuffer key = new StringBuffer();
		key.append(host);
		key.append(port);

		Log.d("IM", "getXMPPClient , key :" + key.toString());
		XMPPClient client = null;
		if(AbstractXMPPService.clients.size()>0){
			client = AbstractXMPPService.clients.get(key.toString());
		}
		if (client == null) {
			try {
				client = new XMPPClient(host, port);
				AbstractXMPPService.clients.put(client.getKey(), client);
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}
		return client;
	}

	private XMPPClient(String host, int port)throws XMPPException {
		this.host = host;
		this.port = port;
		AndroidConnectionConfiguration config = null;
		config = new AndroidConnectionConfiguration(host, port);
		config.setReconnectionAllowed(true);
		
		ProviderManager pm = ProviderManager.getInstance();
		configure(pm);
		connection = new XMPPConnection(config);
	}

	public void login(Context context,
			Class<? extends AbstractXMPPService> clazz, String username,
			String password) {
		this.username = username;
		this.password = password;
		this.context = context;
		this.clazz = clazz;
		Intent intent = new Intent();
		intent.setClass(context, clazz);
		intent.putExtra(AbstractXMPPService.KEY, getKey());
		context.startService(intent);
	}

	public void logout() {
		if (context != null && clazz != null) {
			Intent intent = new Intent();
			intent.setClass(context, clazz);
			context.stopService(intent);
		}
	}

	/**
	 * 注册
	 * 
	 * @param account
	 *            注册帐号
	 * @param password
	 *            注册密码
	 * @return 1、注册成功 0、服务器没有返回结果2、这个账号已经存在3、注册失败
	 */
	public void registration(final String account, final String password,
			RegistrationCallBack callback) {
		new RegistrationTask(host, port, account, password, callback).execute();
	}

	public XMPPConnection getXMPPConnection() {
		return connection;
	}

	String getKey() {
		StringBuffer key = new StringBuffer();
		key.append(host);
		key.append(port);

		return key.toString();
	}

	void setChatManagerListener(NeetChatManagerListener neetChatManagerListener) {
		this.neetChatManagerListener = neetChatManagerListener;
	}

	NeetChatManagerListener getChatManagerListener() {
		return neetChatManagerListener;
	}

	private void configure(ProviderManager pm) {
		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());
		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());
		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());
		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// vCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());
		// pm.addIQProvider("open","http://jabber.org/protocol/ibb", new
		// IBBProviders.Open());
		// pm.addIQProvider("close","http://jabber.org/protocol/ibb", new
		// IBBProviders.Close());
		// pm.addExtensionProvider("data","http://jabber.org/protocol/ibb", new
		// IBBProviders.Data());
		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
		// Html
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());
		// Ad-Hoc Command
		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());
		// Chat State
		ChatStateExtension.Provider chatState = new ChatStateExtension.Provider();
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates", chatState);
		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates", chatState);
		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates", chatState);
		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates", chatState);
		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates", chatState);
		// MUC User,Admin,Owner
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());
	}
}
