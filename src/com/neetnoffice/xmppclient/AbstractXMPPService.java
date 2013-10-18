package com.neetnoffice.xmppclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public abstract class AbstractXMPPService extends Service implements
		InvitationListener, ConnectionListener {
	public static final String USER = "USER";
	public static final String PASSWORD = "PASSWORD";
	public static final String HOST = "HOST";
	public static final String PORT = "PORT";
	public static final String SERVICE = "SERVICE";
	public static final String KEY = "KEY";
	public static final String ROOM = "ROOM";
	static Map<String, XMPPClient> clients;

	private static List<XMPPServiceCallback> callbacks;
	private Handler mHandler = new Handler();
	private Thread connectThread;
	private int startId;

	public interface XMPPServiceCallback {
		public void onLogin();
	};

	public static void addXMPPServiceCallback(XMPPServiceCallback callback) {
		if (callbacks == null) {
			callbacks = new ArrayList<XMPPServiceCallback>();
		}
		callbacks.add(callback);
	}

	public static void removeXMPPServiceCallback(XMPPServiceCallback callback) {
		if (callbacks != null) {
			callbacks.remove(callback);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.startId = startId;
		try {
			SmackAndroid.init(this);
		} catch (Exception e) {

		}
		if (clients != null) {
			Log.d("IM", "clients is not null");
			Log.d("IM", "clients size :" + clients.size());
			connectThread = new ConnectThread(intent);
			connectThread.start();
		}
		return START_STICKY;
	}

	private class ConnectThread extends Thread {
		Intent intent;

		ConnectThread(Intent intent) {
			this.intent = intent;
		}

		@Override
		public void run() {
			if (intent != null) {
				XMPPClient client = clients.get(intent.getStringExtra(KEY));
				connectAndLogin(client);
			} else {
				for (XMPPClient client : clients.values()) {
					connectAndLogin(client);
				}
			}

			Notification notification = getNotification();
			if (notification != null) {
				notification.flags = Notification.FLAG_NO_CLEAR;
				startForeground(AbstractXMPPService.this.getId(), notification);
			}
			mHandler.post(new CallbackRunnable());
		}
	}

	private void connectAndLogin(XMPPClient client) {
		XMPPConnection connection = client.getXMPPConnection();
		Log.d("IM", "on ConnectThread run");
		Log.d("IM", "client key : " + client.getKey());
		if (!connection.isConnected()) {
			try {
				connection.connect();
			} catch (XMPPException e) {
				Log.d("IM", "XMPPException : " + e.getMessage());
			} catch (Exception e) {
				Log.d("IM", "Exception : " + e.getMessage());
			}
		}
		Log.d("IM", "username : " + client.username);
		Log.d("IM", "password : " + client.password);
		try {
			connection.login(client.username, client.password);
			Presence presence = new Presence(Presence.Type.available);
			connection.sendPacket(presence);
			client.setChatManagerListener(new NeetChatManagerListener(client));
			MultiUserChat.addInvitationListener(connection,
					AbstractXMPPService.this);
			connection.getChatManager().addChatListener(
					client.getChatManagerListener());
			connection.addConnectionListener(AbstractXMPPService.this);
		} catch (XMPPException e) {
			Log.d("IM", "XMPPException : " + e.getMessage());
		} catch (Exception e) {
			Log.d("IM", "Exception : " + e.getMessage());
		}
	}

	private class CallbackRunnable implements Runnable {

		public void run() {
			if (callbacks != null) {
				for (XMPPServiceCallback callback : callbacks) {
					try {
						if (callback != null) {
							callback.onLogin();
						}
					} catch (Exception e) {
						callbacks.remove(callback);
					}
				}
			}
		}

	}

	@Override
	public void onDestroy() {
		for (XMPPClient client : clients.values()) {
			XMPPConnection connection = client.getXMPPConnection();
			connection.getChatManager().removeChatListener(
					client.getChatManagerListener());
			if (connection.isConnected()) {
				connection.disconnect();
			}
		}
		clients = null;
		this.stopForeground(true);
		super.onDestroy();
	}

	public abstract void processMessage(XMPPClient client, Chat chat,
			Message message);

	public abstract void invitationReceived(Connection conn, String room,
			String inviter, String reason, String password, Message message);

	public abstract Notification getNotification();

	protected abstract int getId();

	public class NeetChatManagerListener implements ChatManagerListener,
			MessageListener {
		XMPPClient client;

		NeetChatManagerListener(XMPPClient client) {
			this.client = client;
		}

		public void chatCreated(Chat chat, boolean arg1) {
			chat.addMessageListener(this);
		}

		public void processMessage(Chat chat, Message message) {
			AbstractXMPPService.this.processMessage(client, chat, message);
		}
	};

	@Override
	public void connectionClosed() {

	}

	@Override
	public void connectionClosedOnError(Exception arg0) {

	}

	@Override
	public void reconnectingIn(int arg0) {

	}

	@Override
	public void reconnectionFailed(Exception arg0) {

	}

	@Override
	public void reconnectionSuccessful() {

	}
}
