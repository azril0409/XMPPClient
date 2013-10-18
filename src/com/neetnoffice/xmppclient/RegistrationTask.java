package com.neetnoffice.xmppclient;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;

import android.os.AsyncTask;
import android.util.Log;

public class RegistrationTask extends AsyncTask<Void, Void, Integer> {
	public final String ERROR = "No response from server.";
	private String host;
	private int port;
	private String account;
	private String password;
	private RegistrationCallBack callBack;
	private String error_message;

	RegistrationTask(String host, int port, String account, String password,
			RegistrationCallBack callBack) {
		this.host = host;
		this.port = port;
		this.account = account;
		this.password = password;
		this.callBack = callBack;
	}

	@Override
	protected Integer doInBackground(Void... arg0) {
		XMPPConnection connection = XMPPClient.getXMPPClient(host, port).getXMPPConnection();
		if (!connection.isConnected()) {
			try {
				connection.connect();
			} catch (XMPPException e) {
				Log.d("IM", e.getMessage());
			}
		}

		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(connection.getServiceName());
		reg.setUsername(account);
		reg.setPassword(password);
		reg.addAttribute("android", "geolo_createUser_android");
		PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = connection.createPacketCollector(filter);
		connection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		collector.cancel();// 停止请求results（是否成功的结果）
		if (result == null) {
			error_message = ERROR;
			Log.d("IM", ERROR);
			return 0;
		} else if (result.getType() == IQ.Type.RESULT) {
			Log.d("IM", "seccess");
			return 1;
		} else {
			error_message = result.getError().toString();
			if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
				Log.d("IM", "IQ.Type.ERROR 2: " + result.getError().toString());
				return 2;
			} else {
				Log.d("IM", "IQ.Type.ERROR 3: " + result.getError().toString());
				return 3;
			}
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		if(callBack != null){
			if(result == 1){
				callBack.onSeccess();
			}else{
				callBack.onError(error_message);
			}			
		}
	}

}
